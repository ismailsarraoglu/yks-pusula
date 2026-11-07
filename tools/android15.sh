#!/bin/bash
# Android 15 (API 35) ayrı test turu. Play Store'lu imaj: reklam SDK'sı ve Play Billing de denenebilsin.
# Her aşama ci/logs/durum-a15.tsv dosyasına PASS/FAIL/NOT_RUN olarak yazılır; biri bile PASS değilse çıkış kodu 1.
# Ayrıntılar: ci/logs/a15/, ekranlar: ci/screens-a15/
source tools/kapi.sh a15
IMG="system-images;android-35;google_apis_playstore;x86_64"
D=ci/logs/a15
O=ci/logs/olcum-a15.txt
mkdir -p "$D" ci/screens ci/screens-a15

PLAN="test-kurulum ana-testler yasam-arka-plan yasam-surec-olumu yasam-bellek-baskisi yasam-cokme kalicilik-yaz kalicilik-zorla-durdurma kalicilik-yeniden-baslatma
gezinme-hareket e2e-hareket-dikey e2e-hareket-dikey-yazi130 e2e-hareket-yatay kesik-etkin e2e-kesik-yatay gezinme-3dugme e2e-3dugme-yatay
bildirim-goster hatirlatma-zamaninda saat-dilimi bildirim-izni-kaldirildi bildirim-izinsiz pdf-secici foto-secici yedek-secici
cevrimici-ag reklam billing release-kurulum release-cokme release-anr release-acik-kaldi"

if ! emulator_ac a15 "$IMG" 4096; then
  kayit acilis FAIL "emülatör açılmadı ($IMG)"
  calismadi "emülatör açılmadı" $PLAN
  emulator_kapat; kapi_bitir; exit 3
fi
kayit acilis PASS "$(tail -1 ci/logs/kurulum-a15.txt)"
echo "cihaz=$(adb shell getprop ro.product.model | tr -d '\r') $(adb shell wm density | tr -d '\r' | tr '\n' ' ') $(adb shell wm size | tr -d '\r' | tr '\n' ' ')" >> $O
# Yeni açılan Play imajı uzun süre güncelleme yapar; sistem sakinleşsin.
sleep 90
cevrimdisi
echo "ucak-modu: $(adb shell settings get global airplane_mode_on | tr -d '\r')" >> $O

# Gezinme biçimi: 0 = üç düğme, 2 = hareket. Değişiklik gerçekten olmadıysa aşama FAIL olur.
gezinme() {
  local hedef=$1 paket
  [ "$hedef" = 2 ] && paket=com.android.internal.systemui.navbar.gestural || paket=com.android.internal.systemui.navbar.threebutton
  adb shell cmd overlay enable-exclusive --category $paket; sleep 8
  local m; m=$(adb shell settings get secure navigation_mode | tr -d '\r')
  [ "$m" = "$hedef" ]
}
E=$PKG.UiFlowTest#systemBarsLayoutOnAllScreens
S=$PKG.SystemIntegrationTest

if ! apk_kur; then
  calismadi "test APK kurulamadı" $(echo $PLAN | tr ' ' '\n' | grep -vE '^(test-kurulum|release-.*)$')
