#!/bin/bash
# Android 16 Play imajında, parmak reklamın üstündeyken sayfa kaydırılır.
# Amaç: kaydırmanın reklamı yanlışlıkla açıp açmadığını doğrudan görmek.
# Uygulama kodu değişmez, tam test turunun yerine geçmez.
I="system-images;android-36;google_apis_playstore;x86_64"
for n in 1 2 3 4; do
  REKLAM_SINIF='app.ykspusula.AdOnlineTest#dragOverAdDoesNotOpenIt' CEVRIMICI=1 YALNIZ_REKLAM=1 REKLAM_TUR=3 BELLEK=4096 timeout -k 10 900 bash tools/smoke.sh "t$n" "$I" app.ykspusula.MigrationTest || echo "t$n bitis kodu: $?" >> ci/logs/matris.txt
done
# Düzeltilen reklam testi de iki Play imajında yeniden denenir.
CEVRIMICI=1 YALNIZ_REKLAM=1 REKLAM_TUR=4 BELLEK=4096 timeout -k 10 900 bash tools/smoke.sh t5 "$I" app.ykspusula.MigrationTest || echo "t5 bitis kodu: $?" >> ci/logs/matris.txt
CEVRIMICI=1 YALNIZ_REKLAM=1 REKLAM_TUR=3 timeout -k 10 900 bash tools/smoke.sh t6 "system-images;android-34;google_apis_playstore;x86_64" app.ykspusula.MigrationTest || echo "t6 bitis kodu: $?" >> ci/logs/matris.txt
true
