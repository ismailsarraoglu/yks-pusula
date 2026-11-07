#!/bin/bash
# Android 11 tam turu: bütün cihaz testleri, düğme ve stres tekrarları, beş ekran yapılandırması,
# kalıcılık ve release açılışı. Her aşama PASS/FAIL/NOT_RUN olarak kaydedilir; biri bile geçmezse çıkış kodu 1.
source tools/kapi.sh a11
mkdir -p ci/screens
if ! emulator_ac a11 "system-images;android-30;default;x86_64" 3072; then
  kayit acilis FAIL "emülatör açılmadı"; emulator_kapat; kapi_bitir; exit 3
fi
kayit acilis PASS "$(tail -1 ci/logs/kurulum-a11.txt)"
cevrimdisi
echo "ucak-modu: $(adb shell settings get global airplane_mode_on | tr -d '\r')" >> ci/logs/olcum-a11.txt

# Bütün cihaz testleri (çevrimiçi, stres ve sistem testleri ayrı aşamalarda). Derleme işinin ürettiği APK'lar kurulur.
if ! apk_kur; then
  calismadi "test APK kurulamadı" cihaz-testleri akislar dugmeler-1 dugmeler-2 dugmeler-3 dugmeler-4 dugmeler-5 stres-1 stres-2 stres-3 \
    ekran-standart ekran-buyuk-yazi duzen-buyuk-yazi ekran-yatay duzen-yatay ekran-kucuk ekran-tablet kalicilik-yaz kalicilik-zorla-durdurma kalicilik-yeniden-baslatma
  release_dene; emulator_kapat; kapi_bitir; exit $?
fi
HARIC=$PKG.AdOnlineTest,$PKG.StresTest,$PKG.SystemIntegrationTest,$PKG.NotificationDeniedTest,$PKG.PersistenceTest
TEST_SURESI=3600 testi_calistir cihaz-testleri ci/logs/connected.txt "" -e package $PKG -e notClass $HARIC
I=ci/logs/a11
mkdir -p $I
testi_calistir akislar $I/akislar.txt "$PKG.UiFlowTest#onboardingAndTabs,$PKG.UiFlowTest#studyLogUpdatesStats,$PKG.UiFlowTest#examSessionSurvivesRecreate"
for n in 1 2 3 4 5; do
  testi_calistir "dugmeler-$n" $I/dugmeler-$n.txt "$PKG.UiFlowTest#mainButtonsDoTheirJob"
  adb exec-out run-as $PKG cat files/hata.png > ci/screens/hata-$n.png 2>/dev/null; adb shell run-as $PKG rm -f files/hata.png
done
for n in 1 2 3; do testi_calistir "stres-$n" $I/stres-$n.txt $PKG.StresTest; done

ekranlar() { adb exec-out run-as $PKG sh -c 'cd files && tar -cf - screens e2e' 2>/dev/null | tar -x -C "$1" 2>/dev/null; }
mkdir -p ci/screens-buyuk ci/a11-e2e
testi_calistir ekran-standart $I/standart.txt "$PKG.UiFlowTest#allScreensOpenBothThemes"
ekranlar ci/
adb shell settings put system font_scale 1.5
testi_calistir ekran-buyuk-yazi $I/buyuk.txt "$PKG.UiFlowTest#allScreensOpenBothThemes"
testi_calistir duzen-buyuk-yazi $I/duzen-buyuk.txt "$PKG.UiFlowTest#systemBarsLayoutOnAllScreens" -e etiket a11-yazi150
adb exec-out run-as $PKG sh -c 'cd files && tar -cf - screens' 2>/dev/null | tar -x -C /tmp/ 2>/dev/null
cp /tmp/screens/*-s-dark-0[0-9]-* ci/screens-buyuk/ 2>/dev/null
adb shell settings put system font_scale 1.0
adb shell settings put system accelerometer_rotation 0; adb shell settings put system user_rotation 1
testi_calistir ekran-yatay $I/yatay.txt "$PKG.UiFlowTest#allScreensOpenBothThemes"
testi_calistir duzen-yatay $I/duzen-yatay.txt "$PKG.UiFlowTest#systemBarsLayoutOnAllScreens" -e etiket a11-yatay
adb shell settings put system user_rotation 0
adb shell wm size 480x800; adb shell wm density 240
testi_calistir ekran-kucuk $I/kucuk.txt "$PKG.UiFlowTest#allScreensOpenBothThemes"
adb shell wm size 1600x2560; adb shell wm density 280
testi_calistir ekran-tablet $I/tablet.txt "$PKG.UiFlowTest#allScreensOpenBothThemes"
adb shell wm size reset; adb shell wm density reset
ekranlar ci/
mv ci/e2e ci/a11-e2e 2>/dev/null
echo "ekran-toplam=$(ls ci/screens | wc -l)" >> ci/logs/olcum-a11.txt

kalicilik
adb shell dumpsys alarm | grep -c ykspusula | sed 's/^/alarm-kaydi: /' >> ci/logs/olcum-a11.txt
release_dene
adb logcat -d > ci/logs/logcat-a11.txt 2>/dev/null
emulator_kapat
kapi_bitir
