#!/bin/bash
# Emülatörü kurar, açar ve kabul testlerini çalıştırır. Her adımın günlüğü ci/logs altına yazılır.
set -x
L=ci/logs/emulator-setup.txt
exec > >(tee -a "$L") 2>&1
API=30
IMG="system-images;android-$API;default;x86_64"
export ANDROID_AVD_HOME="$HOME/.android/avd"
mkdir -p "$ANDROID_AVD_HOME"
SDK="$ANDROID_HOME/cmdline-tools/latest/bin"
yes | "$SDK/sdkmanager" --licenses > /dev/null
"$SDK/sdkmanager" "$IMG" "emulator" "platform-tools" | tail -3
df -h / | tail -1
echo no | "$SDK/avdmanager" create avd -n test -k "$IMG" -d pixel_6 --force
cat >> "$ANDROID_AVD_HOME/test.avd/config.ini" <<EOF
hw.ramSize=3072
disk.dataPartition.size=4096M
hw.keyboard=yes
EOF
"$ANDROID_HOME/platform-tools/adb" start-server
"$ANDROID_HOME/emulator/emulator" -accel-check
ls -l /dev/kvm
"$ANDROID_HOME/emulator/emulator" -avd test -accel on -cores 2 -memory 3072 -no-metrics -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot -camera-back none -camera-front none > ci/logs/emulator.txt 2>&1 &
EMU=$!
ADB="$ANDROID_HOME/platform-tools/adb"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
for i in $(seq 1 180); do
  B=$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  [ "$B" = "1" ] && break
  [ $((i % 12)) = 0 ] && { "$ADB" devices; "$ADB" shell getprop init.svc.bootanim; }
  kill -0 $EMU 2>/dev/null || { echo "EMULATOR KAPANDI"; tail -30 ci/logs/emulator.txt; exit 1; }
  sleep 5
done
echo "acilis: $B, deneme: $i"
[ "$B" = "1" ] || { echo "ACILIS ZAMAN ASIMI"; tail -30 ci/logs/emulator.txt; exit 1; }
# Süre aşılırsa betik yetim kalıp sonraki sürümün emülatörünü bozmasın.
timeout -k 10 3900 bash tools/emulator-tests.sh || echo "emulator-tests bitis kodu: $?"
"$ADB" emu kill; sleep 5
pkill -f qemu-system; sleep 2
"$SDK/avdmanager" delete avd -n test > /dev/null 2>&1
rm -rf "$ANDROID_HOME/$(echo "$IMG" | tr ';' '/')" "$ANDROID_AVD_HOME/test.avd"
true
