#!/bin/bash
# Kullanım: cihaz.sh <ad>   (a8 a11 a13 a14 a15 a16 a14p a16p)
# Tek bir Android sürümünü çalıştırır. Çıkış kodu kalite kapısıdır: 0 yalnızca bütün aşamalar PASS ise.
P=app.ykspusula
T=$P.DataFlowTest,$P.MigrationTest,$P.AdTouchGuardTest,$P.LearningFlowTest,$P.BackupSecurityTest
case "$1" in
  a8)   BELLEK=1536 bash tools/smoke.sh a8 "system-images;android-26;default;x86_64" "$T" ;;
  a11)  bash tools/run-emulator.sh ;;
  a13)  bash tools/smoke.sh a13 "system-images;android-33;aosp_atd;x86_64" "$T" ;;
  a14)  KALICILIK=1 HAREKET=1 bash tools/smoke.sh a14 "system-images;android-34;aosp_atd;x86_64" "$T,$P.UiFlowTest" ;;
  a15)  bash tools/android15.sh ;;
  a16)
    KALICILIK=1 HAREKET=1 BELLEK=4096 bash tools/smoke.sh a16 "system-images;android-36;google_apis;x86_64" "$T,$P.UiFlowTest"; k=$?
    if [ $k = 3 ]; then
      # google_apis imajı açılmadıysa aynı API düzeyindeki default imajla denenir; ilk deneme kayıtlarda kalır.
      mv ci/logs/durum-a16.tsv ci/logs/a16-ilk-imaj-durum.tsv
      echo "android-36 google_apis imajı açılmadı; android-36 default imajıyla çalıştırıldı (bkz. a16-ilk-imaj-durum.tsv)" >> ci/logs/notlar-a16.txt
      KALICILIK=1 HAREKET=1 BELLEK=4096 bash tools/smoke.sh a16 "system-images;android-36;default;x86_64" "$T,$P.UiFlowTest"; k=$?
    fi
    exit $k ;;
  # Yerel test reklamı uygulama yükleme reklamıdır; SDK onu Play Store'suz cihazda reddeder. Çevrimiçi reklam testi Play imajlarında.
  a14p) TEKRAR=6 CEVRIMICI=1 REKLAM_KAYDIRMA=1 bash tools/smoke.sh a14p "system-images;android-34;google_apis_playstore;x86_64" $P.MigrationTest ;;
  a16p) TEKRAR=6 CEVRIMICI=1 REKLAM_KAYDIRMA=1 BELLEK=4096 bash tools/smoke.sh a16p "system-images;android-36;google_apis_playstore;x86_64" $P.MigrationTest ;;
  *) echo "bilinmeyen cihaz: $1"; exit 2 ;;
esac
