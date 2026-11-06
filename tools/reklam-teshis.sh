#!/bin/bash
# Android 16 Play imajında, parmak reklamın üstündeyken sayfa hızlı hızlı kaydırılır.
# Koruyucu devreye girdiğinde kayıt düşer; reklam açılırsa test başarısız olur.
I="system-images;android-36;google_apis_playstore;x86_64"
for n in 1 2 3 4; do
  REKLAM_SINIF='app.ykspusula.AdOnlineTest#dragOverAdDoesNotOpenIt' CEVRIMICI=1 YALNIZ_REKLAM=1 REKLAM_TUR=3 BELLEK=4096 timeout -k 10 900 bash tools/smoke.sh "t$n" "$I" app.ykspusula.MigrationTest || echo "t$n bitis kodu: $?" >> ci/logs/matris.txt
done
true
