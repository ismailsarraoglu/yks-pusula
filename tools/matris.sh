#!/bin/bash
# Sürüm matrisini tek makinede sırayla çalıştırır (CI'da her sürüm ayrı işte, paralel çalışır).
# Kullanım: matris.sh [a8 a11 ...]   Varsayılan: bütün sürümler.
# Her sürümün sonucu ci/logs/matris.tsv dosyasına yazılır. Süre aşımı, açılmama veya tek bir başarısız aşama
# o sürümü FAIL yapar; çalıştırılmayan sürüm NOT_RUN olarak görünür. Biri bile PASS değilse çıkış kodu 1.
mkdir -p ci/logs
M=ci/logs/matris.tsv; : > $M
LISTE=${*:-a8 a11 a13 a14 a15 a16 a14p a16p}
for a in $LISTE; do printf '%s\tNOT_RUN\tsırası gelmedi\n' "$a" >> $M; done
for a in $LISTE; do
  case $a in a11) dk=110 ;; a15) dk=170 ;; a14p|a16p) dk=90 ;; *) dk=60 ;; esac
  timeout -k 30 ${dk}m bash tools/cihaz.sh "$a"; k=$?
  case $k in
    0) s=PASS; d="bütün aşamalar geçti" ;;
    3) s=FAIL; d="emülatör açılmadı" ;;
    124|137) s=FAIL; d="$dk dakika süre sınırı aşıldı"; printf 'zaman-siniri\tFAIL\t%s\n' "$d" >> ci/logs/durum-$a.tsv ;;
    *) s=FAIL; d="çıkış kodu $k: $(awk -F'\t' '$2!="PASS"{printf "%s=%s ", $1, $2}' ci/logs/durum-$a.tsv 2>/dev/null | cut -c1-200)" ;;
  esac
  awk -F'\t' -v OFS='\t' -v a="$a" -v s="$s" -v d="$d" '$1==a{$2=s;$3=d}1' $M > $M.yeni && mv $M.yeni $M
done
echo "--- matris ---"; cat $M
! grep -qv $'\tPASS\t' $M
