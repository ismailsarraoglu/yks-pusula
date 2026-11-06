#!/bin/bash
# Android 16 Play imajında reklam testini soğuk açılıştan sonra tekrar tekrar çalıştırır.
# Amaç: nadir görülen "Process crashed" olayının nedenini Android'in kendi kaydından okumak.
# Uygulama kodu değişmez, tam test turunun yerine geçmez.
I="system-images;android-36;google_apis_playstore;x86_64"
for n in 1 2 3 4 5; do
  CEVRIMICI=1 YALNIZ_REKLAM=1 REKLAM_TUR=4 BELLEK=4096 timeout -k 10 1020 bash tools/smoke.sh "t$n" "$I" app.ykspusula.MigrationTest || echo "t$n bitis kodu: $?" >> ci/logs/matris.txt
done
true
