#!/bin/bash
# Testlerde ağ kapalıdır. Sonuçlar ci/ klasörüne yazılır.
set +e
# adb komutu cihaz yokken sonsuza dek bekleyebilir. Her çağrı süre sınırıyla çalışır,
# emülatör düştüyse hemen hata döner. Böylece tek bir kayıp cihaz bütün turu kilitlemez.
adb() {
  local A="$ANDROID_HOME/platform-tools/adb"
  case "$1" in
    wait-for-device|emu|devices|start-server|kill-server) ;;
    *) timeout 20 "$A" get-state > /dev/null 2>&1 || { echo "adb: cihaz yok ($1)" >&2; return 1; } ;;
  esac
  timeout 1200 "$A" "$@"
}
R=ci/logs/kabul.txt
: > $R
PKG=app.ykspusula
timeout 300 "$ANDROID_HOME/platform-tools/adb" wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell svc wifi disable; adb shell svc data disable
adb shell cmd connectivity airplane-mode enable
sleep 8
echo "ucak-modu: $(adb shell settings get global airplane_mode_on | tr -d '\r')" >> $R
echo "wifi: $(adb shell settings get global wifi_on | tr -d '\r')" >> $R
echo "ping: $(adb shell ping -c 1 -W 2 8.8.8.8 2>&1 | tail -1 | tr -d '\r')" >> $R

GR="./gradlew --no-daemon --console=plain"
$GR :app:connectedDebugAndroidTest 2>&1 | tee ci/logs/connected.txt | tail -40
echo "connected=$(grep -cE 'BUILD SUCCESSFUL' ci/logs/connected.txt)" >> $R
mkdir -p ci/logs/xml; find app/build/outputs/androidTest-results -name '*.xml' -exec cp {} ci/logs/xml/ \;
adb shell run-as $PKG tar -cf /data/local/tmp/s.tar -C files screens 2>/dev/null
adb exec-out run-as $PKG sh -c 'cd files && tar -cf - screens' > /tmp/screens.tar 2>/dev/null
tar -xf /tmp/screens.tar -C ci/ 2>/dev/null
echo "ekran=$(ls ci/screens | wc -l)" >> $R

./gradlew --no-daemon --console=plain --offline :app:installDebug :app:installDebugAndroidTest 2>&1 | tail -3
INST="$PKG.test/androidx.test.runner.AndroidJUnitRunner"
run() {
  for t in 1 2 3; do
    o=$(adb shell am instrument -w -e class "$1" $INST 2>&1 | tail -4 | tr '\n' ' ')
    case "$o" in *"at com.android"*) sleep 8;; *) break;; esac
  done
  echo "$o"
}

# Akış ekranlarının görüntüleri (Gradle uygulamayı kaldırdığı için ayrıca çalıştırılır)
echo "akislar: $(run $PKG.UiFlowTest#onboardingAndTabs) $(run $PKG.UiFlowTest#studyLogUpdatesStats) $(run $PKG.UiFlowTest#examSessionSurvivesRecreate)" >> $R
# Ana düğmeler aynı sürümde beş kez arka arkaya denenir; ara sıra çıkan hatanın kaynağını görmek için.
for n in 1 2 3 4 5; do
  adb shell am instrument -w -e class $PKG.UiFlowTest#mainButtonsDoTheirJob $INST 2>&1 | tail -60 > ci/logs/dugmeler-$n.txt
  echo "dugmeler-$n: $(tail -4 ci/logs/dugmeler-$n.txt | tr '\n' ' ' | cut -c1-120)" >> $R
  adb exec-out run-as $PKG cat files/hata.png > ci/screens/hata-$n.png 2>/dev/null
  adb shell run-as $PKG rm -f files/hata.png
done
# Geçiş sürerken etkinlik kapanması: Compose iç hatası gerçek kullanımda da çıkar mı?
for n in 1 2 3; do
  adb shell am instrument -w -e class $PKG.StresTest $INST 2>&1 | tail -60 > ci/logs/stres-$n.txt
  echo "stres-$n: $(tail -4 ci/logs/stres-$n.txt | tr '\n' ' ' | cut -c1-120)" >> $R
