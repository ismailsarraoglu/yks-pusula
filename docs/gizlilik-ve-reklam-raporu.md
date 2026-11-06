# Gizlilik ve reklam denetimi (2.1.0)

Tarih: 6 Kasım 2026. Kod ve politika metinleri karşılaştırıldı. Hukuki görüş değildir.

## Reklam

| Kontrol | Sonuç | Dayanak |
|---|---|---|
| Biçim | Yalnızca yerel (native) reklam; banner, geçiş ve ödüllü reklam yok | `monetize/Ads.kt`, `ui/components/AdSlot.kt` |
| Yerleşim | Yalnızca Ana sayfa, Çalış, Denemeler, İstatistik; içerik akışında bir kart olarak. Odaklanma, deneme sayacı, sonuç girişi, yedekleme ve satın alma ekranlarında yok | `AdPlacement` |
| Etiket | Kartta "Reklam" etiketi ve reklamveren bilgisi; uygulama içeriğinden ayrı görünür | `res/layout/ad_native.xml` |
| Yanlış tıklama | Reklamın üstünden başlayan kaydırma tıklama sayılmaz (2.0.0'da testte bulunan sorun). Üstüne şeffaf katman, sahte kapatma düğmesi veya yönlendirme yok | `monetize/AdTouchGuard.kt` |
| Kaydırma alanına bindirme | Reklam içeriğin üstünde yüzmez; sayfanın akışında yer alır | AdMob politikası: reklam içerik üstünde yüzmemeli |
| Sıklık | Aynı alan en erken 90 saniyede yeni reklam ister; ilk 3 açılışta reklam yok | `AdsConfig` |
| İçerik düzeyi | PG ile sınırlı (tüm kullanıcılar) | `RequestConfiguration` |
| Kimlik ayrımı | Debug ve yan yükleme derlemesi Google test kimlikleri; Play derlemesi test kimliğiyle derlenemez | `app/build.gradle.kts` |
| Reklamsız kullanıcı | Hak açıksa rıza istenmez, SDK başlatılmaz, reklam isteği yapılmaz | `AdsManager.onActivityStart`, test: `DataFlowTest.entitlementFlowAndAdsOff` |
| İnternet yok | Reklam alanı görünmez, uygulama çalışır | Uçak modu testleri |

## Rıza (UMP)

- Her açılışta rıza bilgisi güncellenir; gerekiyorsa form gösterilir. Reklam isteği yalnızca `canRequestAds()` doğruysa yapılır.
- Gizlilik seçenekleri gerekiyorsa Profil'de "Gizlilik seçenekleri" görünür.
- **Denenmedi:** Avrupa Ekonomik Alanı konumunda gerçek AdMob GDPR iletisi. AdMob panelinde ileti oluşturulmalı (`play/admob-talimat.md`).

## Satın alma

- Ürün: `remove_ads_lifetime`, tek seferlik, yalnızca reklamları kaldırır. Kod ve metinler bunu gerçek ürün olarak kullanır; eğitim özelliği satılmaz.
- Doğrulama cihazda: Play imzası, ürün kimliği, paket adı, satın alma durumu. Doğrulanmayan kayıt onaylanmaz ve hak açmaz. Açık anahtar yoksa satın alma kapalıdır.
- Bekleyen ödeme hak açmaz; tamamlandığında açılır. İptal veya hata durumunda açıklayıcı mesaj gösterilir. Açılışta ve öne gelişte satın almalar yeniden sorgulanır; "Satın almaları geri yükle" aynı sorguyu çalıştırır.
- **Güvenlik sınırı:** Sunucu olmadığı için doğrulama atlatılabilir (root'lu cihaz, değiştirilmiş APK). Tam sahtekârlık koruması yoktur.
- **Denenmedi:** Gerçek Play ile satın alma, iptal, bekleyen ödeme, geri yükleme. Play Console ürünü ve lisans test hesabı gerekir.

## Hedef kitle

Öneri: Play Console'da 16-17 ve 18+ hedef yaş grubu. Ayrıntı ve kaynak: `play/veri-guvenligi-envanteri.md`. Bu seçimle tarafsız yaş ekranı gerekmez; uygulamaya yaş ekranı eklenmedi. 13-15 eklenirse Families şartları yeniden değerlendirilmeli.

## Veri

- Çalışma verileri yalnızca cihazda. Uygulamanın sunucusu, hesabı, analitik veya çökme raporu SDK'sı yok.
- İnternet yalnızca AdMob, UMP ve Play Billing için.
- Android bulut yedeği kapalı (`dataExtractionRules`).
- 2.1.0'da yeni izin veya SDK eklenmedi.

## Gizlilik politikasında sizin doldurmanız gerekenler

`play/gizlilik-politikasi.html`:

1. `[GELİŞTİRİCİ ADI]`: Play Console'daki geliştirici adınız.
2. `[GELİŞTİRİCİ E-POSTA ADRESİ]`: Play Console'da doğruladığınız iletişim e-postası.

Sonra dosyayı herkese açık bir adreste yayımlayıp adresi Play Console'a girin.

## Kaynaklar

- [Hedef kitle ve uygulama içeriği ayarları](https://support.google.com/googleplay/android-developer/answer/9867159?hl=en)
- [AdMob reklam yerleşimi ilkeleri](https://support.google.com/admob/answer/6293636?hl=en)
- [Uygulama imzalama](https://developer.android.com/studio/publish/app-signing)
