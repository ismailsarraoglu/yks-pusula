# Değişiklik raporu: 2.1.0 → 2.1.1

Dal: `duzeltme-2.1.1` (2.1.0 `surum-2.1` dalında ve `08a4f6b` kaydında olduğu gibi duruyor). Yeni özellik eklenmedi; değişiklikler güvenilirlik, yayın kalitesi ve politika uyumu içindir. Test sonuçları: `docs/test-raporu.md`, Android 15: `docs/android15-test-raporu.md`.

## 1. Test otomasyonu artık gerçek bir kalite kapısı

| Sorun (2.1.0) | Düzeltme | Dosya |
|---|---|---|
| Derleme, manifest, emülatör, matris, reklam ve Android 15 adımlarında `continue-on-error: true` vardı; testler başarısız olsa da CI yeşil görünebiliyordu | Hiçbir adımda `continue-on-error` yok. İş akışı üç işe ayrıldı: `derle` (birim testleri, derleme, release denetimi, manifest ve imza), `cihaz` (her Android sürümü ayrı ve paralel iş), `sonuc` (özet ve son kapı). Bir aşama PASS değilse iş, `sonuc` ve bütün çalıştırma başarısız olur | `.github/workflows/derleme.yml`, `tools/ozet.py` |
| `smoke.sh` emülatör açılmasa veya testler başarısız olsa da 0 ile bitiyordu | Emülatör açılmazsa çıkış kodu 3, herhangi bir aşama FAIL veya NOT RUN ise 1 | `tools/smoke.sh`, `tools/kapi.sh` |
| `matris.sh` her zaman `true` ile bitiyordu, çalışmayan sürüm raporda görünmüyordu | Her sürüm PASS, FAIL veya NOT RUN olarak `ci/logs/matris.tsv` dosyasına yazılır; süre aşımı ve açılmama FAIL sayılır; biri bile PASS değilse çıkış kodu 1 | `tools/matris.sh`, `tools/cihaz.sh` |
| Raporda yalnızca serbest metin vardı | Her aşama `durum-<iş>.tsv` dosyasında PASS / FAIL / NOT_RUN ve açıklamayla. Önceki aşama başarısız olduğu için çalışmayan aşamalar NOT RUN olarak yazılır; plana alınıp hiç başlamayan iş de NOT RUN görünür. Özet: `logs/DURUM.md` ve GitHub iş özeti | `tools/kapi.sh`, `tools/ozet.py` |
| Yeniden denemeler hatayı gizleyebilirdi | Test yalnızca kanıtlı altyapı nedeninde (Play Hizmetleri'nin bağlı süreci kapatması, sistem süreçlerinin de yanıt vermediği aşırı yük) bir kez yeniden denenir ve bu not sonuca yazılır. Uygulama çökmesi ve doğrulama hatası yeniden denenmez. Reklam testi yalnızca reklam sunucusu reklam vermediğinde (NO_FILL) yeniden denenir | `tools/kapi.sh`, `tools/smoke.sh` |
| Hata durumunda loglar kaybolabiliyordu | Her işin `ci/` klasörü `if: always()` ile artifact olarak saklanır (30 gün). Özet ve bütün loglar ayrıca `ci-results-<dal>` dalına yazılır | `.github/workflows/derleme.yml` |
| Birim testleri Gradle önbelleğinden gelebilirdi | `testDebugUnitTest --rerun`: her çalıştırmada gerçekten yeniden çalışır | `.github/workflows/derleme.yml` |
| Kapının gerçekten durdurduğu kanıtlanmamıştı | Kasıtlı hata denemesi: `kapi_denemesi=birim` ve `kapi_denemesi=cihaz`. Normal çalıştırmada bu testler hiçbir şey yapmaz | `GateProbeTest` (birim ve cihaz) |

Kaldırılan eski betikler: `tools/emulator-tests.sh`, `tools/reklam-teshis.sh` (kaydırma koruması testi artık Android 14 ve 16 Play imajı işlerinde zorunlu aşama), `tools/push.sh`, `tools/wait_ci.sh`, `tools/wait_hash.sh` (yerlerine `tools/ci-sonuc.sh`).

## 2. Android 15 (API 35) test matrisinde

`tools/android15.sh`, CI'da `cihaz-a15` işi. Ayrıntılı sonuç: `docs/android15-test-raporu.md`.

## 3. Reklam ve yaş uygunluğu

| Sorun (2.1.0) | Düzeltme | Dosya |
|---|---|---|
| Rapor, 16-17 ve 18+ seçilirse tarafsız yaş ekranı gerekmediğini söylüyordu. Google 16-17'nin bazı bölgelerde çocuk sayılabileceğini belirtiyor; YKS kitlesinin önemli kısmı 18 altı | Tarafsız yaş ekranı: ana sayfada bir kez, boş ay ve yıl alanı, varsayılan yok, "Belirtmek istemiyorum" seçeneği, onaydan sonra değiştirilemez. Bilgi yalnızca cihazda, yedeğe girmez | `domain/Logic.kt` `AgeGate`, `ui/screens/HomeScreens.kt` `BirthDateCard`, `monetize/Monetize.kt` |
| Reklam SDK'sı (GMA Next-Gen) Families sertifikalı listede değil; herkese reklam isteniyordu | Reklam ve UMP rıza SDK'sı yalnızca 18 yaşını doldurduğu belirlenen kullanıcı için başlatılır. 18 altı, yaşını belirtmeyen ve henüz cevap vermeyen kullanıcıya reklam yok, reklam kimliği aktarımı yok | `monetize/Ads.kt`, `monetize/Monetize.kt` |
| Yaş işlemi belirtilmiyordu, yalnızca PG içerik sınırı vardı | Doğrulanmış yetişkin isteklerinde `AgeRestrictedTreatment.UNSPECIFIED`; PG sınırı korunuyor | `monetize/Ads.kt` |
| Yaş bilgisi uygulama açıldıktan sonra yüklenirse rıza akışı o oturumda hiç başlamayabilirdi | Yaş veya hak değişince rıza bu oturumda sorulmadıysa son etkinlikle başlatılır | `monetize/Ads.kt` `recheck` |
| Gizlilik politikası "uygulama yaş istemez" diyordu | Politika, veri güvenliği envanteri ve gizlilik raporu güncellendi; hukuki ve bölgesel belirsizlikler açıkça yazıldı | `play/gizlilik-politikasi.html`, `play/veri-guvenligi-envanteri.md`, `docs/gizlilik-ve-reklam-raporu.md` |

Gelir etkisi: 18 yaş altındaki kullanıcılar reklam görmez. Alternatif (sertifikalı `play-services-ads` SDK'sına geçip 18 altına çocuk veya genç işlemli reklam) bu sürümde yapılmadı; gerekçe `docs/gizlilik-ve-reklam-raporu.md`.

## 4. Satın alma doğrulaması kapalı varsayılanla (fail-closed)

| Sorun (2.1.0) | Düzeltme | Dosya |
|---|---|---|
| `packageName` yoksa kayıt kabul ediliyordu | Paket adı zorunlu ve `app.ykspusula` ile tam eşleşmeli | `monetize/Entitlement.kt` |
| `purchaseState` yoksa kayıt kabul ediliyordu | Durum zorunlu, sayı olmalı ve yalnızca 0 (PURCHASED) kabul edilir. Metin olarak gelen durum reddedilir | `monetize/Entitlement.kt` |
| Belirteç denetlenmiyordu | JSON'daki `purchaseToken` zorunlu ve BillingClient'ın verdiği belirteçle aynı olmalı; cihazda saklanan kaydın belirteç özeti de JSON'la karşılaştırılır | `monetize/Entitlement.kt`, `monetize/Billing.kt` |
| Ürün listesinde fazladan ürün veya harf farkı | Ürün listesi tam olarak `[remove_ads_lifetime]` olmalı; ürün alanı yoksa kayıt bozuk sayılır | `monetize/Entitlement.kt` |
| Test, paket adı olmayan JSON'u OK kabul ediyordu | Test düzeltildi: artık MALFORMED beklenir. Eksik, yanlış türde ve tutarsız alanlar için olumsuz testler eklendi | `ProductLogicTest`, `TestPurchase`, `DataFlowTest` |

## 5. Yedekleme ve geri yükleme

| Sorun (2.1.0) | Düzeltme | Dosya |
|---|---|---|
| Birleştirmede (`replace=false`) aynı adlı ek dosya önce siliniyor, yeni dosya taşınamazsa eski dosya kayboluyordu | Eski dosya silinmez, önce `.eski-…` adıyla kenara alınır; yeni dosya taşınır; hata olursa eski dosya yerine döner. Her adım geri yükleme günlüğüne yazılır | `data/Backup.kt` `moveStaged` |
| Birleştirmede güvenlik kopyası alınmıyordu | Her iki kipte de güvenlik kopyası (veritabanı, ekler, ayarlar) alınır | `data/Backup.kt` |
| Değiştirmede (`replace=true`) ayarlar hata yönetiminin dışındaydı; ayar yazılamazsa geri dönüş olmuyordu | Veritabanı, dosyalar ve ayarlar tek `try` içinde. Herhangi bir hata önce dosyaları günlükten geri alır, sonra güvenlik kopyasından veritabanını ve ayarları geri yükler | `data/Backup.kt` `restore`, `revertFromJournal` |
| Geri dönüş ayarları geri yüklemiyordu | `rollback` güvenlik kopyasındaki ayarları da uygular | `data/Backup.kt` `rollback` |
| Uygulama geri yükleme sırasında kapanırsa yarım kalan işlem kalıyordu | Günlük varsa açılışta önceki hâle dönülür; Yedekleme ekranında bilgi gösterilir | `data/Backup.kt` `recoverIfInterrupted`, `YksApp.kt`, `ui/screens/ProfileScreens.kt` |

Yeni testler (`BackupSecurityTest`): aynı adlı dosyanın güvenli değişimi, dosya taşıma hatası, veritabanı adımında hata, DataStore (ayarlar) yazma hatası, ayarlardan sonra hata, yarım kalan geri yüklemenin sonraki açılışta kurtarılması, başarılı tam değiştirme. Hatalar test sırasında belirli aşamalara kasıtlı olarak sokulur (`RestoreFaults`); uygulamada bu kanca boştur.

## Depo güvenliği (7 Kasım 2026)

GitHub Actions'ın ücretsiz dakikaları bittiği için depo testler süresince herkese açık yapıldı. Açılmadan önce temizlenmesi gerekiyordu; kısa bir süre temizlenmeden açık kaldı.

- Bütün Git geçmişinden eski yan yükleme anahtarı (`app/signing/yks-pusula-sideload.p12`) ve parolası silindi; geçmiş yeniden yazıldı. Kod değişmedi.
- GitHub'daki bütün dallar yeniden indirilip tarandı: anahtar dosyası, parola, özel anahtar veya API anahtarı bulunmadı.
- Eski anahtar ele geçmiş sayılır ve kullanılmaz (`docs/kalan-manuel-isler.md`). Play yükleme anahtarı depoda hiç olmadı; Play sürümü etkilenmez.
- Kayıt kimlikleri değişti: 2.0.0 `d6ef13c` (`gelistirme` dalı), 2.1.0 `08a4f6b` (`main` dalı). Eski geçmişin yedeği kullanıcıya ayrıca verildi.

## Profil ekranı (ek talimat)

- Profil düzenlemede "YDT dili" bölümü yalnızca hedef alan Dil iken görünür; alan değişince hemen açılır veya gizlenir. Seçim eskisi gibi isteğe bağlıdır (varsayılan İngilizce).
- Bölüm gizliyken kayıttaki dil değişmez; veritabanı yapısı aynı. Dosya: `ui/screens/ProfileScreens.kt`. Test: `UiFlowTest.profileEditShowsYdtLanguageOnlyForDil`.

## Android 15 testinde bulunup düzeltilenler

- Yatay ekranda alt menü kamera kesiğinin altına giriyordu (hareketle gezinme, üç düğme ve kesik öykünmesinde). Alt menü artık gezinme çubuğu ile yatay ekran kesiği boşluğunu birlikte uygular. Dosya: `ui/AppRoot.kt`.
- Sistem testleri (bildirim, hatırlatma, saat dilimi, dosya ve fotoğraf seçici, yedek) JUnit kuralına uymadığı için hiç başlamıyordu ("should be void"). Düzeltildi.
- Çevrimiçi aşamadaki internet denetimi ping kullanıyordu; GitHub sunucuları ping geçirmediği için reklam ve Billing aşamaları çalışmıyordu. Android'in doğrulanmış ağ bilgisine geçildi.

## Sürüm numarası

Sürüm numarası yalnızca bütün tur geçtikten sonra yükseltilir (versionCode 4, versionName 2.1.1). Durumu: `docs/test-raporu.md`.
