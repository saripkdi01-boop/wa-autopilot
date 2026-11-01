#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=== WA Autopilot Backend Deploy ==="
echo "Pastikan lo sudah punya akun Cloudflare (gratis)"
echo ""

cd ~/wa-autopilot/backend

# Install wrangler
if ! command -v wrangler &> /dev/null; then
    echo "[1/5] Install wrangler CLI..."
    npm install -g wrangler
else
    echo "[1/5] Wrangler sudah ada"
fi

# Install dependencies
echo "[2/5] Install dependencies..."
npm install

# Login
echo "[3/5] Login Cloudflare (browser akan terbuka)..."
wrangler login

# Create D1
echo "[4/5] Create D1 database..."
wrangler d1 create wa-autopilot-db 2>&1 || echo "(DB mungkin sudah ada, lanjut)"

# Init schema
echo "[5/5] Init schema + deploy..."
wrangler d1 execute wa-autopilot-db --file=schema.sql --remote
wrangler deploy

echo ""
echo "=== DEPLOY SUKSES ==="
echo ""
echo "LANGKAH SELANJUTNYA:"
echo "1. Catat URL worker di atas (https://wa-autopilot.xxx.workers.dev)"
echo "2. Edit file:"
echo "   ~/wa-autopilot/android/app/src/main/java/com/waautopilot/app/RuleViewModel.kt"
echo "   Ganti BASE_URL dengan URL worker lo"
echo "3. Rebuild APK: bash ~/wa-autopilot/build.sh"
echo "4. Setup Meta WhatsApp (lihat README.md)"
echo "5. Insert user ke database:"
echo "   wrangler d1 execute wa-autopilot-db --remote --command=\"INSERT INTO users (id, email, wa_phone_number_id, wa_access_token) VALUES ('user001', 'email@lo.com', 'PHONE_NUMBER_ID', 'ACCESS_TOKEN')\""
echo ""
