#!/bin/bash
# Android 15 (API 35) ayrı test turu. Play Store'lu imaj: reklam SDK'sı ve Play Billing de denenebilsin.
# Sonuçlar: ci/logs/a15/ (rapor.txt özet), ekranlar: ci/screens-a15/
NAME=a15
IMG="system-images;android-35;google_apis_playstore;x86_64"
PKG=app.ykspusula
D=ci/logs/a15
R=$D/rapor.txt
mkdir -p "$D" ci/screens-a15
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb() {
  local A="$ANDROID_HOME/platform-tools/adb"
  case "$1" in
    wait-for-device|emu|devices|start-server|kill-server) ;;
    *) timeout 20 "$A" get-state > /dev/null 2>&1 || { echo "adb: cihaz yok ($1)" >&2; return 1; } ;;
  esac
  timeout 1500 "$A" "$@"
}
export ANDROID_AVD_HOME="$HOME/.android/avd"
SDK="$ANDROID_HOME/cmdline-tools/latest/bin"
mkdir -p "$ANDROID_AVD_HOME"
yes | "$SDK/sdkmanager" --licenses > /dev/null 2>&1
timeout 10 "$ANDROID_HOME/platform-tools/adb" emu kill 2>/dev/null; sleep 4
pkill -f qemu-system; sleep 2
"$SDK/sdkmanager" "$IMG" "emulator" "platform-tools" 2>&1 | tr '\r' '\n' | tail -3 > "$D/kurulum.txt"
echo no | "$SDK/avdmanager" create avd -n "$NAME" -k "$IMG" -d pixel_6 --force >> "$D/kurulum.txt" 2>&1
if [ ! -f "$ANDROID_AVD_HOME/$NAME.avd/config.ini" ]; then echo "acilis=yok sanal-cihaz-olusmadi" | tee "$R"; exit 0; fi
printf 'hw.ramSize=4096\ndisk.dataPartition.size=6144M\nhw.keyboard=yes\n' >> "$ANDROID_AVD_HOME/$NAME.avd/config.ini"
"$ANDROID_HOME/emulator/emulator" -avd "$NAME" -accel on -cores 3 -memory 4096 -no-metrics -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot -camera-back none -camera-front none > "$D/emulator.txt" 2>&1 &
EMU=$!
B=""
for i in $(seq 1 200); do
  B=$(timeout 15 adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r'); [ "$B" = "1" ] && break
  kill -0 $EMU 2>/dev/null || { echo "emulator-kapandi deneme=$i" >> "$D/kurulum.txt"; break; }
  sleep 5
done
echo "acilis=$B deneme=$i" | tee "$R"
[ "$B" = "1" ] || { tail -40 "$D/emulator.txt" > "$D/emulator-son.txt"; exit 0; }
echo "android=$(adb shell getprop ro.build.version.release | tr -d '\r') api=$(adb shell getprop ro.build.version.sdk | tr -d '\r') imaj=$IMG" >> "$R"
echo "cihaz=$(adb shell getprop ro.product.model | tr -d '\r') yogunluk=$(adb shell wm density | tr -d '\r' | tr '\n' ' ') boyut=$(adb shell wm size | tr -d '\r' | tr '\n' ' ')" >> "$R"
# Yeni açılan Play imajı uzun süre güncelleme yapar; sistem sakinleşsin.
sleep 90
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell svc power stayon true
adb shell settings put global hide_error_dialogs 1
adb shell input keyevent KEYCODE_WAKEUP; adb shell wm dismiss-keyguard

cevrimdisi() { adb shell svc wifi disable; adb shell svc data disable; adb shell cmd connectivity airplane-mode enable 2>/dev/null; sleep 6; }
cevrimici() { adb shell cmd connectivity airplane-mode disable; adb shell svc wifi enable; adb shell svc data enable; sleep 25; }
cevrimdisi
echo "ucak-modu: $(adb shell settings get global airplane_mode_on | tr -d '\r') ping: $(adb shell ping -c 1 -W 2 8.8.8.8 2>&1 | tail -1 | tr -d '\r')" >> "$R"

adb install -r -g app/build/outputs/apk/debug/app-debug.apk | tail -1 >> "$R"
adb install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk | tail -1 >> "$R"

# calistir <rapor-adı> <sınıf> [ek argümanlar...]
# Altyapı kaynaklı kopma (süreç çöktü, çıktı yok) olursa bir kez yeniden denenir; ikisi de raporlanır.
calistir() {
  local ad=$1 cls=$2; shift 2
  adb logcat -c
  timeout 1800 adb shell am instrument -w -e class "$cls" "$@" $PKG.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tail -80 > "$D/$ad.txt"
  local son; son=$(tail -4 "$D/$ad.txt" | tr -d '\r' | tr '\n' ' ')
  if ! echo "$son" | grep -qE 'OK \(|FAILURES'; then
    echo "$ad (1. deneme, altyapı): $son" >> "$R"
    adb logcat -d 2>/dev/null | grep -E -A30 'FATAL EXCEPTION|ANR in|Process crashed|DEPENDENCY' | head -150 | cut -c1-260 > "$D/$ad-kopma.txt"
    sleep 30
    adb logcat -c
    timeout 1800 adb shell am instrument -w -e class "$cls" "$@" $PKG.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tail -80 > "$D/$ad.txt"
    son=$(tail -4 "$D/$ad.txt" | tr -d '\r' | tr '\n' ' ')
  fi
  echo "$ad: $son" >> "$R"
  adb logcat -d 2>/dev/null | grep -E 'FATAL EXCEPTION|E AndroidRuntime' | head -80 | cut -c1-220 > "$D/$ad-crash.txt"
}

# 1. Açılış, gezinme, veri kaydetme, geçiş, yedek güvenliği, bütün ekranlar (3 düğmeli gezinme, dikey)
T=$PKG.DataFlowTest,$PKG.MigrationTest,$PKG.AdTouchGuardTest,$PKG.LearningFlowTest,$PKG.BackupSecurityTest,$PKG.UiFlowTest
calistir ana-testler "$T" -e etiket 3dugme-dikey

# 2. Veri kalıcılığı: yaz, zorla durdur, oku, yeniden başlat, oku
P=$PKG.PersistenceTest
kisa() { timeout 300 adb shell am instrument -w -e class "$1" $PKG.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tail -3 | tr -d '\r' | tr '\n' ' '; }
echo "kalicilik-yaz: $(kisa "$P#writeData")" >> "$R"
adb shell am force-stop $PKG
echo "kalicilik-durdurma: $(kisa "$P#readData")" >> "$R"
adb reboot; sleep 20; timeout 300 adb wait-for-device
for i in $(seq 1 80); do [ "$(timeout 15 adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] && break; sleep 3; done
sleep 60
adb shell svc power stayon true; adb shell input keyevent KEYCODE_WAKEUP; adb shell wm dismiss-keyguard
cevrimdisi
for d in 1 2 3; do SONUC=$(kisa "$P#readData"); echo "$SONUC" | grep -q 'OK (1 test)' && break; sleep 45; done
echo "kalicilik-yeniden-baslatma: $SONUC (deneme $d)" >> "$R"

# 3. Kenardan kenara düzen: farklı gezinme biçimi, yazı boyutu ve yön
E=$PKG.UiFlowTest#systemBarsLayoutOnAllScreens
adb shell cmd overlay enable com.android.internal.systemui.navbar.gestural; sleep 6
echo "gezinme-modu: $(adb shell settings get secure navigation_mode | tr -d '\r') (2=hareket)" >> "$R"
calistir e2e-hareket-dikey "$E" -e etiket hareket-dikey
adb shell settings put system font_scale 1.3; sleep 3
calistir e2e-hareket-dikey-yazi130 "$E" -e etiket hareket-dikey-yazi130
adb shell settings put system font_scale 1.0
adb shell settings put system accelerometer_rotation 0; adb shell settings put system user_rotation 1; sleep 4
calistir e2e-hareket-yatay "$E" -e etiket hareket-yatay
adb shell cmd overlay disable com.android.internal.systemui.navbar.gestural; sleep 6
echo "gezinme-modu: $(adb shell settings get secure navigation_mode | tr -d '\r') (0=3 düğme)" >> "$R"
calistir e2e-3dugme-yatay "$E" -e etiket 3dugme-yatay
adb shell settings put system user_rotation 0; sleep 4

# 4. Bildirimler ve zamanlanmış hatırlatmalar (izin verilmiş)
S=$PKG.SystemIntegrationTest
calistir bildirim-goster "$S#notificationShownWhenAllowed"
calistir hatirlatma-zamaninda "$S#scheduledReminderActuallyFires"
calistir saat-dilimi "$S#timeZoneChangeReschedules"
adb shell dumpsys alarm 2>/dev/null | grep -B2 -A6 "app.ykspusula" | head -60 > "$D/alarm-kaydi.txt"
# İzin reddedilmiş
adb shell pm revoke $PKG android.permission.POST_NOTIFICATIONS
echo "bildirim-izni-reddedildi: $(adb shell dumpsys package $PKG | grep -m1 'POST_NOTIFICATIONS' | tr -d '\r ')" >> "$R"
calistir bildirim-izinsiz "$PKG.NotificationDeniedTest"
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS

# 5. Dosya seçici, PDF ve fotoğraf
calistir pdf-secici "$S#pdfPickerImportsAndRenders"
calistir foto-secici "$S#photoPickerAttachesToWrong"
calistir yedek-secici "$S#backupSaveAndRestoreThroughPicker"

# Uygulama dosyalarını al (ekranlar, düzen raporları, sistem notları)
adb exec-out run-as $PKG sh -c 'cd files && tar -cf - screens e2e sistem 2>/dev/null' 2>/dev/null | tar -x -C ci/screens-a15 2>/dev/null
mv ci/screens-a15/e2e "$D/e2e" 2>/dev/null; mv ci/screens-a15/sistem "$D/sistem" 2>/dev/null

# 6. Çevrimiçi: reklam SDK'sı ve Play Billing
cevrimici
echo "cevrimici-ping: $(adb shell ping -c 1 -W 3 8.8.8.8 2>&1 | tail -1 | tr -d '\r')" >> "$R"
for d in 1 2 3; do
  calistir reklam-$d "$PKG.AdOnlineTest#testAdLoadsAndPurchaseRemovesIt"
  adb logcat -d 2>/dev/null | grep -E 'YksAds|Ads|UserMessagingPlatform' | tail -60 | cut -c1-220 > "$D/reklam-$d-logcat.txt"
  grep -q 'OK (1 test)' "$D/reklam-$d.txt" && break
  sleep 30
done
adb exec-out run-as $PKG cat files/reklam-yerel.png > ci/screens-a15/reklam-yerel.png 2>/dev/null
calistir billing "$S#billingClientStarts"
adb exec-out run-as $PKG cat files/sistem/billingClientStarts.txt > "$D/sistem-billing.txt" 2>/dev/null
adb logcat -d 2>/dev/null | grep -iE 'BillingClient|BillingLogger' | tail -40 | cut -c1-220 > "$D/billing-logcat.txt"

# 7. Yan yükleme (release ayarlı) sürüm: temiz kurulum, soğuk açılış, çökme
adb uninstall $PKG.test > /dev/null; adb uninstall $PKG > /dev/null
echo "release-kurulum: $(adb install -r app/build/outputs/apk/sideload/app-sideload.apk 2>&1 | tail -1)" >> "$R"
adb logcat -c
for n in 1 2 3; do
  adb shell am force-stop $PKG; sleep 2
  echo "soguk-acilis-$n: $(adb shell am start -W -n $PKG/.MainActivity 2>&1 | grep -E 'TotalTime|Status' | tr -d '\r' | tr '\n' ' ')" >> "$R"
  sleep 8
done
sleep 10
echo "release-surec: $(adb shell pidof $PKG | tr -d '\r')" >> "$R"
adb exec-out screencap -p > ci/screens-a15/release-acilis.png
adb logcat -d | grep -E 'FATAL EXCEPTION|E AndroidRuntime' | head -120 | cut -c1-200 > "$D/release-crash.txt"
echo "release-crash: $(grep -c 'Process: app.ykspusula' "$D/release-crash.txt")" >> "$R"
echo "release-anr: $(adb logcat -d | grep -c 'ANR in app.ykspusula')" >> "$R"
adb logcat -d 2>/dev/null | grep -E 'app\.ykspusula' | grep -E 'has died|Killing|ANR in|Force stopping' | cut -c1-260 > "$D/release-olum.txt"
echo "release-olum-kaydi: $(wc -l < "$D/release-olum.txt")" >> "$R"
adb shell dumpsys meminfo $PKG | grep -E 'TOTAL PSS|TOTAL:' | head -2 | tr -d '\r' >> "$R"

adb emu kill; sleep 5
pkill -f qemu-system; sleep 2
"$SDK/avdmanager" delete avd -n "$NAME" > /dev/null 2>&1
true
