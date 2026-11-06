#!/bin/bash
# Android 16 Play imajında reklam testini soğuk açılıştan sonra tekrar tekrar çalıştırır.
# İlk iki açılışta test gerçek parmak kaydırması yapar, sonrakilerde dokunmasız kaydırır.
# Amaç: etkinliğin arka plana düşmesi kaydırmadan mı geliyor, bunu kesinleştirmek.
# Uygulama kodu değişmez, tam test turunun yerine geçmez.
I="system-images;android-36;google_apis_playstore;x86_64"
for n in 1 2 3 4 5 6 7; do
  ARG=""; [ "$n" -le 2 ] && ARG="-e kaydirma dokunma"
  REKLAM_ARG="$ARG" CEVRIMICI=1 YALNIZ_REKLAM=1 REKLAM_TUR=4 BELLEK=4096 timeout -k 10 720 bash tools/smoke.sh "t$n" "$I" app.ykspusula.MigrationTest || echo "t$n bitis kodu: $?" >> ci/logs/matris.txt
done
true
