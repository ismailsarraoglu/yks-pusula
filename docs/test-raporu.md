# Test raporu (2.1.2)

Sürüm 2.1.2 (versionCode 5). Test edilen kod: `c181571`, son tam tur `42388476039`, 7 Kasım 2026. Dal: `surum-2.1.2`.
Testler GitHub Actions üzerindeki Android emülatörlerinde çalıştı. **Gerçek telefonda ve gerçek Google Play hesabıyla test yapılmadı.** Önceki raporlar: `docs/test-raporu-2.1.1.md`, `docs/test-raporu-2.1.0.md`.

## Son tam tur: kalite kapısı PASS, yeniden deneme yok

| İş | Sonuç | Kapsam |
|---|---|---|
| Derleme | PASS (8/8) | Birim testleri (73; 72 geçti, 1 kapı denemesi testi atlandı), debug + sideload + test APK'sı, Play derlemesinin anahtarsız durması, hata ayıklanamaz, **reklam kimliği ve AdServices izni yok**, hedef SDK 36, APK imzası |
| Android 8 | PASS (7/7) | 55 cihaz testi, release APK |
| Android 11 | PASS (26/26) | Bütün cihaz testleri (64), akışlar, ana düğmeler 5 kez, geçiş stresi 3 tur, 5 ekran yapılandırması, büyük yazı ve yatay düzen, kalıcılık, release APK |
| Android 13 | PASS (7/7) | 55 cihaz testi, release APK |
| Android 14 | PASS (10/10) | 63 cihaz testi (arayüz dahil), kalıcılık, release APK |
| Android 15 | PASS (35/35) | 63 test, yaşam döngüsü, kalıcılık, hareketle ve üç düğmeli gezinme, ekran kesiği, bildirim, hatırlatma, saat dilimi, PDF/fotoğraf/yedek seçicileri, ÖSYM bağlantısı, çevrimiçi reklam, Billing başlatma, release APK |
| Android 16 | PASS (10/10) | 63 cihaz testi, kalıcılık, release APK |
| Android 14 Play imajı | PASS (10/10) | Gerçek test reklamı (yeni SDK ile), reklam üstünden kaydırma koruması, 6/6 temiz kurulum |
| Android 16 Play imajı | PASS (10/10) | Aynısı |

Son turda hiçbir aşama yeniden denenmedi; altyapı kurtarması gerekmedi. Uygulama çökmesi 0, ANR 0.

Ölçümler (release ayarlı APK, emülatör): soğuk açılış Android 11'de 826, 746, 710 ms; Android 15'te 1567, 1648, 1597 ms. Bellek Android 11 ve 15'te 73 ve 59 MB PSS. Yeni reklam SDK'sıyla bellek ve açılış süresi 2.1.1'e göre arttı (2.1.1: Android 11'de 662-720 ms, 35 MB). Bu ölçümler emülatörde ve tek seferliktir.

## 2.1.2'ye özel testler

| Konu | Test | Sonuç |
|---|---|---|
| Günlük rota tekrarı: 6 eşzamanlı oluşturma tek rota yazar | `DataFlowTest.routeIsNotDuplicated` | PASS (Android 8, 11, 13, 14, 15, 16) |
| Eski sürümden kalan kopya öneri açılışta silinir | Aynı test | PASS |
| Yarına taşınan öneri yarının rotasını engellemez | Aynı test | PASS |
| Önceki rota kararları korunur | `DataFlowTest.routeDecisionsPersist` | PASS |
| Reklam yapılandırması herkese çocuk korumalı (TFCD, TFUA, G) | `DataFlowTest.adsAreChildSafeForEveryone` (çevrimdışı), `AdOnlineTest` (Play imajında gerçek test reklamıyla) | PASS |
| Reklam kimliği ve AdServices izinleri APK'da yok | Aynı test ve CI `manifest-reklam-kimligi-yok` | PASS |
| 2.1.1'den kalan doğum bilgisi silinir | `DataFlowTest.adsAreChildSafeForEveryone` | PASS |
| Yeni SDK ile yerel reklam yüklenir, reklamsız hak sonrası kaybolur | `AdOnlineTest.testAdLoadsAndPurchaseRemovesIt` | PASS (Android 14, 15, 16 Play imajı) |
| Reklamın üstünden başlayan kaydırma reklamı açmaz | `AdOnlineTest.dragOverAdDoesNotOpenIt` | PASS (Android 14, 16 Play imajı) |

## Bu sürümde düşen ve düzeltilen aşamalar

Son tura gelene kadar 6 tam tur çalıştı. Düşen aşamaların hiçbiri uygulama hatası değildi. Hepsi kayıt altında; çözüm olarak ya testin zamanlaması düzeltildi ya da kanıtlı altyapı kuralı eklendi.

| Tur | Düşen | Neden | Yapılan |
|---|---|---|---|
| `42370724897` | Android 11 kalıcılık | Android'in sistem süreci emülatörde yerel olarak çöktü (tombstone) | Kanıtlı altyapı nedeni: test bir kez yeniden denenir, not düşülür |
| `42370724897` | Android 15, yeniden başlatmadan sonraki her şey | Emülatör yeniden başlatmadan geri gelmedi | Önce adb bağlantısı yenilenir |
| `42373624901` | (Hepsi geçti) | Yeni SDK'nın reklam kimliği ve ilgi alanı izinleri eklediği görüldü | İzinler kaldırıldı, CI denetimi eklendi |
| `42376286973` | Android 15 ÖSYM bağlantısı | Chrome isteği dağıtıp ilk açılış ekranına geçti; test isteği okuyamadı | Test son görevler kaydını da okur |
| `42379268070` | Android 15 PDF | Sayfa görüntüsü, sayfa sayısı yazısından sonra çizildi | Test görüntüyü bekler |
| `42382441882` | Android 15 reklam | Sistem Play Hizmetleri'ni durdurdu ve bağlı uygulamayı kapattı | Kanıtlı altyapı nedeni |
| `42385335375` | Android 16, yeniden başlatma sonrası okuma | Emülatör yeniden başlatmadan dönmedi, zorla kapatılıp açıldı (ani güç kesilmesi gibi); son yazılan veri okunamadı | Zorla kapatma artık "yeniden başlatma" sayılmaz; kalıcılık sırası baştan tekrarlanır ve bu sonuca yazılır |

**Not (bilinen sınır):** Son satırdaki durum gerçek bir yeniden başlatma değil, ani güç kesilmesine benzer. Android'in veritabanı (SQLite, WAL kipi), ani güç kesilmesinde son birkaç saniye veya dakikada yazılan kayıtları kaybedebilir; veritabanı bozulmaz. Normal yeniden başlatmada veriler bütün sürümlerde korundu. Gerçek telefonda ani güç kesilmesi denenmedi.

## Kalite kapısı (2.1.1'de kanıtlandı, değişmedi)

Kasıtlı birim testi hatası, cihaz testi hatası ve emülatörün açılmaması: üçünde de CI başarısız oldu (`docs/test-raporu-2.1.1.md`).

## Denenemeyenler

Gerçek telefon; Android 9, 10, 12; gerçek Play Billing bağlantısı ve satın alma; gerçek AdMob kimlikleri; Avrupa rıza formu; ÖSYM sayfasının içeriği; gerçek telefonda ani güç kesilmesi; **imzalı Play AAB (anahtar ve kimlikler yok)**. Ayrıntı: `docs/kalan-manuel-isler.md`.
