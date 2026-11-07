# Test raporu (2.1.1)

Sürüm 2.1.1 (versionCode 4). Test edilen kod: `43b566f`, son tam tur `42363540765`, 7 Kasım 2026. Dal: `duzeltme-2.1.1`.
Testler GitHub Actions üzerindeki Android emülatörlerinde çalıştı. **Gerçek telefonda ve gerçek Google Play hesabıyla hiçbir test yapılmadı.** Önceki raporlar: `docs/test-raporu-2.1.0.md`, `docs/test-raporu-2.0.md`.

Her aşama PASS, FAIL veya NOT RUN olarak kaydedildi. Ham kayıtlar: GitHub'daki `ci-results-duzeltme-2.1.1` dalında `logs/DURUM.md` ve `logs/durum-*.tsv`.

## Son tam tur: kalite kapısı PASS

| İş | Sonuç | Kapsam |
|---|---|---|
| Derleme | PASS (7/7 aşama) | Birim testleri, debug + sideload + test APK'sı, Play derlemesinin anahtarsız durması, manifest (hata ayıklanamaz, hedef SDK 36), APK imzası |
| Android 8 (API 26) | PASS (7/7) | 53 cihaz testi (veri, geçiş, reklam dokunma koruması, öğrenme akışı, yedek güvenliği), release ayarlı APK |
| Android 11 (API 30) | PASS (26/26) | Bütün cihaz testleri (63), 3 akış testi, ana düğmeler 5 kez, geçiş stresi 3 tur, 5 ekran yapılandırması (standart, yazı %150, yatay, küçük ekran, tablet; toplam 691 ekran görüntüsü), büyük yazı ve yatayda düzen denetimi, kalıcılık (zorla durdurma, yeniden başlatma), release APK |
| Android 13 (API 33) | PASS (7/7) | 53 cihaz testi, release APK |
| Android 14 (API 34) | PASS (10/10) | 62 cihaz testi (arayüz testleri dahil, hareketle gezinme), kalıcılık, release APK |
| **Android 15 (API 35)** | **PASS (35/35)** | Ayrı rapor: `docs/android15-test-raporu.md` |
| Android 16 (API 36) | PASS (10/10) | 62 cihaz testi (hareketle gezinme), kalıcılık, release APK |
| Android 14 Play imajı | PASS (10/10) | Room geçişi, çevrimiçi test reklamı, reklamın üstünden başlayan kaydırma reklamı açmıyor, art arda 6 temiz kurulum |
| Android 16 Play imajı | PASS (10/10) | Aynısı |

Birim testleri: 76 test, 75 çalıştı ve geçti, 1 atlandı (kalite kapısı denemesi testi; yalnızca deneme modunda çalışır). Gradle önbelleği kullanılmadan (`--rerun`) yeniden çalıştırıldı.

Ölçümler (release ayarlı APK, emülatör): soğuk açılış Android 11'de 720, 707, 662 ms; Android 15'te 1504, 1346, 1418 ms. Bellek 31,7-36,2 MB PSS. Hiçbir sürümde uygulama çökmesi veya ANR yok.

Android 9, 10 ve 12 denenmedi.

## Kalite kapısının gerçekten durdurduğunun kanıtı

Kasıtlı hata denemeleri (`workflow_dispatch`, `kapi_denemesi`). Normal çalıştırmada bu denemeler hiçbir şey yapmaz.

| Deneme | Çalıştırma | Beklenen | Sonuç |
|---|---|---|---|
| `birim`: bir birim testi kasıtlı başarısız | `42348249351` | Derleme durur, cihaz testleri çalışmaz, CI başarısız | birim-testleri FAIL (76 testten 1); derleme ve sonraki 5 aşama NOT RUN; Android 8 işi NOT RUN; kapı FAIL; GitHub sonucu: failure |
| `cihaz`: bir cihaz testi kasıtlı başarısız | `42348617485` | Derleme geçer, cihaz işi ve CI başarısız | Derleme PASS; Android 8'de 54 testten yalnızca kasıtlı test FAIL; kapı FAIL; GitHub sonucu: failure |
| `emulator`: emülatör oluşturulamaz (var olmayan imaj) | `42349762196` | `smoke.sh` 3 ile biter, CI başarısız | `acilis` FAIL, çıkış kodu 3; kapı FAIL; GitHub sonucu: failure |

Ayrıca gerçek hatalar da kapıyı durdurdu: geliştirme sırasında iki birim testi hatası ve bir derleme hatası CI'yı kırmızıya düşürdü, sonraki aşamalar NOT RUN göründü. Bu çalıştırmalar `docs/degisiklik-raporu-2.1.1.md` dönemine aittir; kayıt kimlikleri geçmiş temizliğinden önceki kimliklerdir.

## Yedekleme ve geri yükleme testleri (yeniden çalıştırıldı)

`BackupSecurityTest`, 21 test, son turda Android 8, 11, 13, 14, 15 ve 16'da geçti.

