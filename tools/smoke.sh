#!/bin/bash
# Kullanım: smoke.sh <ad> "<sistem imajı>" "<test sınıfları>"
# Bir Android sürümünde temiz kurulum, testler ve release açılışı. Her aşama PASS/FAIL/NOT_RUN olarak kaydedilir.
# Çıkış kodu: 0 hepsi geçti, 1 bir aşama başarısız veya çalışmadı, 3 emülatör açılmadı.
NAME=$1; IMG=$2; CLS=$3
source tools/kapi.sh "$NAME"
mkdir -p ci/screens
if ! emulator_ac "$NAME" "$IMG" "${BELLEK:-3072}"; then
  kayit acilis FAIL "emülatör açılmadı ($IMG)"
  emulator_kapat
  exit 3
fi
kayit acilis PASS "$(tail -1 ci/logs/kurulum-$NAME.txt)"
[ -n "$HAREKET" ] && { adb shell cmd overlay enable com.android.internal.systemui.navbar.gestural; sleep 3; }
cevrimdisi
echo "ucak-modu: $(adb shell settings get global airplane_mode_on | tr -d '\r')" >> ci/logs/olcum-$NAME.txt

# Reklam sunucusunun reklam vermemesi (NO_FILL) uygulama hatası değildir; yalnızca bu durumda yeniden denenir.
# Denemeler ayrı dosyaya yazılır, kapıya tek satır girer. reklam_testi <aşama> <AdOnlineTest yöntemi>
reklam_testi() {
  local asama=$1 yontem=$2 ANA=$DURUM sonuc=FAIL aciklama="" d
  DURUM=ci/logs/$asama-denemeleri-$NAME.tsv; : > "$DURUM"
  for d in 1 2 3; do
    if testi_calistir "$asama-deneme-$d" "ci/logs/$asama-$NAME-$d.txt" "$PKG.AdOnlineTest#$yontem"; then sonuc=PASS; aciklama="$d. denemede"; break; fi
    if ! grep -qE 'NO_FILL|No fill|code[=: ]3\b' "ci/logs/$asama-$NAME-$d.txt"; then aciklama="başarısız (NO_FILL değil): $(tail -1 "$DURUM" | cut -f3)"; break; fi
    aciklama="3 denemede de reklam sunucusu reklam vermedi (NO_FILL)"
    sleep 30
  done
  DURUM=$ANA
  kayit "$asama" "$sonuc" "$aciklama"
}

if [ -n "$CLS" ]; then
  [ "$KAPI_DENEMESI" = "cihaz" ] && CLS="$CLS,$PKG.GateProbeTest"
  if apk_kur; then
    testi_calistir testler "ci/logs/uyum-$NAME-test.txt" "$CLS"
    [ -n "$KALICILIK" ] && kalicilik
    if [ -n "$CEVRIMICI" ]; then
      cevrimici
      reklam_testi reklam testAdLoadsAndPurchaseRemovesIt
      # Parmak reklamın üstündeyken hızlı kaydırma reklamı açmamalı (yanlışlıkla tıklama koruması).
      [ -n "$REKLAM_KAYDIRMA" ] && reklam_testi reklam-kaydirma dragOverAdDoesNotOpenIt
      adb exec-out run-as $PKG cat files/reklam-yerel.png > "ci/screens/reklam-$NAME.png" 2>/dev/null
      cevrimdisi
    fi
    mkdir -p "ci/screens-$NAME"
    adb exec-out run-as $PKG sh -c 'cd files && tar -cf - screens e2e sistem 2>/dev/null' 2>/dev/null | tar -x -C "ci/screens-$NAME" 2>/dev/null
  else
    calismadi "test APK kurulamadı" testler
  fi
fi
release_dene
if [ -n "$TEKRAR" ]; then
  ok=0; notlar=""
  for n in $(seq 1 "$TEKRAR"); do
    for deneme in 1 2; do
      adb uninstall $PKG > /dev/null 2>&1
      adb install -r app/build/outputs/apk/sideload/app-sideload.apk > /dev/null 2>&1
      adb logcat -c
      adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
      sleep 15
      if [ -n "$(adb shell pidof $PKG | tr -d '\r')" ]; then ok=$((ok+1)); break; fi
      adb logcat -d 2>/dev/null > "ci/logs/release-kayip-$NAME-$n-$deneme.txt"
      neden=$(altyapi_nedeni "ci/logs/release-kayip-$NAME-$n-$deneme.txt")
      [ -z "$neden" ] && { notlar="$notlar $n: neden bulunamadı;"; break; }
      notlar="$notlar $n/$deneme: $neden;"
    done
  done
  kontrol "release-tekrar" "$ok/$TEKRAR temiz kurulumda süreç ayakta.$notlar" [ "$ok" = "$TEKRAR" ]
fi
emulator_kapat
kapi_bitir
