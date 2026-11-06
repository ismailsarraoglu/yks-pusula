#!/bin/bash
# Yalnızca çevrimiçi reklam testi. Google test kimliklerini kullanır.
# Yerel test reklamı uygulama yükleme reklamıdır; Play Store'suz imajda SDK onu reddeder, bu yüzden Play Store'lu imaj gerekir.
TANI=1 CEVRIMICI=1 bash tools/smoke.sh a14p "system-images;android-34;google_apis_playstore;x86_64" app.ykspusula.MigrationTest
true