else
  # 1. Ana ekranlar (ana sayfa, profil, çalışma, plan, denemeler, yanlış defteri, istatistik, ayarlar ve diğerleri),
  #    veri kaydetme, geçiş, yedek güvenliği, düzen denetimi: üç düğmeli gezinme, dikey.
  gezinme 0 || echo "uyarı: 3 düğme moduna geçilemedi" >> $O
  echo "ana-testler gezinme-modu: $(adb shell settings get secure navigation_mode | tr -d '\r')" >> $O
  T=$PKG.DataFlowTest,$PKG.MigrationTest,$PKG.AdTouchGuardTest,$PKG.LearningFlowTest,$PKG.BackupSecurityTest,$PKG.UiFlowTest
  [ "$KAPI_DENEMESI" = "cihaz" ] && T="$T,$PKG.GateProbeTest"
  testi_calistir ana-testler $D/ana-testler.txt "$T" -e etiket 3dugme-dikey

  # 2. Yaşam döngüsü: arka plana alma ve geri dönme, arka planda süreç öldürülmesi, bellek baskısı.
  adb logcat -c
  adb shell am force-stop $PKG; sleep 2
  adb shell am start -W -n $PKG/.MainActivity > $D/yasam.txt 2>&1; sleep 5
  p1=$(adb shell pidof $PKG | tr -d '\r')
  adb shell input keyevent KEYCODE_HOME; sleep 5
  adb shell am start -W -n $PKG/.MainActivity >> $D/yasam.txt 2>&1; sleep 4
  p2=$(adb shell pidof $PKG | tr -d '\r')
  ust=$(adb shell dumpsys activity activities | grep -m1 -E 'topResumedActivity|mResumedActivity' | tr -d '\r')
  ayni_surec_onde() { [ -n "$p1" ] && [ "$p1" = "$p2" ] && echo "$ust" | grep -q "$PKG"; }
  kontrol yasam-arka-plan "süreç $p1 -> $p2; $ust" ayni_surec_onde
  adb shell input keyevent KEYCODE_HOME; sleep 4
  adb shell am kill $PKG; sleep 3
  p3=$(adb shell pidof $PKG | tr -d '\r')
  adb shell am start -W -n $PKG/.MainActivity >> $D/yasam.txt 2>&1; sleep 6
  p4=$(adb shell pidof $PKG | tr -d '\r')
  adb exec-out screencap -p > ci/screens-a15/yasam-yeniden-acilis.png 2>/dev/null
  kontrol yasam-surec-olumu "arka planda öldürüldü (kalan süreç: '${p3:-yok}'), yeniden açıldı: ${p4:-yok}" [ -z "$p3" -a -n "$p4" -a "$p4" != "$p1" ]
  t=$(adb shell am send-trim-memory $PKG RUNNING_CRITICAL 2>&1 | tr -d '\r'); sleep 3
  p5=$(adb shell pidof $PKG | tr -d '\r')
  kontrol yasam-bellek-baskisi "trim-memory RUNNING_CRITICAL ${t:+($t) }sonrası süreç: ${p5:-yok}" [ -n "$p5" -a "$p5" = "$p4" ]
  adb logcat -d > $D/yasam-logcat.txt 2>/dev/null
  c=$(grep -A3 'FATAL EXCEPTION' $D/yasam-logcat.txt | grep -c "Process: $PKG"); a=$(grep -c "ANR in $PKG" $D/yasam-logcat.txt)
  kontrol yasam-cokme "çökme: $c, ANR: $a" [ "$c" = 0 -a "$a" = 0 ]

  # 3. Veri kalıcılığı: yaz, zorla durdur, oku, cihazı yeniden başlat, oku.
  kalicilik

  # 4. Kenardan kenara düzen: hareketle gezinme (dikey, büyük yazı, yatay), ekran kesiği, üç düğme (yatay).
  gezinme 2; r=$?
  if kontrol gezinme-hareket "navigation_mode=$(adb shell settings get secure navigation_mode | tr -d '\r') (2 bekleniyor)" [ $r = 0 ]; then
    testi_calistir e2e-hareket-dikey $D/e2e-hareket-dikey.txt "$E" -e etiket hareket-dikey
    adb shell settings put system font_scale 1.3; sleep 3
    testi_calistir e2e-hareket-dikey-yazi130 $D/e2e-hareket-dikey-yazi130.txt "$E" -e etiket hareket-dikey-yazi130
    adb shell settings put system font_scale 1.0
    adb shell settings put system accelerometer_rotation 0; adb shell settings put system user_rotation 1; sleep 4
    testi_calistir e2e-hareket-yatay $D/e2e-hareket-yatay.txt "$E" -e etiket hareket-yatay
    adb shell settings put system user_rotation 0; sleep 3
  else
    calismadi "hareketle gezinmeye geçilemedi" e2e-hareket-dikey e2e-hareket-dikey-yazi130 e2e-hareket-yatay
  fi
  # Uzun ekran kesiği öykünmesi; yatayda kesik içeriğin yanına gelir, en zor durum budur.
  adb shell cmd overlay enable-exclusive --category com.android.internal.display.cutout.emulation.tall; sleep 6
  adb shell settings put system accelerometer_rotation 0; adb shell settings put system user_rotation 1; sleep 4
  adb shell dumpsys window displays 2>/dev/null | grep -m2 -iE 'cutout' | tr -d '\r' | cut -c1-250 > $D/kesik.txt
  if kontrol kesik-etkin "$(head -1 $D/kesik.txt | cut -c1-200)" sh -c "adb shell cmd overlay list | tr -d '\r' | grep -q '\[x\] com.android.internal.display.cutout.emulation.tall'"; then
    testi_calistir e2e-kesik-yatay $D/e2e-kesik-yatay.txt "$E" -e etiket kesik-yatay
  else
    calismadi "kesik öykünmesi açılamadı" e2e-kesik-yatay
  fi
  adb shell cmd overlay disable com.android.internal.display.cutout.emulation.tall; sleep 4
  gezinme 0; r=$?
  if kontrol gezinme-3dugme "navigation_mode=$(adb shell settings get secure navigation_mode | tr -d '\r') (0 bekleniyor)" [ $r = 0 ]; then
    testi_calistir e2e-3dugme-yatay $D/e2e-3dugme-yatay.txt "$E" -e etiket 3dugme-yatay
  else
    calismadi "üç düğmeye geçilemedi" e2e-3dugme-yatay
  fi
  adb shell settings put system user_rotation 0; sleep 4

  # 5. Bildirimler ve zamanlanmış hatırlatmalar (izin verilmiş, sonra kaldırılmış).
  testi_calistir bildirim-goster $D/bildirim-goster.txt "$S#notificationShownWhenAllowed"
  testi_calistir hatirlatma-zamaninda $D/hatirlatma-zamaninda.txt "$S#scheduledReminderActuallyFires"
  testi_calistir saat-dilimi $D/saat-dilimi.txt "$S#timeZoneChangeReschedules"
  adb shell dumpsys alarm 2>/dev/null | grep -B2 -A6 "$PKG" | head -60 > $D/alarm-kaydi.txt
  adb shell pm revoke $PKG android.permission.POST_NOTIFICATIONS
  izin=$(adb shell dumpsys package $PKG | grep -m1 'android.permission.POST_NOTIFICATIONS: granted' | tr -d '\r ')
  if kontrol bildirim-izni-kaldirildi "${izin:-bilgi yok}" sh -c "echo '$izin' | grep -q 'granted=false'"; then
    testi_calistir bildirim-izinsiz $D/bildirim-izinsiz.txt "$PKG.NotificationDeniedTest"
  else
    calismadi "izin kaldırılamadı" bildirim-izinsiz
  fi
  adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS

  # 6. Sistem dosya seçicisi: PDF içe aktarma ve görüntüleme, fotoğraf ekleme, yedeği dosyaya yazma ve geri yükleme.
  testi_calistir pdf-secici $D/pdf-secici.txt "$S#pdfPickerImportsAndRenders"
  testi_calistir foto-secici $D/foto-secici.txt "$S#photoPickerAttachesToWrong"
  testi_calistir yedek-secici $D/yedek-secici.txt "$S#backupSaveAndRestoreThroughPicker"
  adb exec-out run-as $PKG sh -c 'cd files && tar -cf - screens e2e sistem 2>/dev/null' 2>/dev/null | tar -x -C ci/screens-a15 2>/dev/null
  mv ci/screens-a15/e2e $D/e2e 2>/dev/null; mv ci/screens-a15/sistem $D/sistem 2>/dev/null

  # 7. Çevrimiçi: reklam SDK'sı (Google test kimlikleri) ve Play Billing bağlantısı.
  cevrimici
  # GitHub sunucuları ICMP (ping) geçirmez; bağlantı Android'in doğrulanmış ağ bilgisinden okunur.
  ag_dogrulandi() { local n; for n in $(seq 1 12); do adb shell dumpsys connectivity 2>/dev/null | grep -qE '&VALIDATED' && return 0; sleep 5; done; return 1; }
  if kontrol cevrimici-ag "doğrulanmış ağ: $(adb shell dumpsys connectivity 2>/dev/null | grep -m1 -oE 'NetworkAgentInfo\{[^ ]* [^ ]*' | tr -d '\r')" ag_dogrulandi; then
    ANA=$DURUM; DURUM=$D/reklam-denemeleri.tsv; : > "$DURUM"
    sonuc=FAIL; aciklama=""
    for d in 1 2 3; do
      if testi_calistir "reklam-deneme-$d" $D/reklam-$d.txt "$PKG.AdOnlineTest#testAdLoadsAndPurchaseRemovesIt"; then sonuc=PASS; aciklama="$d. denemede"; break; fi
      adb logcat -d 2>/dev/null | grep -E 'YksAds|Ads|UserMessagingPlatform' | tail -60 | cut -c1-220 > $D/reklam-$d-logcat.txt
      if ! grep -qE 'NO_FILL|No fill|code[=: ]3\b' $D/reklam-$d.txt; then aciklama="başarısız (NO_FILL değil): $(tail -1 "$DURUM" | cut -f3)"; break; fi
      aciklama="3 denemede de reklam sunucusu reklam vermedi (NO_FILL)"
      sleep 30
    done
    DURUM=$ANA
    kayit reklam "$sonuc" "$aciklama"
    adb exec-out run-as $PKG cat files/reklam-yerel.png > ci/screens-a15/reklam-yerel.png 2>/dev/null
    testi_calistir billing $D/billing.txt "$S#billingClientStarts"
    adb exec-out run-as $PKG cat files/sistem/billingClientStarts.txt > $D/sistem-billing.txt 2>/dev/null
    adb logcat -d 2>/dev/null | grep -iE 'BillingClient|BillingLogger' | tail -40 | cut -c1-220 > $D/billing-logcat.txt
  else
    calismadi "emülatör internete çıkamadı" reklam billing
  fi
  cevrimdisi
fi

# 8. Yan yükleme (release ayarlı) sürüm: temiz kurulum, soğuk açılış, çökme, ANR.
release_dene
adb logcat -d > $D/logcat-son.txt 2>/dev/null
emulator_kapat
kapi_bitir
