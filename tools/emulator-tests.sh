#!/bin/bash
set +e
adb wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell svc wifi disable; adb shell svc data disable
./gradlew --no-daemon --console=plain :app:connectedDebugAndroidTest 2>&1 | tee ci/logs/connected.txt
adb logcat -d > ci/logs/logcat.txt
adb shell run-as app.ykspusula sh -c 'ls files/screens' > /dev/null 2>&1
adb exec-out run-as app.ykspusula tar -c -C files screens 2>/dev/null | tar -x -C ci/ 2>/dev/null
cp -r app/build/reports/androidTests/connected ci/logs/connected-report 2>/dev/null
for f in app/build/outputs/androidTest-results/connected/debug/*.xml app/build/outputs/androidTest-results/connected/*.xml; do [ -f "$f" ] && cp "$f" ci/logs/; done
true
