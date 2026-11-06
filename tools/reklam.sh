#!/bin/bash
# Yalnızca çevrimiçi reklam testi. Google test kimliklerini kullanır.
./gradlew --no-daemon --console=plain --offline :app:dependencies --configuration releaseRuntimeClasspath > ci/logs/bagimlilik.txt 2>&1
TANI=1 CEVRIMICI=1 bash tools/smoke.sh a14g "system-images;android-34;google_apis;x86_64" app.ykspusula.MigrationTest
true
