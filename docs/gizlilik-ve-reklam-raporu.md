# Gizlilik ve reklam denetimi (2.1.1)

Tarih: 7 Kasım 2026 (2.1.0 raporunun yaş ve satın alma bölümleri yeniden değerlendirildi). Kod ve politika metinleri karşılaştırıldı. Hukuki görüş değildir.

## Reklam

| Kontrol | Sonuç | Dayanak |
|---|---|---|
| Biçim | Yalnızca yerel (native) reklam; banner, geçiş ve ödüllü reklam yok | `monetize/Ads.kt`, `ui/components/AdSlot.kt` |
| Yerleşim | Yalnızca Ana sayfa, Çalış, Denemeler, İstatistik; içerik akışında bir kart olarak. Odaklanma, deneme sayacı, sonuç girişi, yedekleme ve satın alma ekranlarında yok | `AdPlacement` |
| Etiket | Kartta "Reklam" etiketi ve reklamveren bilgisi; uygulama içeriğinden ayrı görünür | `res/layout/ad_native.xml` |
| Yanlış tıklama | Reklamın üstünden başlayan kaydırma tıklama sayılmaz (2.0.0'da testte bulunan sorun). Üstüne şeffaf katman, sahte kapatma düğmesi veya yönlendirme yok | `monetize/AdTouchGuard.kt` |
| Kaydırma alanına bindirme | Reklam içeriğin üstünde yüzmez; sayfanın akışında yer alır | AdMob politikası: reklam içerik üstünde yüzmemeli |
| Sıklık | Aynı alan en erken 90 saniyede yeni reklam ister; ilk 3 açılışta reklam yok | `AdsConfig` |
| Kime gösterilir | Yalnızca tarafsız yaş ekranında 18 yaşını doldurduğu belirlenen kullanıcıya. Yaş bilinmiyorsa, 18 altındaysa veya "Belirtmek istemiyorum" seçildiyse reklam ve UMP rıza SDK'sı hiç başlatılmaz | `AgeGate`, `AdsManager.onActivityStart`, `Monetize.verifiedAdult` |
| Yaş işlemi (AgeRestrictedTreatment) | UNSPECIFIED: istekler yalnızca doğrulanmış yetişkinler için yapıldığından | `RequestConfiguration` |
| İçerik düzeyi | Ayrıca PG ile sınırlı | `RequestConfiguration` |
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

## Hedef kitle ve yaş (yeniden değerlendirme)

### 2.1.0 değerlendirmesi neden yetersizdi

2.1.0 raporu "16-17 ve 18+ seçilirse tarafsız yaş ekranı gerekmez" diyordu. Bu doğru değildi. Google Play'e göre 13-15 ve 16-17 grupları bazı bölgelerde çocuk sayılabilir. Bu grupları 18+ ile birlikte seçen uygulamanın kitlesi karma kabul edilir; Families politikası uygulanır ve karma kitlede tarafsız yaş ekranı gerekir. YKS'ye hazırlananların önemli bir kısmı 18 yaşından küçüktür (11. ve 12. sınıf, bazı 9. ve 10. sınıf öğrencileri). Hedef kitleyi yalnızca 18+ seçmek gerçeğe aykırı olurdu; bu yapılmadı.

### Resmî kaynaklardan çıkanlar (7 Kasım 2026'da okundu)

| Konu | Kaynak ne diyor |
|---|---|
| Hedef kitle | 13-15 ve 16-17 yaş grupları bazı bölgelerde çocukları içerebilir. Çocuk ve yetişkinleri birlikte hedefleyen uygulamada tarafsız yaş ekranı gerekir. Geçerli örnek: doğum tarihinin serbestçe girilmesi. Geçersiz örnek: tarihin önceden doldurulması veya belirli bir yaşa yönlendirme. |
| Families reklam şartı | Çocuklara gösterilebilecek reklamlar yalnızca Families Self-Certified Ads SDK listesindeki SDK'lardan gelmeli. Karma kitlede sertifikasız SDK yalnızca çocuk olmayan kullanıcılara reklam için kullanılabilir (ör. tarafsız yaş ekranıyla ayırarak). Çocuklara kişiselleştirilmiş reklam yasak. |
| Sertifikalı SDK listesi | Google AdMob için yalnızca `com.google.android.gms:play-services-ads` 19.0.0 ve sonrası listede. Uygulamanın kullandığı **GMA Next-Gen SDK (`com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk`) listede yok.** |
| AgeRestrictedTreatment | Next-Gen SDK'da `setAgeRestrictedTreatment(CHILD / TEEN / UNSPECIFIED)`. TFCD ve TFUA kullanımdan kaldırıldı; ikisinden biri TRUE ise CHILD karşılığıdır. Hangi değerin kullanılacağı için Google "hukuk danışmanınıza danışın" diyor; seçim ölçütü vermiyor. |
| CHILD ve TEEN etkisi (AdMob yardım) | CHILD: kişiselleştirilmiş reklam ve yeniden pazarlama kapalı, üçüncü taraf reklam isteği kapalı, reklam kimliği gönderilmez, çocuk korumaları uygulanır. TEEN: kişiselleştirilmiş reklam ve yeniden pazarlama kapalı, gençlere özel korumalar uygulanır. UNSPECIFIED: özel yaş işlemi yok. |
| İçerik derecesi | `setMaxAdContentRating` (G, PG, T, MA) yalnızca reklamın içerik derecesini sınırlar. Yaş işlemi veya Families şartlarının yerini tutmaz. |

### Karar ve uygulanan çözüm

1. **Tarafsız yaş ekranı eklendi.** Ana sayfada bir kez doğum ayı ve yılı sorulur. Alanlar boş gelir, varsayılan yaş yok, belirli bir yaşa yönlendiren metin yok, "Belirtmek istemiyorum" seçeneği var. Kaydedilmeden önce onay istenir ve sonradan değiştirilemez (yaşı deneme yanılmayla değiştirip reklam açmayı zorlaştırmak için). Bilgi yalnızca cihazda saklanır, yedeğe girmez, reklam ağına gönderilmez.
2. **Reklam yalnızca 18 yaşını doldurmuş kullanıcıya.** Türkiye'de ergin olma yaşı 18'dir; Next-Gen SDK sertifikalı listede olmadığından 18 altındaki kullanıcıya bu SDK ile reklam gösterilmez. 18 altı, yaşını belirtmeyen ve yaş ekranını henüz görmemiş kullanıcı için UMP rıza ve reklam SDK'sı hiç başlatılmaz; reklam isteği ve reklam kimliği aktarımı olmaz. Uygulamanın bütün çalışma özellikleri herkes için aynıdır.
3. **Yetişkin isteklerinde UNSPECIFIED.** İstekler yalnızca doğrulanmış yetişkinler için yapıldığından CHILD veya TEEN gerekmez. PG içerik sınırı ayrıca korunur.
4. **Play Console önerisi:** hedef yaş gruplarını gerçek kitleye göre seçin: 13-15 (9. ve 10. sınıf kullanıcılar varsa), 16-17 ve 18+. Bu karma kitle demektir; Families politikası uygulanır. Uygulama şu anda bu şartlar için gerekeni yapar: tarafsız yaş ekranı, çocuklara reklam yok, çocuklardan reklam kimliği aktarımı yok.

### Gelir etkisi ve alternatif

- 18 yaş altındaki ve yaşını belirtmeyen kullanıcılar reklam görmez. Kitlenin büyük kısmı 16-17 yaşındaysa reklam geliri belirgin biçimde azalır.
- Alternatif: reklam SDK'sını sertifikalı `play-services-ads` (klasik GMA SDK) ile değiştirip 18 altına çocuk veya genç işlemli, kişiselleştirilmemiş reklam göstermek. Bu bir SDK değişikliğidir (yeni özellik değil ama kapsamlı bir geçiş); bu sürümde yapılmadı. Yapılırsa yeniden test ve yeniden politika değerlendirmesi gerekir.
- Google, Next-Gen SDK'yı sertifikalı listeye eklerse 18 altı için TEEN veya CHILD işlemiyle reklam yeniden değerlendirilebilir.

### Açık kalan hukuki ve bölgesel belirsizlikler

- Google, hangi kullanıcıya CHILD veya TEEN uygulanacağını kendisi belirlemiyor; hukuk danışmanına yönlendiriyor. Bu rapor hukuki görüş değildir.
- Türkiye'de KVKK, çocukların kişisel verileri için ayrı bir dijital rıza yaşı belirlemez; ergin olma yaşı (Türk Medeni Kanunu m. 11) 18'dir. 16-17 yaşındakilerin "çocuk" sayılıp sayılmayacağı bağlama göre değişebilir. Bu yüzden ihtiyatlı yorum seçildi: 18 altına reklam yok.
- Uygulama Türkiye dışında da indirilebilirse (ör. Avrupa Ekonomik Alanı), dijital rıza yaşı ülkeye göre 13-16 arasında değişir. 18 sınırı bu yaşların hepsinden yüksek olduğundan reklam tarafı için ihtiyatlıdır; ama rıza iletileri (UMP) AEA'da gerçek cihazda denenmedi.
- Tarafsız yaş ekranının Play incelemesinde yeterli sayılacağı garanti değildir; son karar Google'ındır.

## Veri

- Çalışma verileri yalnızca cihazda. Uygulamanın sunucusu, hesabı, analitik veya çökme raporu SDK'sı yok.
- İnternet yalnızca AdMob, UMP ve Play Billing için.
- Android bulut yedeği kapalı (`dataExtractionRules`).
- 2.1.1'de yeni izin veya SDK eklenmedi. Yeni saklanan tek bilgi doğum ayı ve yılıdır; yalnızca cihazda, yalnızca reklam kararı için.

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
- [AdMob reklam yerleşimi ilkeleri](https://support.google.com/admob/answer/6293636?hl=en)
- [Uygulama imzalama](https://developer.android.com/studio/publish/app-signing)
