# Gizlilik ve reklam denetimi (2.1.2)

Tarih: 7 Kasım 2026. 2.1.2'de yaş ekranı kaldırıldı ve reklam modeli değişti; bu rapor ona göre güncellendi. Kod ve politika metinleri karşılaştırıldı. Hukuki görüş değildir.

## Reklam

| Kontrol | Sonuç | Dayanak |
|---|---|---|
| Biçim | Yalnızca yerel (native) reklam; banner, geçiş ve ödüllü reklam yok | `monetize/Ads.kt`, `ui/components/AdSlot.kt` |
| Yerleşim | Yalnızca Ana sayfa, Çalış, Denemeler, İstatistik; içerik akışında bir kart olarak. Odaklanma, deneme sayacı, sonuç girişi, yedekleme ve satın alma ekranlarında yok | `AdPlacement` |
| Etiket | Kartta "Reklam" etiketi ve reklamveren bilgisi; uygulama içeriğinden ayrı görünür | `res/layout/ad_native.xml` |
| Yanlış tıklama | Reklamın üstünden başlayan kaydırma tıklama sayılmaz (2.0.0'da testte bulunan sorun). Üstüne şeffaf katman, sahte kapatma düğmesi veya yönlendirme yok | `monetize/AdTouchGuard.kt` |
| Kaydırma alanına bindirme | Reklam içeriğin üstünde yüzmez; sayfanın akışında yer alır | AdMob politikası: reklam içerik üstünde yüzmemeli |
| Sıklık | Aynı alan en erken 90 saniyede yeni reklam ister; ilk 3 açılışta reklam yok | `AdsConfig` |
| Kime gösterilir | Reklamsız hakkı olmayan herkese (üçüncü açılıştan itibaren). Yaş sorulmaz | `AdsManager` |
| SDK | Google Mobile Ads SDK `play-services-ads` 25.5.0: Families Self-Certified Ads SDK listesinde ("19.0.0 or later") | `app/build.gradle.kts` |
| Yaş işlemi | Her istekte çocuğa yönelik işlem (TFCD) ve rıza yaşı altı (TFUA) açık. AdMob'a göre bu, çocuk işlemine (CHILD) karşılık gelir: kişiselleştirilmiş reklam ve yeniden pazarlama kapalı, reklam kimliği gönderilmez | `AdsManager.requestConfiguration`, testler: `DataFlowTest.adsAreChildSafeForEveryone`, `AdOnlineTest` |
| İçerik düzeyi | G (genel izleyici) | Aynı |
| Reklam kimliği ve ilgi alanı | `com.google.android.gms.permission.AD_ID` ve SDK'nın eklediği Privacy Sandbox izinleri (`ACCESS_ADSERVICES_AD_ID`, `_TOPICS`, `_ATTRIBUTION`) manifestten kaldırıldı. CI her derlemede APK izinlerini denetler, cihaz testi de doğrular | `AndroidManifest.xml` |
| Rıza formu | UMP isteği rıza yaşı altı etiketiyle yapılır | `AdsManager.onActivityStart` |
| Kimlik ayrımı | Debug ve yan yükleme derlemesi Google test kimlikleri; Play derlemesi test kimliğiyle derlenemez | `app/build.gradle.kts` |
| Reklamsız kullanıcı | Hak açıksa rıza istenmez, SDK başlatılmaz, reklam isteği yapılmaz | `AdsManager.onActivityStart`, test: `DataFlowTest.entitlementFlowAndAdsOff` |
| İnternet yok | Reklam alanı görünmez, uygulama çalışır | Uçak modu testleri |

## Rıza (UMP)

- Her açılışta rıza bilgisi güncellenir; gerekiyorsa form gösterilir. Reklam isteği yalnızca `canRequestAds()` doğruysa yapılır.
- Gizlilik seçenekleri gerekiyorsa Profil'de "Gizlilik seçenekleri" görünür.
- **Denenmedi:** Avrupa Ekonomik Alanı konumunda gerçek AdMob GDPR iletisi. AdMob panelinde ileti oluşturulmalı (`play/admob-talimat.md`).

## Satın alma

- Ürün: `remove_ads_lifetime`, tek seferlik, yalnızca reklamları kaldırır. Eğitim özelliği satılmaz.
- Doğrulama cihazda ve kapalı varsayılanla (fail-closed) yapılır. Hak yalnızca şu koşulların hepsi sağlanınca açılır:
  - Play açık anahtarı tanımlı ve imza geçerli,
  - `packageName` var ve `app.ykspusula` ile aynı,
  - `purchaseState` var, sayı ve 0 (PURCHASED),
  - ürün listesi tam olarak `remove_ads_lifetime` (fazla ürün, büyük harf veya boşluk farkı reddedilir),
  - `purchaseToken` var, boş değil ve BillingClient'ın verdiği belirteçle aynı,
  - cihazda saklanan kayıtta belirteç özeti JSON'daki belirteçle aynı.
  Alanlardan biri eksik, yanlış türde veya tutarsızsa satın alma onaylanmaz ve hak açılmaz. 2.1.0'da paket adı veya durum eksik kayıt kabul edilebiliyordu; bu düzeltildi. Testler: `ProductLogicTest` (`missingOrInconsistentFieldsRejected`, `cachedTokenMustMatchJson`), `DataFlowTest`.
- Bekleyen ödeme hak açmaz; tamamlandığında açılır. Açılışta ve öne gelişte satın almalar yeniden sorgulanır; "Satın almaları geri yükle" aynı sorguyu çalıştırır.
- **Güvenlik sınırı:** Sunucu olmadığı için doğrulama atlatılabilir (root'lu cihaz, değiştirilmiş APK). Tam sahtekârlık koruması yoktur.
- **Denenmedi:** Gerçek Play ile satın alma, iptal, bekleyen ödeme, geri yükleme. Play Console ürünü ve lisans test hesabı gerekir.

## Hedef kitle ve yaş (2.1.2)

### Karar

Yaş ekranı istek üzerine kaldırıldı. Uygulama kimsenin yaşını bilmediği için **her kullanıcı "yaşı bilinmeyen" sayılır ve her reklam isteği çocuklara uygulanan kurallarla yapılır.** Böylece reklam bütün kitleye gösterilir ve 18 yaş altı kullanıcı da çocuklar için gereken korumanın aynısını alır.

| Families / AdMob şartı (resmî kaynak) | 2.1.2'de |
|---|---|
| Çocuklara ve yaşı bilinmeyen kullanıcılara reklam yalnızca Families Self-Certified Ads SDK'larıyla | `play-services-ads` 25.5.0 (listede: Google AdMob, 19.0.0 ve sonrası). 2.1.1'deki Next-Gen SDK listede olmadığı için değiştirildi |
| Çocuklara kişiselleştirilmiş reklam yok, yeniden pazarlama yok | TFCD ve TFUA her istekte açık |
| Çocuklardan ve yaşı bilinmeyenlerden reklam kimliği gönderilmez | TFCD açık ve AD_ID izni kaldırıldı |
| Çocuklara uygun reklam içeriği; AdMob: çocuk ve yaşı bilinmeyen kullanıcılarda en yüksek içerik derecesi G | G |
| Karma kitlede "tarafsız yaş ekranı gibi yaş ayırma önlemleri" | **Uygulanmıyor** (aşağıya bakın) |

### Açık kalan politika riski

Google Play Families politikası özetinde "karma kitleli uygulama, çocukların yalnızca kişiselleştirilmemiş reklam görmesini sağlamak için tarafsız yaş ekranı içermelidir" yazıyor. Bu ekranın amacı, çocuklara uygun olmayan reklamların yalnızca büyüklere gösterilmesidir. 2.1.2'de hiçbir kullanıcıya çocuklara uygun olmayan veya kişiselleştirilmiş reklam gösterilmez; bu yüzden yaş ekranının koruduğu sonuç herkese uygulanmış olur. Ancak politika metni yaş ekranını açıkça istiyor ve "herkese çocuk gibi davranmanın" yaş ekranının yerine geçtiğini açıkça söylemiyor. Google'ın AdMob yardım sayfası da yalnızca "çocuklara ve yaşı bilinmeyen kullanıcılara sunulan istekler" için ne yapılacağını anlatıyor. **Play incelemesinin bu yorumu kabul edeceği garanti değildir.** Ret gelirse iki yol var: tarafsız yaş ekranını geri eklemek (2.1.1'deki kod Git geçmişinde) veya hedef kitleyi gerçeğe uygun biçimde yeniden değerlendirmek. Hedef kitleyi gerçeğe aykırı biçimde yalnızca 18+ seçmek önerilmez.

### Play Console'da

- **Hedef kitle:** gerçek kitle: 13-15 (9. ve 10. sınıf kullanıcılar varsa), 16-17, 18+. Families politikası uygulanır.
- **Reklam kimliği beyanı:** "Uygulamam reklam kimliğini kullanmıyor" (AD_ID izni yok).
- **Reklam içeriyor:** Evet.
- **AdMob paneli:** içerik sınırı G; uygulama ayarlarında aileler için uygun SDK ve çocuk işlemi koddan gelir.

### Gelir etkisi

Reklam artık herkese gösterilir (2.1.1'de yalnızca doğrulanmış 18+). Ama bütün istekler kişiselleştirilmemiş ve G içerikli olduğu için reklam başına gelir kişiselleştirilmiş reklama göre düşük olur ve doluluk oranı azalabilir.

### Hukuki ve bölgesel belirsizlik

Google, yaş işlemi seçimi için hukuk danışmanına yönlendiriyor ve araçlarının yasal yükümlülükleri kaldırmadığını söylüyor. Türkiye'de KVKK ayrı bir dijital rıza yaşı belirlemez; AEA'da ülkeye göre değişir. Bu rapor hukuki görüş değildir.

## Veri

- Çalışma verileri yalnızca cihazda. Uygulamanın sunucusu, hesabı, analitik veya çökme raporu SDK'sı yok.
- İnternet yalnızca AdMob, UMP ve Play Billing için.
- Android bulut yedeği kapalı (`dataExtractionRules`).
- 2.1.2'de reklam SDK'sı değişti (`play-services-ads`), AD_ID izni kaldırıldı. Yaş bilgisi artık saklanmaz; 2.1.1'den kalan doğum ayı ve yılı ilk açılışta silinir.

## Gizlilik politikasında sizin doldurmanız gerekenler

`play/gizlilik-politikasi.html`:

1. `[GELİŞTİRİCİ ADI]`: Play Console'daki geliştirici adınız.
2. `[GELİŞTİRİCİ E-POSTA ADRESİ]`: Play Console'da doğruladığınız iletişim e-postası.

Sonra dosyayı herkese açık bir adreste yayımlayıp adresi Play Console'a girin.

## Kaynaklar

- [Hedef kitle ve uygulama içeriği ayarları](https://support.google.com/googleplay/android-developer/answer/9867159?hl=en)
- [Families politikası](https://support.google.com/googleplay/android-developer/answer/9893335?hl=en)
- [Families Self-Certified Ads SDK Program](https://support.google.com/googleplay/android-developer/answer/9283445?hl=en)
- [GMA Next-Gen SDK hedefleme: AgeRestrictedTreatment](https://developers.google.com/admob/android/next-gen/targeting)
- [AdMob yardım: yaş işlemi etiketleri (TFAT)](https://support.google.com/admob/answer/9009425?hl=en)
- [AdMob yardım: Google Play Families politikasına uyum](https://support.google.com/admob/answer/6223431?hl=en)
- [Google Mobile Ads SDK sürüm notları](https://developers.google.com/admob/android/rel-notes)
- [AdMob reklam yerleşimi ilkeleri](https://support.google.com/admob/answer/6293636?hl=en)
- [Uygulama imzalama](https://developer.android.com/studio/publish/app-signing)
