WA AUTOPILOT
Auto-Reply WhatsApp Business untuk UMKM Indonesia
Dibuat di Oppo A5s via Termux

═══════════════════════════════════════════
 APA INI
═══════════════════════════════════════════

Aplikasi Android + backend serverless yang bikin
bisnis WhatsApp lo bisa auto-reply 24/7. Customer
chat jam 2 pagi tetap dibales. Tinggal pasang
rule keyword -> balasan otomatis.

Contoh:
  Customer kirim: "harga"
  Bot balas: "Harga produk kami mulai Rp50.000.
              Mau order? Ketik ORDER ya."

═══════════════════════════════════════════
 ARSITEKTUR
═══════════════════════════════════════════

  Customer WA --> Meta Cloud API --> Cloudflare Worker
                                        |
                                        v
                                  D1 Database
                                  (rules, users, logs)
                                        |
                                        v
                                  Android APK
                                  (dashboard kelola rules)

Backend: Cloudflare Workers + D1 (gratis, global edge)
APK: Native Kotlin, minSdk 24, ringan buat HP 2GB RAM
WhatsApp: Official Meta Cloud API (1000 conversation/bulan gratis)

═══════════════════════════════════════════
 STRUKTUR FILE
═══════════════════════════════════════════

  wa-autopilot/
    backend/               Cloudflare Worker + D1
      wrangler.toml        config deploy
      schema.sql           database schema
      package.json         dependencies
      tsconfig.json        TypeScript config
      src/index.ts         webhook + API + auto-reply engine

    android/               Aplikasi Android
      app/src/main/
        java/              Kotlin source
        res/               Layout, drawable, values

    .github/workflows/
      build-apk.yml        CI/CD auto-build APK

    build.sh               Script build APK on-device
    deploy-backend.sh      Script deploy backend
    README.md              File ini

═══════════════════════════════════════════
 SETUP LENGKAP
═══════════════════════════════════════════

--- STEP 1: Deploy Backend ---

  cd ~/wa-autopilot
  bash deploy-backend.sh

  Script ini akan:
  - Install wrangler CLI
  - Login Cloudflare (browser terbuka)
  - Create D1 database
  - Init schema
  - Deploy worker

  Catat URL worker: https://wa-autopilot.xxx.workers.dev

--- STEP 2: Setup Meta WhatsApp ---

  1. Buka https://developers.facebook.com/
  2. Login -> My Apps -> Create App -> Business
  3. Add Product "WhatsApp" -> Cloud API
  4. Catat:
     - Phone Number ID
     - Access Token (System User -> Generate permanent token)
  5. Set Webhook:
     - Callback URL: https://wa-autopilot.xxx.workers.dev/webhook
     - Verify Token: token_rahasia_lo_pilih_sendiri
     - Subscribe: messages

  PENTING: Nomor WA harus nomor BARU (belum terdaftar WA).

--- STEP 3: Insert User ke Database ---

  cd ~/wa-autopilot/backend
  wrangler d1 execute wa-autopilot-db --remote --command="INSERT INTO users (id, email, wa_phone_number_id, wa_access_token) VALUES ('user001', 'email@lo.com', 'PHONE_NUMBER_ID', 'ACCESS_TOKEN')"

--- STEP 4: Build APK ---

  Opsi A - On-device (Oppo A5s):
    bash ~/wa-autopilot/build.sh

  Opsi B - GitHub Actions (recommended):
    cd ~/wa-autopilot
    git init && git add . && git commit -m "init"
    git remote add origin https://github.com/USERNAME/wa-autopilot.git
    git push -u origin main
    -> Tab Actions -> download artifact wa-autopilot-apk

  Sebelum build, edit BASE_URL di:
    android/app/src/main/java/com/waautopilot/app/RuleViewModel.kt
    Ganti dengan URL worker lo.

--- STEP 5: Install & Test ---

  1. Transfer APK ke HP
  2. Install (allow unknown sources)
  3. Buka app -> lihat dashboard
  4. Tambah rule: keyword "harga" -> reply "Harga mulai Rp50rb"
  5. Kirim WA "harga" ke nomor bisnis lo
  6. Auto-replay harusnya jalan

═══════════════════════════════════════════
 MONETIZATION
═══════════════════════════════════════════

  Free:     5 rules, 50 reply/bulan
  Pro:      Rp99k/bulan - unlimited rules + reply
  Business: Rp299k/bulan - multi-nomor + broadcast

  DFY Setup: Rp500k-1jt per klien
  Maintenance: Rp150k/bulan per klien

  Target: UMKM, reseller, online shop, jasa
         (laundry, catering, ojek, dll)

═══════════════════════════════════════════
 TROUBLESHOOTING
═══════════════════════════════════════════

  Build gagal JAVA_HOME:
    export JAVA_HOME=$PREFIX/lib/jvm/java-17-openjdk

  Build gagal DNS/Maven:
    Cek settings.gradle.kts - Alibaba mirror harus ada

  Build gagal OOM:
    Pastikan gradle.properties pakai -Xmx1280m

  APK install gagal:
    Settings -> Security -> Unknown Sources -> allow

  Webhook tidak terima pesan:
    Cek URL webhook di Meta Dashboard
    Pastikan verify token sama dengan wrangler.toml

  Auto-reply tidak jalan:
    Cek user sudah di-insert ke D1 dengan
    wa_phone_number_id dan wa_access_token yang benar

═══════════════════════════════════════════
 DIBUAT DENGAN
═══════════════════════════════════════════

  Device: Oppo A5s CPH1909 (Android 8.1, 2.7GB RAM)
  Tools: Termux, Node 26, openjdk-17, Gradle 8.2
  Backend: Cloudflare Workers + D1 + Hono
  APK: Kotlin, Material Components, Retrofit, ViewBinding
