#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=== WA Autopilot Build Script ==="
echo "Target: Oppo A5s | Android 8.1 | 2GB RAM"
echo "Started: $(date)"
echo ""

# Pastikan tools dasar ada
pkg install -y openjdk-17 unzip curl 2>/dev/null || true
export JAVA_HOME=$PREFIX/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# ---- Android SDK ----
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

if [ ! -d "$ANDROID_HOME/cmdline-tools/latest/bin" ]; then
    echo "[1/4] Download Android cmdline-tools (~150MB)..."
    cd $HOME
    curl -L -o cmdtools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    mkdir -p $ANDROID_HOME/cmdline-tools
    cd $ANDROID_HOME/cmdline-tools
    unzip -q $HOME/cmdtools.zip
    mv cmdline-tools latest
    rm -f $HOME/cmdtools.zip
else
    echo "[1/4] cmdline-tools sudah ada, skip download"
fi

echo "[2/4] Install SDK components (platform-35, build-tools)..."
yes | sdkmanager --sdk_root=$ANDROID_HOME --licelines > /dev/null 2>&1 || true
yes | sdkmanager --sdk_root=$ANDROID_HOME --licenses > /dev/null 2>&1 || true
sdkmanager --sdk_root=$ANDROID_HOME "platforms;android-35" "build-tools;35.0.0" "platform-tools" 2>&1 | tail -5

# ---- Gradle ----
if [ ! -d "$HOME/gradle-8.2" ]; then
    echo "[3/4] Download Gradle 8.2 (~130MB)..."
    cd $HOME
    curl -L -o gradle.zip "https://services.gradle.org/distributions/gradle-8.2-bin.zip"
    unzip -q gradle.zip
    rm -f gradle.zip
else
    echo "[3/4] Gradle 8.2 sudah ada, skip download"
fi
export PATH=$PATH:$HOME/gradle-8.2/bin

# ---- Build ----
echo "[4/4] Build APK (bisa 10-25 menit di Oppo A5s)..."
cd $HOME/wa-autopilot/android
gradle assembleDebug --no-daemon --max-workers=1 --stacktrace 2>&1 | tail -40

APK_PATH=$(find app/build/outputs -name "app-debug.apk" 2>/dev/null | head -1)
if [ -n "$APK_PATH" ]; then
    cp "$APK_PATH" $HOME/wa-autopilot/app-debug.apk
    echo ""
    echo "=== BUILD SUKSES ==="
    echo "APK: $HOME/wa-autopilot/app-debug.apk"
    ls -lh $HOME/wa-autopilot/app-debug.apk
else
    echo ""
    echo "=== BUILD GAGAL ==="
    echo "Cek log di atas. Alternatif: pakai GitHub Actions (sudah setup)."
    exit 1
fi
