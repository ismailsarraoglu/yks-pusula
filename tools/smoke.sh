#!/bin/bash
# Kullanım: smoke.sh ad "sistem imajı" "test sınıfı (boş olabilir)"
# Belirtilen Android sürümünde temiz kurulum, açılış ve isteğe bağlı testleri dener.
NAME=$1; IMG=$2; CLS=$3
R=ci/logs/uyum-$NAME.txt
PKG=app.ykspusula
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export ANDROID_AVD_HOME="$HOME/.android/avd"
SDK="$ANDROID_HOME/cmdline-tools/latest/bin"
adb emu kill 2>/dev/null; sleep 4
"$SDK/sdkmanager" "$IMG" | tail -1
echo no | "$SDK/avdmanager" create avd -n "$NAME" -k "$IMG" -d pixel_6 --force
printf 'hw.ramSize=3072\ndisk.dataPartition.size=4096M\nhw.keyboard=yes\n' >> "$ANDROID_AVD_HOME/$NAME.avd/config.ini"
"$ANDROID_HOME/emulator/emulator" -avd "$NAME" -accel on -cores 2 -memory 3072 -no-metrics -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot > "ci/logs/emulator-$NAME.txt" 2>&1 &
B=""
for i in $(seq 1 170); do
  B=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r'); [ "$B" = "1" ] && break; sleep 5
done
echo "acilis=$B deneme=$i" | tee "$R"
if [ "$B" != "1" ]; then
  adb devices >> "$R"
  adb logcat -d 2>/dev/null | tail -150 > "ci/logs/logcat-$NAME.txt"
  tail -40 "ci/logs/emulator-$NAME.txt" > "ci/logs/emulator-son-$NAME.txt"
  pkill -f qemu-system; sleep 5
  exit 0
fi
echo "android=$(adb shell getprop ro.build.version.release | tr -d '\r') api=$(adb shell getprop ro.build.version.sdk | tr -d '\r')" >> "$R"
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell svc wifi disable; adb shell svc data disable
if [ -n "$CLS" ]; then
  adb install -r -g app/build/outputs/apk/debug/app-debug.apk | tail -1 >> "$R"
  adb install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk | tail -1 >> "$R"
  adb shell am instrument -w -e class "$CLS" $PKG.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tail -60 > "ci/logs/uyum-$NAME-test.txt"
  echo "test: $(tail -4 ci/logs/uyum-$NAME-test.txt | tr '\n' ' ')" >> "$R"
  mkdir -p ci/screens-$NAME
  adb exec-out run-as $PKG sh -c 'cd files && tar -cf - screens' 2>/dev/null | tar -x -C ci/screens-$NAME 2>/dev/null
  adb uninstall $PKG.test; adb uninstall $PKG
fi
echo "release-kurulum: $(adb install -r app/build/outputs/apk/release/app-release.apk 2>&1 | tail -1)" >> "$R"
adb logcat -c
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
sleep 15
echo "release-surec: $(adb shell pidof $PKG)" >> "$R"
adb exec-out screencap -p > "ci/screens/uyum-$NAME.png"
echo "release-crash: $(adb logcat -d | grep -cE 'FATAL EXCEPTION|E AndroidRuntime')" >> "$R"
adb emu kill; sleep 3
true
