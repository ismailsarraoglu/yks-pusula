# Android 15 (API 35) test raporu

> **2.1.2 güncellemesi:** sürüm 2.1.2 (kod `c181571`, tur `42388476039`) Android 15'te yine 35/35 aşama PASS verdi; reklam testi yeni SDK (`play-services-ads`) ile geçti. Aşağıdaki ayrıntılar 2.1.1 turuna aittir.

Sürüm 2.1.1 (versionCode 4), kod `43b566f`, son tur `42363540765`, 7 Kasım 2026. **Sonuç: 35/35 aşama PASS.**

Ortam: GitHub Actions, Android Emulator, `system-images;android-35;google_apis_playstore;x86_64`, Pixel 6 profili (1080×2400, 420 dpi), 4 GB bellek. **Gerçek telefonda denenmedi.**
Betik: `tools/android15.sh` (CI'da `cihaz-a15` işi). Her aşama PASS, FAIL veya NOT RUN olarak kaydedilir; biri bile PASS değilse iş başarısız olur.

## Neden 2.1.0'da yoktu

Bilinçli bir karar değildi. Matrise Android 8, 11, 13, 14 ve 16 eklenmiş, 15 yalnızca "denenmedi" diye not edilmişti. 2.1.1'de Android 15 ayrı ve zorunlu bir iş oldu.

## Sonuçlar

| Aşama | Sonuç | Açıklama |
|---|---|---|
| acilis | PASS | android=15 api=35 imaj=system-images;android-35;google_apis_playstore;x86_64 |
| test-kurulum | PASS | Success / Success |
| ana-testler | PASS | OK (62 tests) |
| yasam-arka-plan | PASS | süreç 6534 -> 6534;     topResumedActivity=ActivityRecord{d8edf7b u0 app.ykspusula/.MainActivity t36} |
| yasam-surec-olumu | PASS | arka planda öldürüldü (kalan süreç: 'yok'), yeniden açıldı: 6674 |
| yasam-bellek-baskisi | PASS | trim-memory RUNNING_CRITICAL sonrası süreç: 6674 |
| yasam-cokme | PASS | çökme: 0, ANR: 0 |
| kalicilik-yaz | PASS | OK (1 test) |
| kalicilik-zorla-durdurma | PASS | OK (1 test) |
| kalicilik-yeniden-baslatma | PASS | OK (1 test) |
| gezinme-hareket | PASS | navigation_mode=2 (2 bekleniyor) |
| e2e-hareket-dikey | PASS | OK (1 test) |
| e2e-hareket-dikey-yazi130 | PASS | OK (1 test) |
| e2e-hareket-yatay | PASS | OK (1 test) |
| kesik-etkin | PASS |     * HideDisplayCutout:32:35 |
| e2e-kesik-yatay | PASS | OK (1 test) |
| gezinme-3dugme | PASS | navigation_mode=0 (0 bekleniyor) |
| e2e-3dugme-yatay | PASS | OK (1 test) |
| bildirim-goster | PASS | OK (1 test) |
| hatirlatma-zamaninda | PASS | OK (1 test) |
| saat-dilimi | PASS | OK (1 test) |
| bildirim-izni-kaldirildi | PASS | android.permission.POST_NOTIFICATIONS:granted=false,flags=[USER_SENSITIVE_WHEN_GRANTED/USER_SENSITIVE_WHEN_DENIED] |
| bildirim-izinsiz | PASS | OK (1 test) |
| pdf-secici | PASS | OK (1 test) |
| foto-secici | PASS | OK (1 test) |
| yedek-secici | PASS | OK (1 test) |
| osym-cevrimdisi | PASS | OK (1 test) |
| cevrimici-ag | PASS | doğrulanmış ağ: NetworkAgentInfo{network{100} |
| osym-tarayici | PASS | OK (1 test) |
| reklam | PASS | 1. denemede |
| billing-baslatma | PASS | OK (1 test) |
| release-kurulum | PASS | Success |
| release-cokme | PASS | uygulama çökmesi: 0 |
| release-anr | PASS | ANR: 0 |
| release-acik-kaldi | PASS | süreç 7404 |

Ölçümler (bu emülatörde, release ayarlı APK): soğuk açılış 1504, 1346, 1418 ms; bellek 31,7 MB PSS; çökme 0, ANR 0.

Düzen denetimi ayrıntısı (her ekranda sorun sayısı 0):

| Ayar | Pencere | Güvenli alan (sol, üst – sağ, alt) |
|---|---|---|
| Üç düğme, dikey | 1080×2400 | 0, 128 – 1080, 2274 |
| Hareketle gezinme, dikey (ve yazı %130) | 1080×2400 | 0, 128 – 1080, 2337 |
| Hareketle gezinme, yatay | 2400×1080 | 128, 63 – 2400, 1017 |
| Ekran kesiği (uzun), yatay | 2400×1080 | 128, 63 – 2400, 1017 |
| Üç düğme, yatay | 2400×1080 | 128, 63 – 2274, 1080 |

Durum ve gezinme çubuğu simgeleri koyu temada açık, açık temada koyu; her ayarda temaya uyuyor.

Sistemle ilgili ayrıntılar:
- Zamanlanmış hatırlatma gerçekten geldi (50 sn gecikmeyle; sistem alarmları toplu çalıştırır).
- Saat dilimi Etc/UTC'den Asia/Tokyo'ya değişince hatırlatma yeni saate kuruldu (20:00Z → 11:00Z, beklenen 11:00Z).
- PDF Android'in belge seçicisiyle (`com.google.android.documentsui`) içe aktarıldı ve 1/2 sayfası açıldı.
- ÖSYM kartı: Chrome öne geldi; tarayıcıya giden istek `https://www.osym.gov.tr/sinav-soru-ve-cevaplari` adresini taşıyor (sistem etkinlik kaydından okundu). Chrome yeni kurulu emülatörde ilk açılış ekranındaydı; ÖSYM sayfasının içeriği görüntülenip doğrulanmadı.
- Play Billing: kitaplık Play Store'a bağlanmayı denedi; emülatörde Google hesabı olmadığı için Play "In-app billing API version 3 is not supported on this device" yanıtı verdi ve kitaplık yeniden denemeyi bıraktı. Uygulama çökmedi, fiyat görünmedi (satın alma düğmesi kapalı), reklamsız hak açılmadı. **Gerçek satın alma bağlantısı kurulamadı ve denenmedi.**

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
| Reklam SDK'sı | Google test kimliğiyle yerel reklamın yüklenmesi ve reklamsız hak sonrası kaybolması (2.1.2'de bütün kullanıcılar için, çocuk korumalı yapılandırmayla) |
| Play Billing | Kitaplığın başlatılıp Play Store'a bağlanmayı denemesi; yanıt gelmezse satın alma düğmesinin kapalı kalması; doğrulanmış satın alma olmadan reklamsız hakkın açılmaması; çökme olmaması |
| ÖSYM bağlantısı | Uçak modunda karta basınca uyarı ve uygulamanın önde kalması; çevrimiçiyken tarayıcının öne gelmesi ve isteğin resmî arşiv adresini taşıması |
| Release ayarlı APK | Temiz kurulum, üç soğuk açılış, çökme ve ANR yok, süreç ayakta |

## Bulunan ve düzeltilen sorunlar

| Sorun | Nerede görüldü | Düzeltme |
|---|---|---|
| Yatay ekranda alt menü sol taraftaki kamera kesiği alanına giriyordu ("Ana Sayfa" sekmesi sol=0, güvenli alan 128 px) | Hareketle gezinme yatay, üç düğme yatay, kesik öykünmesi | Alt menü gezinme çubuğu ile yatay ekran kesiği boşluğunu birlikte uygular (`ui/AppRoot.kt`) |
| Sistem testleri (bildirim, hatırlatma, saat dilimi, seçiciler, Billing) hiç başlamıyordu | İlk Android 15 turları | Bir test yöntemi `Unit` döndürmüyordu; JUnit sınıfı reddediyordu. Düzeltildi |
| Çevrimiçi aşama hiç çalışmıyordu | İnternet denetimi | GitHub sunucuları ping geçirmiyor; Android'in doğrulanmış ağ bilgisine geçildi |
| Billing testi 30 ve 120 sn beklemede yanıt alamadı | Play imajı, Google hesabı yok | Uygulama hatası değil. Test gerçek bağlantı yerine güvenli başlatmayı doğrulayacak şekilde değiştirildi; sınır raporda açıkça yazılı |
| Belge seçicide bayatlayan öğe (bir kez) | Yedek seçici | Arama yeniden denenir |
| Deneme ekranının yeniden oluşturulma testi bir kez başarısız oldu | Bir ara turda, sonraki turlarda geçti | Nedeni logda görünmedi; artık hata nedeni iş loguna da yazılıyor. Ara sıra görülen olay olarak izleniyor |

## Sınırlar

- Emülatörde Google hesabı yoktur; Play Billing yalnızca başlatma düzeyinde denendi, bağlantı kurulamadı. Ürün fiyatı, satın alma, iptal ve geri yükleme denenmedi.
- ÖSYM sayfasının içeriği (yıl seçimi, TYT/AYT/YDT kitapçıkları ve cevap anahtarları) görüntülenip denetlenmedi; ÖSYM sitesi otomatik okumaya da izin vermedi.
- Reklam Google test kimliğiyle denendi; gerçek AdMob reklamı ve AEA rıza formu denenmedi.
- Gerçek telefon, üretici arayüzleri (Samsung, Xiaomi vb.) ve fiziksel çentik denenmedi.