done
# Standart telefon: mağaza görselleri bu ölçüden alınır
echo "standart: $(run $PKG.UiFlowTest#allScreensOpenBothThemes)" >> $R
adb exec-out run-as $PKG sh -c 'cd files && tar -cf - screens' > /tmp/screens.tar 2>/dev/null
tar -xf /tmp/screens.tar -C ci/ 2>/dev/null
# Büyük yazı
adb shell settings put system font_scale 1.5
echo "buyuk-yazi: $(run $PKG.UiFlowTest#allScreensOpenBothThemes)" >> $R
adb exec-out run-as $PKG sh -c 'cd files && tar -cf - screens' | tar -x -C /tmp/ 2>/dev/null
mkdir -p ci/screens-buyuk; cp /tmp/screens/*-s-dark-0[0-9]-* ci/screens-buyuk/ 2>/dev/null
adb shell settings put system font_scale 1.0
# Yatay ekran
adb shell settings put system accelerometer_rotation 0; adb shell settings put system user_rotation 1
echo "yatay: $(run $PKG.UiFlowTest#allScreensOpenBothThemes)" >> $R
adb shell settings put system user_rotation 0
# Küçük ve tablet ekran
adb shell wm size 480x800; adb shell wm density 240
adb shell settings put system font_scale 1.0
echo "kucuk: $(run $PKG.UiFlowTest#allScreensOpenBothThemes)" >> $R
adb shell wm size 1600x2560; adb shell wm density 280
echo "tablet: $(run $PKG.UiFlowTest#allScreensOpenBothThemes)" >> $R
adb shell wm size reset; adb shell wm density reset
adb exec-out run-as $PKG sh -c 'cd files && tar -cf - screens' > /tmp/screens.tar 2>/dev/null
tar -xf /tmp/screens.tar -C ci/ 2>/dev/null
echo "ekran-toplam=$(ls ci/screens | wc -l)" >> $R

# Kalıcılık: yaz, zorla durdur, oku, yeniden başlat, oku
echo "yaz: $(run $PKG.PersistenceTest#writeData)" >> $R
adb shell am force-stop $PKG
echo "durdurma-sonrasi: $(run $PKG.PersistenceTest#readData)" >> $R
adb reboot; sleep 20
if ! timeout 180 "$ANDROID_HOME/platform-tools/adb" wait-for-device; then
  # Emülatör yeniden başlarken düşerse aynı veri diskiyle tekrar açılır, veri kalıcılığı yine ölçülür.
  echo "yeniden-baslatma: emulator kapandi, ayni diskle aciliyor (qemu: $(pgrep -c -f qemu-system))" >> $R
  pkill -f qemu-system; sleep 3
  "$ANDROID_HOME/emulator/emulator" -avd test -accel on -cores 2 -memory 3072 -no-metrics -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot -camera-back none -camera-front none >> ci/logs/emulator-ikinci.txt 2>&1 &
  timeout 240 "$ANDROID_HOME/platform-tools/adb" wait-for-device
fi
for i in $(seq 1 60); do [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] && break; sleep 3; done
sleep 5
echo "yeniden-baslatma-sonrasi: $(run $PKG.PersistenceTest#readData)" >> $R
adb shell dumpsys alarm | grep -c ykspusula | sed 's/^/alarm: /' >> $R

# Bağımsız release APK: temiz kurulum ve açılış
adb uninstall $PKG
adb uninstall $PKG.test
adb install -r app/build/outputs/apk/sideload/app-sideload.apk 2>&1 | tail -1 | sed 's/^/release-kurulum: /' >> $R
adb logcat -c
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
sleep 12
echo "release-surec: $(adb shell pidof $PKG)" >> $R
adb exec-out screencap -p > ci/screens/release-acilis.png
adb logcat -d | grep -E 'FATAL EXCEPTION|E AndroidRuntime' | head -20 > ci/logs/release-crash.txt
echo "release-crash-satir: $(wc -l < ci/logs/release-crash.txt)" >> $R
# Performans: release sürümünde soğuk açılış ve bellek
for n in 1 2 3; do adb shell am force-stop $PKG; sleep 2; echo "soguk-acilis-$n: $(adb shell am start -W -n $PKG/.MainActivity | grep -E 'TotalTime' | tr -d '\r')" >> $R; sleep 3; done
adb shell dumpsys meminfo $PKG | grep -E 'TOTAL( PSS)?:|TOTAL  ' | head -2 | tr -d '\r' > ci/logs/bellek.txt
echo "anr: $(adb logcat -d | grep -c 'ANR in')" >> $R
adb logcat -d > ci/logs/logcat.txt
true
