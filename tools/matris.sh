#!/bin/bash
# Android sürüm matrisi. Her sürümde veri, geçiş ve arayüz testleri çalışır.
T=app.ykspusula.DataFlowTest,app.ykspusula.MigrationTest,app.ykspusula.AdTouchGuardTest,app.ykspusula.LearningFlowTest,app.ykspusula.BackupSecurityTest
# Bir sürüm takılırsa süre sınırıyla kesilir, sonraki sürümler yine denenir.
smoke() { timeout -k 10 1800 bash tools/smoke.sh "$@" || echo "smoke $1 bitis kodu: $?" >> ci/logs/matris.txt; }
BELLEK=1536 smoke a8 "system-images;android-26;default;x86_64" "$T"
smoke a13 "system-images;android-33;aosp_atd;x86_64" "$T"
KALICILIK=1 HAREKET=1 smoke a14 "system-images;android-34;aosp_atd;x86_64" "$T,app.ykspusula.UiFlowTest"
KALICILIK=1 HAREKET=1 BELLEK=4096 smoke a16 "system-images;android-36;google_apis;x86_64" "$T,app.ykspusula.UiFlowTest"
if ! grep -q 'acilis=1' ci/logs/uyum-a16.txt; then
  KALICILIK=1 HAREKET=1 smoke a16b "system-images;android-36;default;x86_64" "$T,app.ykspusula.UiFlowTest"
fi
# Yerel test reklamı uygulama yükleme reklamıdır; SDK onu Play Store'suz cihazda reddeder.
# Bu yüzden çevrimiçi reklam testi Play Store'lu imajlarda çalışır.
TEKRAR=6 CEVRIMICI=1 smoke a14p "system-images;android-34;google_apis_playstore;x86_64" app.ykspusula.MigrationTest
TEKRAR=6 CEVRIMICI=1 BELLEK=4096 smoke a16p "system-images;android-36;google_apis_playstore;x86_64" app.ykspusula.MigrationTest
true
