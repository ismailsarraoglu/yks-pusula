#!/bin/bash
# Kullanım: tools/ci-sonuc.sh <sonuç-dalı> [kısa-hash] [deneme-sayısı]
# Örnek: tools/ci-sonuc.sh ci-results-duzeltme-2.1.1 abc1234
# Sonuç dalında verilen commit'in kaydı görünene kadar bekler, sonra kalite kapısı özetini yazar.
# Çıkış kodu: 0 kapı PASS, 1 kapı FAIL, 2 zaman aşımı.
D=$1; H=$2
for i in $(seq 1 "${3:-1}"); do
  if git fetch -q --depth 1 origin "$D" 2>/dev/null && { [ -z "$H" ] || git show FETCH_HEAD:logs/commit.txt 2>/dev/null | grep -q "^$H"; }; then
    git show FETCH_HEAD:logs/commit.txt
    git show FETCH_HEAD:logs/DURUM.md | head -40
    exit "$(git show FETCH_HEAD:logs/kapi-sonucu.txt 2>/dev/null || echo 1)"
  fi
  [ "$i" -lt "${3:-1}" ] && sleep 30
done
echo "sonuç henüz yok"; exit 2