| Senaryo | Test | Sonuç |
|---|---|---|
| Birleştirmede aynı adlı ek dosya güvenle değişir | `mergeReplacesSameNameFileSafely` | PASS |
| Dosya taşıma hatası: eski dosya ve veriler korunur | `fileMoveFailureKeepsOldFileAndData` | PASS |
| Veritabanı adımında hata: tam geri dönüş | `databaseStepFailureRollsBack` | PASS |
| DataStore (ayarlar) yazma hatası: veriler, dosyalar ve ayarlar geri döner | `settingsWriteFailureRollsBackEverything` | PASS |
| Ayarlardan sonra hata: ayarlar da geri döner | `failureAfterSettingsRestoresSettings` | PASS |
| Yarım kalan geri yükleme sonraki açılışta kurtarılır | `interruptedRestoreRecoveredOnNextStart` | PASS (süreç gerçekten öldürülmedi; işlem ortasında durma benzetildi) |
| Başarılı tam değiştirme her şeyi uygular | `successfulReplaceAppliesEverything` | PASS |
| Önceki güvenlik testleri (yol aşımı, ZIP bombası, boyut ve sayı sınırları, bozuk ek ve JSON, eksik fotoğraf, güvenlik kopyası, geçici dosya kalmaması) | 14 test | PASS |
| Yedeği Android dosya seçicisiyle kaydedip geri yükleme | `SystemIntegrationTest` (Android 15) | PASS |

## Satın alma doğrulaması testleri (yeniden çalıştırıldı)

| Senaryo | Test | Sonuç |
|---|---|---|
| Paket adı eksik kayıt reddedilir (2.1.0'da OK kabul ediliyordu) | `purchaseChecksProductStateAndPackage` | PASS |
| Eksik veya yanlış türde durum, eksik veya boş belirteç, fazla ürün, büyük harf veya boşluk farkı, yanlış paket, PENDING, belirteç uyuşmazlığı, `[]`, `{}` | `missingOrInconsistentFieldsRejected` | PASS |
| Saklanan kaydın belirteç özeti JSON'la eşleşmeli | `cachedTokenMustMatchJson` | PASS |
| İmza, açık anahtar eksikliği, çevrimdışı hak korunması | 4 birim testi | PASS |
| Cihazda: belirteç uyuşmazlığında hak açılmaz | `DataFlowTest` | PASS |
| Play Billing kitaplığının başlatılması (Android 15 Play imajı) | `billingClientStarts` | PASS, sınırıyla: Google hesabı olmadığı için Play satın almayı desteklemediğini bildirdi; uygulama çökmedi, satın alma düğmesi kapalı kaldı, hak açılmadı. **Gerçek bağlantı kurulamadı** |

## Bu sürümde eklenen diğer testler

| Konu | Test | Sonuç |
|---|---|---|
| Tarafsız yaş ekranı: alanlar boş, 16 yaş ve "Belirtmek istemiyorum" reklam SDK'sını başlatmaz, 30 yaş doğrulanır | `UiFlowTest.neutralAgeScreenGatesAds` | PASS (Android 11, 14, 15, 16) |
| Yaş hesabı ve giriş denetimi | `AgeGateTest` (3 birim testi) | PASS |
| Profil: YDT dili yalnızca Dil alanında görünür, gizlemek seçimi silmez | `UiFlowTest.profileEditShowsYdtLanguageOnlyForDil` | PASS (Android 11, 14, 15, 16) |
| ÖSYM kartı: internet yokken uyarı | `SystemIntegrationTest.osymLinkOfflineShowsWarning` | PASS (Android 15) |
| ÖSYM kartı: tarayıcı resmî arşiv adresiyle açılır | `SystemIntegrationTest.osymLinkOpensBrowser` | PASS (Android 15, Chrome). Sayfanın içeriği görüntülenip denetlenmedi |

## Ara sıra görülen olaylar

| Olay | Sıklık | Durum |
|---|---|---|
| Android 15'te deneme ekranının yeniden oluşturulma testi başarısız oldu | Yeni betikle yapılan 6 Android 15 turunun 1'inde | Nedeni logda görünmedi (log kısaltması). Hata nedeni artık iş loguna da yazılıyor. Diğer bütün turlarda ve sürümlerde geçti |
| Android 15'te belge seçicide öğe bayatladı | 1 kez | Test seçiciyi yeniden arar. Uygulama hatası değil |
| Önceki sürümde görülen Play Hizmetleri kaynaklı kapanma | Bu sürümün son turunda görülmedi | Kanıtlı altyapı nedeninde tek yeniden deneme kuralı korunuyor; yeniden deneme olursa sonuca yazılır |

## Denenemeyenler

Gerçek telefon; Android 9, 10, 12; gerçek Play Billing bağlantısı, satın alma, iptal, bekleyen ödeme ve geri yükleme; üretim AdMob kimlikleri ve gerçek reklam; Avrupa rıza formu; ÖSYM sayfasının içeriği; ana ekrana bileşen ekleme; gerçek bir 2.1.0 kurulumunun üstüne 2.1.1; geri yükleme sırasında gerçek güç kaybı; kendi anahtarınızla `yayin-aab` işi. Yapılacaklar: `docs/kalan-manuel-isler.md`.
