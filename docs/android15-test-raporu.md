# Android 15 (API 35) test raporu

Ortam: GitHub Actions, Android Emulator, `system-images;android-35;google_apis_playstore;x86_64`, Pixel 6 profili (1080×2400, 420 dpi), 4 GB bellek. **Gerçek telefonda denenmedi.**
Betik: `tools/android15.sh` (CI'da `cihaz-a15` işi). Her aşama PASS, FAIL veya NOT RUN olarak kaydedilir; biri bile PASS değilse iş başarısız olur.

## Neden 2.1.0'da yoktu

Bilinçli bir karar değildi. Matrise Android 8, 11, 13, 14 ve 16 eklenmiş, 15 yalnızca "denenmedi" diye not edilmişti. 2.1.1'de Android 15 ayrı ve zorunlu bir iş oldu.

## Sonuçlar

SONUC_TABLOSU

## Kapsam

| Alan | Nasıl denendi |
|---|---|
| Ana ekranlar | `UiFlowTest`: ana sayfa, profil, çalışma, plan, denemeler, yanlış defteri, istatistik, ayarlar ve toplam 70'ten fazla ekran, açık ve koyu temada açılır ve görüntüsü alınır. Veri, geçiş, yedek güvenliği ve öğrenme akışı testleri aynı aşamada |
| Kenardan kenara düzen | `UiFlowTest.systemBarsLayoutOnAllScreens`: her ekranda görünen metin ve dokunulabilir öğeler durum çubuğu, gezinme çubuğu ve kamera kesiği alanına girmez; yatayda ekran dışına taşmaz; dokunulabilir öğeler birbirinin üstüne binmez; durum ve gezinme çubuğu simge rengi temaya uyar |
| Gezinme biçimleri | Üç düğme (dikey ve yatay) ve hareketle gezinme (dikey, yazı %130, yatay). Geçişin gerçekten olduğu `navigation_mode` değeriyle doğrulanır |
| Ekran kesiği | Uzun kesik öykünmesi açık, yatay ekran (kesik içeriğin yanına gelir) |
| Yaşam döngüsü | Arka plana alıp geri dönme (aynı süreç, etkinlik önde), arka planda süreç öldürülüp yeniden açılma, bellek baskısı (`trim-memory RUNNING_CRITICAL`), çökme ve ANR sayısı. Deneme sırasında etkinliğin yeniden oluşturulması `UiFlowTest.examSessionSurvivesRecreate` içinde |
| Veri kaydetme | Yaz, zorla durdur, oku; cihazı yeniden başlat, oku |
| Bildirim ve hatırlatma | Bildirimin gösterilmesi, zamanlanmış hatırlatmanın gerçekten gelmesi (gerçek bekleme), saat dilimi değişince yeniden kurulması, bildirim izni kaldırılınca uygulamanın çalışmaya devam etmesi |
| Dosya seçici | Android'in kendi seçicisiyle PDF içe aktarma ve görüntüleme, fotoğrafı yanlış kaydına ekleme, yedeği dosyaya kaydedip aynı dosyadan geri yükleme |
| Reklam SDK'sı | Google test kimliğiyle yerel reklamın yüklenmesi ve reklamsız hak sonrası kaybolması (18 yaş üstü doğrulanmış kullanıcıyla) |
| Play Billing | BillingClient bağlantısının yanıt vermesi; doğrulanmış satın alma olmadan reklamsız hakkın açılmaması |
| Release ayarlı APK | Temiz kurulum, üç soğuk açılış, çökme ve ANR yok, süreç ayakta |

## Bulunan ve düzeltilen sorunlar

BULGULAR

## Sınırlar

- Emülatörde Google hesabı yoktur; Play Billing yalnızca bağlantı düzeyinde denendi. Ürün fiyatı, satın alma, iptal ve geri yükleme denenmedi.
- Reklam Google test kimliğiyle denendi; gerçek AdMob reklamı ve AEA rıza formu denenmedi.
- Gerçek telefon, üretici arayüzleri (Samsung, Xiaomi vb.) ve fiziksel çentik denenmedi.
