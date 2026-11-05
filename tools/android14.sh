#!/bin/bash
# Android 14 için imajları sırayla dener; açılan ilk imajda veri ve arayüz testlerini çalıştırır.
TESTS=app.ykspusula.DataFlowTest,app.ykspusula.UiFlowTest
i=0
for IMG in "system-images;android-34;aosp_atd;x86_64" "system-images;android-34;default;x86_64" "system-images;android-34;google_apis;x86_64"; do
  i=$((i+1))
  bash tools/smoke.sh a14-$i "$IMG" "$TESTS"
  echo "imaj-$i: $IMG: $(head -1 ci/logs/uyum-a14-$i.txt)" >> ci/logs/a14-ozet.txt
  grep -q 'acilis=1' ci/logs/uyum-a14-$i.txt && break
done
true
