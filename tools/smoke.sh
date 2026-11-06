#!/bin/bash
# Kullanım: smoke.sh ad "sistem imajı" "test sınıfı (boş olabilir)"
# Belirtilen Android sürümünde temiz kurulum, açılış ve isteğe bağlı testleri dener.
NAME=$1; IMG=$2; CLS=$3
R=ci/logs/uyum-$NAME.txt
PKG=app.ykspusula
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export ANDROID_AVD_HOME="$HOME/.android/avd"
SDK="$ANDROID_HOME/cmdline-tools/latest/bin"
S=ci/logs/kurulum-$NAME.txt
mkdir -p "$ANDROID_AVD_HOME"
yes | "$SDK/sdkmanager" --licenses > /dev/null 2>&1
timeout 10 adb emu kill 2>/dev/null; sleep 4
"$SDK/sdkmanager" "$IMG" "emulator" "platform-tools" 2>&1 | tr '\r' '\n' | tail -3 > "$S"
echo no | "$SDK/avdmanager" create avd -n "$NAME" -k "$IMG" -d pixel_6 --force >> "$S" 2>&1
df -h / | tail -1 >> "$S"
if [ ! -f "$ANDROID_AVD_HOME/$NAME.avd/config.ini" ]; then echo "acilis=yok sanal-cihaz-olusmadi" | tee "$R"; exit 0; fi
printf 'hw.ramSize=3072\ndisk.dataPartition.size=4096M\nhw.keyboard=yes\n' >> "$ANDROID_AVD_HOME/$NAME.avd/config.ini"
"$ANDROID_HOME/emulator/emulator" -avd "$NAME" -accel on -cores 2 -memory ${BELLEK:-3072} -no-metrics -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot -camera-back none -camera-front none > "ci/logs/emulator-$NAME.txt" 2>&1 &
EMU=$!
B=""
for i in $(seq 1 170); do
  B=$(timeout 15 adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r'); [ "$B" = "1" ] && break
  kill -0 $EMU 2>/dev/null || { echo "emulator-kapandi deneme=$i" >> "$S"; break; }
  sleep 5
done
echo "acilis=$B deneme=$i" | tee "$R"
if [ "$B" != "1" ]; then
  timeout 10 adb devices >> "$R"
  timeout 15 adb shell getprop init.svc.bootanim >> "$R" 2>&1
  timeout 30 adb logcat -d 2>/dev/null | tail -300 | cut -c1-220 > "ci/logs/logcat-$NAME.txt"
  tail -40 "ci/logs/emulator-$NAME.txt" > "ci/logs/emulator-son-$NAME.txt"
  pkill -f qemu-system; sleep 5
  rm -rf "$ANDROID_HOME/$(echo "$IMG" | tr ';' '/')" "$ANDROID_AVD_HOME/$NAME.avd"
  exit 0
fi
echo "android=$(adb shell getprop ro.build.version.release | tr -d '\r') api=$(adb shell getprop ro.build.version.sdk | tr -d '\r')" >> "$R"
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell svc wifi disable; adb shell svc data disable
adb shell svc power stayon true
# Yavaş emülatörde sistem arayüzünün "yanıt vermiyor" penceresi testlerin odağını çalmasın.
adb shell settings put global hide_error_dialogs 1
[ -n "$HAREKET" ] && { adb shell cmd overlay enable com.android.internal.systemui.navbar.gestural; sleep 3; }
adb shell cmd connectivity airplane-mode enable 2>/dev/null
sleep 6
echo "ucak-modu: $(adb shell settings get global airplane_mode_on | tr -d '\r') ping: $(adb shell ping -c 1 -W 2 8.8.8.8 2>&1 | tail -1 | tr -d '\r')" >> "$R"
if [ -n "$CLS" ]; then
  adb install -r -g app/build/outputs/apk/debug/app-debug.apk | tail -1 >> "$R"
  adb install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk | tail -1 >> "$R"
  timeout 1800 adb shell am instrument -w -e class "$CLS" $PKG.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tail -60 > "ci/logs/uyum-$NAME-test.txt"
  echo "test: $(tail -4 ci/logs/uyum-$NAME-test.txt | tr '\n' ' ')" >> "$R"
  if ! tail -4 "ci/logs/uyum-$NAME-test.txt" | grep -q 'OK ('; then
    # Başarısız tur saklanır, ikinci tur ayrı satırda raporlanır.
    cp "ci/logs/uyum-$NAME-test.txt" "ci/logs/uyum-$NAME-test-ilk.txt"
    sleep 30
    timeout 1800 adb shell am instrument -w -e class "$CLS" $PKG.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tail -60 > "ci/logs/uyum-$NAME-test.txt"
    echo "test-ikinci-tur: $(tail -4 ci/logs/uyum-$NAME-test.txt | tr '\n' ' ')" >> "$R"
  fi
  if [ -n "$CEVRIMICI" ]; then
    adb shell cmd connectivity airplane-mode disable; adb shell svc wifi enable; adb shell svc data enable
    for i in $(seq 1 12); do
      PING=$(timeout 10 adb shell ping -c 1 -W 3 8.8.8.8 2>&1 | tail -2 | tr -d '\r' | tr '\n' ' ')
      echo "$PING" | grep -q ' 0% packet loss' && break
      sleep 5
    done
    echo "ping-cevrimici: $PING" >> "$R"
    echo "dns-cevrimici: $(timeout 20 adb shell ping -c 1 -W 3 googleads.g.doubleclick.net 2>&1 | head -1 | tr -d '\r' | cut -c1-90)" >> "$R"
    if [ -n "$TANI" ]; then
      adb shell setprop log.tag.GoogleMobileAds VERBOSE; adb shell setprop log.tag.Ads VERBOSE
      adb logcat -c
      timeout 280 adb shell am instrument -w -e class $PKG.AdDiagTest $PKG.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tail -30 > "ci/logs/tani-$NAME.txt"
      adb logcat -d 2>/dev/null > /tmp/tani.txt
      grep -E 'YksTani' /tmp/tani.txt | cut -c1-900 > "ci/logs/tani-sonuc-$NAME.txt"
      grep -vE 'GoogleMobileAdsNetwork|BillingClient|EmojiCompat|AppsFilter' /tmp/tani.txt | grep -E ' (W|E) |GoogleMobileAds|Ads  ' | tail -700 | cut -c1-500 > "ci/logs/tani-logcat-$NAME.txt"
    fi
    for d in 1 2 3; do
      adb logcat -c
      timeout 300 adb shell am instrument -w -e class $PKG.AdOnlineTest $PKG.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tail -30 > "ci/logs/reklam-$NAME.txt"
      adb logcat -d 2>/dev/null | grep -v 'YksAds' | grep -iE 'gma|ads|webview|chromium|cr_|exception|native' | tail -500 | cut -c1-400 > "ci/logs/reklam-logcat-$NAME.txt"
      tail -3 "ci/logs/reklam-$NAME.txt" | grep -q 'OK (1 test)' && break
      sleep 20
    done
    echo "reklam-denemesi: $d" >> "$R"
    echo "reklam-testi: $(tail -4 ci/logs/reklam-$NAME.txt | tr '\n' ' ' | cut -c1-200)" >> "$R"
    grep -m1 'yüklenemedi' "ci/logs/reklam-$NAME.txt" | cut -c1-300 >> "$R"
    adb exec-out screencap -p > "ci/screens/reklam-$NAME.png"
    adb shell cmd connectivity airplane-mode enable; sleep 5
  fi
  if [ -n "$KALICILIK" ]; then
    P=$PKG.PersistenceTest
    calistir() { timeout 300 adb shell am instrument -w -e class "$1" $PKG.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tail -3 | tr -d '\r' | tr '\n' ' '; }
    echo "kalicilik-yaz: $(calistir "$P#writeData")" >> "$R"
    adb shell am force-stop $PKG
    echo "kalicilik-durdurma: $(calistir "$P#readData")" >> "$R"
    adb reboot; sleep 20; timeout 300 adb wait-for-device
    for i in $(seq 1 80); do [ "$(timeout 15 adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] && break; sleep 3; done
    # Yeni sürüm emülatörü açılıştan sonra uzun süre yoğun çalışır; sistem sakinleşince denenir.
    sleep 60
    adb shell svc power stayon true
    for d in 1 2 3; do
      adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null 2>&1
      adb logcat -c
      SONUC=$(calistir "$P#readData")
      echo "$SONUC" | grep -q 'OK (1 test)' && break
      adb logcat -d 2>/dev/null | grep -E 'FATAL EXCEPTION|AndroidRuntime|ykspusula' | tail -40 | cut -c1-220 > "ci/logs/kalicilik-logcat-$NAME.txt"
      sleep 45
    done
    echo "kalicilik-yeniden-baslatma: $SONUC (deneme $d)" >> "$R"
  fi
  mkdir -p ci/screens-$NAME
  adb exec-out run-as $PKG sh -c 'cd files && tar -cf - screens' 2>/dev/null | tar -x -C ci/screens-$NAME 2>/dev/null
  adb uninstall $PKG.test; adb uninstall $PKG
fi
echo "release-kurulum: $(adb install -r app/build/outputs/apk/release/app-release.apk 2>&1 | tail -1)" >> "$R"
adb logcat -c
adb shell input keyevent KEYCODE_WAKEUP; adb shell wm dismiss-keyguard; sleep 2
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null 2>&1
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
sleep 15
echo "release-surec: $(adb shell pidof $PKG)" >> "$R"
echo "ekran-durumu: $(adb shell dumpsys power | grep -m1 mWakefulness= | tr -d '\r ')" >> "$R"
echo "ondeki-etkinlik: $(adb shell dumpsys activity activities | grep -m1 -E 'topResumedActivity|mResumedActivity' | tr -d '\r')" >> "$R"
echo "ilk-cizim: $(adb logcat -d | grep -m1 'Displayed app.ykspusula' | cut -c1-140)" >> "$R"
adb shell uiautomator dump /sdcard/ekran.xml > /dev/null 2>&1
echo "ekrandaki-yazilar: $(adb exec-out cat /sdcard/ekran.xml 2>/dev/null | grep -o 'text="[^"]*"' | grep -v 'text=""' | head -12 | tr '\n' ' ')" >> "$R"
adb exec-out screencap -p > "ci/screens/uyum-$NAME.png"
adb logcat -d | grep -E 'FATAL EXCEPTION|E AndroidRuntime' | head -120 | cut -c1-200 > "ci/logs/release-crash-$NAME.txt"
echo "release-crash-tum-cihaz: $(wc -l < "ci/logs/release-crash-$NAME.txt")" >> "$R"
echo "release-crash: $(grep -c 'Process: app.ykspusula' "ci/logs/release-crash-$NAME.txt")" >> "$R"
adb emu kill; sleep 5
pkill -f qemu-system; sleep 2
# Disk dolmasın: bu sürümün imajı ve sanal cihazı silinir.
"$SDK/avdmanager" delete avd -n "$NAME" > /dev/null 2>&1
rm -rf "$ANDROID_HOME/$(echo "$IMG" | tr ';' '/')" "$ANDROID_AVD_HOME/$NAME.avd"
true
