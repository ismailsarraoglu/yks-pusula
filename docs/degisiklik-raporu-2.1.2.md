# Değişiklik raporu: 2.1.1 → 2.1.2

Dal: `surum-2.1.2`. Sürüm 2.1.2, versionCode 5. Yeni özellik eklenmedi; tasarım değişmedi.

## 1. Günlük rota tekrarı (hata)

| | |
|---|---|
| Belirti | Ana sayfadaki "Bugünkü Rotan" ve koç ekranında aynı öneri iki veya daha fazla kez görünüyordu |
| Neden | Rota dört yerden oluşturuluyor: ilk kurulumun sonu, ana sayfa, çalışma ekranı ve koç ekranı. `ensureRoute` "bugün rota yok mu?" diye bakıp sonra yazıyordu; bu iki adım arasında başka bir çağrı da aynı kontrolü yapınca rota iki kez yazılıyordu. Özellikle ilk kurulum biter bitmez ana sayfa açıldığında olur |
| Düzeltme | Rota oluşturma ve yenileme tek kilit (`Mutex`) altında. Öneriler tür, konu, kaynak ve başlıkla eşsizleştirilir. Önceki sürümün yazdığı kopyalar açılışta silinir (karar verilmiş öneri korunur, bekleyen kopya silinir) |
| Yan hata | Bir öneri yarına taşındığında yarının günü "rotası var" sayılıyor ve o gün yeni öneri oluşturulmuyordu. Artık yalnızca taşınan öğeler varsa gün oluşturulmamış sayılır |
| Dosyalar | `data/Repository.kt`, `data/AppDatabase.kt` (yalnızca sayım sorgusu; tablo yapısı ve veritabanı sürümü değişmedi) |
| Test | `DataFlowTest.routeIsNotDuplicated`: 6 eşzamanlı çağrı, eski kopyanın temizlenmesi, taşınan öğeden sonra yeni günün rotası |

## 2. Doğum tarihi ve yaş ekranı kaldırıldı

- Ana sayfadaki doğum tarihi kartı, `AgeGate` ve ilgili kod kaldırıldı. 2.1.1'de girilmiş doğum ayı ve yılı ilk açılışta cihazdan silinir.
- Dosyalar: `ui/screens/HomeScreens.kt`, `domain/Logic.kt`, `monetize/Monetize.kt`. Testler: `AgeGateTest` ve arayüzdeki yaş ekranı testi kaldırıldı.

## 3. Reklam bütün kitleye, Families kurallarıyla

| Konu | 2.1.1 | 2.1.2 |
|---|---|---|
| Kime | Yalnızca doğrulanmış 18+ | Reklamsız hakkı olmayan herkes (üçüncü açılıştan itibaren, eskisi gibi) |
| SDK | GMA Next-Gen (`ads-mobile-sdk` 1.6.0): Families sertifikalı listede yok | Google Mobile Ads SDK `play-services-ads` 25.5.0: listede ("Google AdMob, 19.0.0 or later") |
| Yaş işlemi | UNSPECIFIED | TFCD + TFUA açık (AdMob'a göre çocuk işlemi: kişiselleştirme ve yeniden pazarlama kapalı, reklam kimliği gönderilmez) |
| İçerik | PG | G |
| İzinler | AD_ID vardı | AD_ID ve AdServices (reklam kimliği, Topics, atıf) izinleri kaldırıldı |
| Rıza formu | Yalnızca 18+ | Herkese, rıza yaşı altı etiketiyle |

Dosyalar: `monetize/Ads.kt`, `ui/components/AdSlot.kt`, `res/layout/ad_native.xml`, `AndroidManifest.xml`, `app/build.gradle.kts`. Görünüm aynı (aynı yerleşim ve kart tasarımı; yalnızca SDK sınıfları değişti).

Testler: `DataFlowTest.adsAreChildSafeForEveryone` (çevrimdışı: yapılandırma, izinler, eski doğum bilgisinin silinmesi), `AdOnlineTest` (Play imajında gerçek test reklamı yüklenirken yapılandırmanın TFCD/TFUA/G olduğu), CI'da APK izin denetimi.

**Politika riski:** `docs/gizlilik-ve-reklam-raporu.md` ("Açık kalan politika riski").

## 4. Test altyapısı

- Android'in sistem sürecinin yerel çökmesi (emülatörde görüldü) kanıtlı altyapı nedeni sayılır: test bir kez yeniden denenir, not düşülür.
- Yeniden başlatmadan sonra geri gelmeyen emülatör aynı veriyle yeniden açılır; kalıcılık sonucu bunu not eder.
- CI derleme işine "reklam kimliği izni yok" aşaması eklendi.
- Son turda bu kurtarmaların hiçbiri gerekmedi.

## 5. Belgeler

Gizlilik politikası, gizlilik ve reklam raporu, veri güvenliği envanteri, AdMob talimatı (içerik sınırı G), sürüm notları, kalan işler, teslim ve test raporları.
