# YKS Pusula 2.1.1 teslim raporu

Sürüm 2.1.1 (versionCode 4). Kod: `43b566f`, dal `duzeltme-2.1.1`. Son tam test turu: `42363540765`, 7 Kasım 2026. 2.1.0 `main` dalında (`08a4f6b`) olduğu gibi duruyor. 2.1.0 teslim raporu: `docs/teslim-raporu-2.1.0.md`.

Bu bir düzeltme sürümü: yeni özellik eklenmedi. Ek talimatla iki küçük değişiklik yapıldı: profil ekranında YDT dili ve bilgi merkezinde ÖSYM bağlantısı.

**Google Play'e yüklenecek imzalı AAB bu teslimde yok.** Yükleme anahtarınız olmadan üretilemez; sahte anahtarla da üretilmedi.

## 1. İstenen beş düzeltme

| # | İstek | Durum | Kanıt |
|---|---|---|---|
| 1 | Test otomasyonu gerçek kalite kapısı | Tamamlandı | `continue-on-error` yok; her aşama PASS/FAIL/NOT RUN; 3 kasıtlı hata denemesinin üçünde de CI başarısız oldu (`docs/test-raporu.md`) |
| 2 | Android 15 testi | Tamamlandı (emülatör) | 35/35 aşama PASS; iki gezinme biçimi, ekran kesiği, seçiciler, bildirim, yaşam döngüsü (`docs/android15-test-raporu.md`) |
| 3 | Reklam ve yaş uygunluğunun yeniden değerlendirilmesi | Tamamlandı, hukuki belirsizlikler raporda | Tarafsız yaş ekranı; reklam yalnızca doğrulanmış 18+ kullanıcıya (`docs/gizlilik-ve-reklam-raporu.md`) |
| 4 | Satın alma doğrulaması fail-closed | Tamamlandı (gerçek Play ile denenmedi) | Paket adı, durum, ürün ve belirteç zorunlu; 29 birim testi |
| 5 | Güvenli yedekleme ve geri yükleme | Tamamlandı | Günlüklü dosya değişimi, ayarlar dahil tam geri dönüş, yarım işlemin kurtarılması; 7 yeni test |

Ayrıntılı değişiklik listesi: `docs/degisiklik-raporu-2.1.1.md`.

## 2. Reklam ve yaş kararı (özet)

- Google Play'e göre 16-17 ve 13-15 grupları bazı bölgelerde çocuk sayılabilir. YKS kitlesi karma kitledir; 2.1.0'daki "yaş ekranı gerekmez" değerlendirmesi geri çekildi.
- Uygulamanın reklam SDK'sı (GMA Next-Gen) Families sertifikalı listede değil. Bu yüzden reklam yalnızca tarafsız yaş ekranında 18 yaşını doldurduğu belirlenen kullanıcıya gösteriliyor. Diğer herkes için reklam ve rıza SDK'sı hiç başlatılmıyor.
- Yetişkin isteklerinde `AgeRestrictedTreatment.UNSPECIFIED`, ayrıca PG içerik sınırı.
- **Gelir etkisi:** 18 yaş altı reklam görmez. Alternatif (sertifikalı `play-services-ads` SDK'sına geçiş) bu sürümde yapılmadı.
- **Hukuki belirsizlik:** Google yaş işlemi seçimi için hukuk danışmanı öneriyor; KVKK'da ayrı bir dijital rıza yaşı yok; AEA'da ülkeye göre değişiyor. Bu rapor hukuki görüş değildir.

## 3. Son tam tur

| İş | Sonuç |
|---|---|
| Derleme, birim testleri (76), release denetimi, manifest, imza | PASS |
| Android 8, 11, 13, 14, 15, 16 | PASS |
| Android 14 ve 16 Play imajı (çevrimiçi reklam, kaydırma koruması, 6/6 temiz kurulum) | PASS |

Uygulama çökmesi 0, ANR 0. Ayrıntı: `docs/test-raporu.md`.

## 4. Ek talimatlar

| Talimat | Durum | Dosya | Test |
|---|---|---|---|
| Profilde YDT dili yalnızca Dil alanında | Tamamlandı | `ui/screens/ProfileScreens.kt` | `UiFlowTest.profileEditShowsYdtLanguageOnlyForDil`: Android 11, 14, 15, 16'da PASS |
| ÖSYM kartı soru ve cevap arşivini tarayıcıda açar | Tamamlandı (sayfa içeriği denetlenmedi) | `ui/screens/ExtraScreens.kt` | `osymLinkOfflineShowsWarning` ve `osymLinkOpensBrowser`: Android 15'te PASS. Chrome öne geldi, istek `https://www.osym.gov.tr/sinav-soru-ve-cevaplari` adresini taşıyor |

## 5. Depo güvenliği

GitHub Actions'ın ücretsiz dakikaları bittiği için depo testler süresince herkese açık yapıldı. Önce bütün Git geçmişinden eski yan yükleme test anahtarı ve parolası silindi; GitHub'daki bütün dallar yeniden tarandı, hiçbir anahtar veya parola bulunmadı.

Depo birkaç dakika temizlenmeden açık kaldığı için **eski test anahtarı (`yks-pusula-sideload-YEDEK.p12`) artık kullanılmaz.** Play yükleme anahtarı depoda hiç olmadı; Play sürümü etkilenmez. Kayıt kimlikleri değişti: 2.0.0 `d6ef13c`, 2.1.0 `08a4f6b`. Eski geçmişin tam yedeği size ayrıca gönderildi (`yks-pusula-eski-gecmis-YEDEK.bundle`; içinde eski anahtar var, paylaşmayın).

**Testler bitti; depoyu yeniden gizli yapabilirsiniz:** Settings > General > Danger Zone > Change visibility > Private.

## 6. Tamamlanmayan testler (açıkça)

**Gerçek cihaz**
- Hiçbir test gerçek telefonda yapılmadı. Android 9, 10 ve 12 hiç denenmedi.
- Gerçek ana ekrana bileşen ekleme, gerçek güç kaybında yarım kalan geri yükleme, gerçek 2.1.0 kurulumunun üstüne 2.1.1.
- ÖSYM sayfasının içeriği (yıl seçimi, TYT/AYT/YDT kitapçıkları, cevap anahtarları).

**AdMob**
- Üretim reklam kimlikleri ve gerçek reklam: kimlikler yok, denenmedi.
- Avrupa Ekonomik Alanı rıza formu: denenmedi.
- Yalnızca Google test kimlikleri kullanıldı; hiçbir reklama tıklanmadı.

**Play satın alma**
- Gerçek Play Billing bağlantısı kurulamadı: emülatörde Google hesabı yok.
- Fiyat, satın alma, iptal, bekleyen ödeme, yeniden kurulumda geri yükleme: hiçbiri denenmedi. **Para harcanmadı.**
- Lisans test hesabıyla dahili test gerekir.

Liste ve adımlar: `docs/kalan-manuel-isler.md`.

## 7. Teslim dosyaları

| Dosya | Açıklama |
|---|---|
| `yks-pusula-2.1.1-test.apk` | Telefona kurulabilir test sürümü (release ayarlı, Google test reklamları, satın alma kapalı). Geçici (debug) anahtarla imzalı; eski test sürümünün üstüne kurulmaz, önce yedek alıp kaldırın |
| `yks-pusula-kaynak-2.1.1.zip` | Kaynak kod. `.jks`, `.p12`, `.keystore`, parola, `local.properties`, `.env`, derleme çıktısı içermez |
| Bu rapor ve `docs/` | Değişiklik, test, Android 15, gizlilik ve reklam raporları |
