import { Hono } from 'hono';
import { cors } from 'hono/cors';

type Bindings = {
  DB: D1Database;
  WHATSAPP_VERIFY_TOKEN: string;
};

const app = new Hono<{ Bindings: Bindings }>();

app.use('*', cors());

// ============================================
// WHATSAPP WEBHOOK VERIFICATION (GET)
// ============================================
app.get('/webhook', (c) => {
  const mode = c.req.query('hub.mode');
  const token = c.req.query('hub.verify_token');
  const challenge = c.req.query('hub.challenge');

  if (mode === 'subscribe' && token === c.env.WHATSAPP_VERIFY_TOKEN) {
    return c.text(challenge || '');
  }
  return c.text('Forbidden', 403);
});

// ============================================
// WHATSAPP WEBHOOK RECEIVER (POST)
// ============================================
app.post('/webhook', async (c) => {
  const body = await c.req.json();

  const entry = body.entry?.[0];
  const change = entry?.changes?.[0];
  const value = change?.value;
  const phoneNumberId = value?.metadata?.phone_number_id;

  if (value?.messages && phoneNumberId) {
    const message = value.messages[0];
    const senderPhone = message.from;
    const messageText = message.text?.body || '';

    // Cari user berdasarkan phone_number_id
    const user = await c.env.DB.prepare(
      'SELECT * FROM users WHERE wa_phone_number_id = ?'
    ).bind(phoneNumberId).first();

    if (user) {
      // Cari rule yang match (keyword ada di dalam pesan)
      const rule = await c.env.DB.prepare(
        'SELECT * FROM rules WHERE user_id = ? AND is_active = 1 AND instr(lower(?), lower(keyword)) > 0 LIMIT 1'
      ).bind(user.id, messageText).first();

      if (rule) {
        // Kirim auto-reply
        await sendWhatsAppMessage(user, senderPhone, rule.reply_text);

        // Log pesan masuk
        await c.env.DB.prepare(
          'INSERT INTO messages (id, user_id, sender_phone, message_text, reply_text, direction) VALUES (?, ?, ?, ?, ?, ?)'
        ).bind(
          crypto.randomUUID(),
          user.id,
          senderPhone,
          messageText,
          rule.reply_text,
          'incoming'
        ).run();

        // Increment hit_count
        await c.env.DB.prepare(
          'UPDATE rules SET hit_count = hit_count + 1 WHERE id = ?'
        ).bind(rule.id).run();
      }
    }
  }

  return c.text('OK');
});

// ============================================
// API UNTUK ANDROID APP
// ============================================

// Middleware auth sederhana (MVP)
app.use('/api/*', async (c, next) => {
  const userId = c.req.header('X-User-Id');
  if (!userId) {
    return c.json({ error: 'Unauthorized' }, 401);
  }
  await next();
});

// GET semua rules
app.get('/api/rules', async (c) => {
  const userId = c.req.header('X-User-Id');
  const { results } = await c.env.DB.prepare(
    'SELECT * FROM rules WHERE user_id = ? ORDER BY created_at DESC'
  ).bind(userId).all();
  return c.json(results);
});

// POST tambah rule
app.post('/api/rules', async (c) => {
  const userId = c.req.header('X-User-Id');
  const { keyword, reply_text } = await c.req.json();
  const id = crypto.randomUUID();

  await c.env.DB.prepare(
    'INSERT INTO rules (id, user_id, keyword, reply_text) VALUES (?, ?, ?, ?)'
  ).bind(id, userId, keyword, reply_text).run();

  return c.json({ id, keyword, reply_text, is_active: 1 });
});

// DELETE rule
app.delete('/api/rules/:id', async (c) => {
  const userId = c.req.header('X-User-Id');
  const id = c.req.param('id');

  await c.env.DB.prepare(
    'DELETE FROM rules WHERE id = ? AND user_id = ?'
  ).bind(id, userId).run();

  return c.json({ success: true });
});

// GET statistik
app.get('/api/stats', async (c) => {
  const userId = c.req.header('X-User-Id');

  const totalRules = await c.env.DB.prepare(
    'SELECT COUNT(*) as count FROM rules WHERE user_id = ?'
  ).bind(userId).first();

  const totalMessages = await c.env.DB.prepare(
    'SELECT COUNT(*) as count FROM messages WHERE user_id = ?'
  ).bind(userId).first();

  const totalReplies = await c.env.DB.prepare(
    'SELECT COUNT(*) as count FROM messages WHERE user_id = ? AND reply_text IS NOT NULL'
  ).bind(userId).first();

  return c.json({
    total_rules: totalRules?.count || 0,
    total_messages: totalMessages?.count || 0,
    total_replies: totalReplies?.count || 0,
  });
});

// ============================================
// HELPER: KIRIM PESAN WHATSAPP
// ============================================
async function sendWhatsAppMessage(user: any, to: string, text: string) {
  if (!user.wa_phone_number_id || !user.wa_access_token) return;

  await fetch(`https://graph.facebook.com/v21.0/${user.wa_phone_number_id}/messages`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${user.wa_access_token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      messaging_product: 'whatsapp',
      to: to,
      type: 'text',
      text: { body: text },
    }),
  });
}

export default app;
