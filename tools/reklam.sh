#!/bin/bash
# Yalnızca çevrimiçi reklam testi. Google test kimliklerini kullanır.
T=app.ykspusula.MigrationTest
CEVRIMICI=1 bash tools/smoke.sh a14g "system-images;android-34;google_apis;x86_64" "$T"
CEVRIMICI=1 BELLEK=4096 bash tools/smoke.sh a16 "system-images;android-36;google_apis;x86_64" "$T"
true
