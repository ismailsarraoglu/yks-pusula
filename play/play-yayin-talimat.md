# Google Play yayın adımları

## 1. İmza anahtarları

Kaynak: [Android geliştirici belgeleri, uygulama imzalama](https://developer.android.com/studio/publish/app-signing) (6 Kasım 2026'da okundu).

Uygulamada iki ayrı anahtar vardır. İkisini karıştırmayın.

| Anahtar | Ne işe yarar | Nerede |
|---|---|---|
| **Yükleme anahtarı** (sizin oluşturacağınız) | Google Play'e yüklenecek AAB'yi imzalar. Play, kullanıcıya giden APK'yı kendi uygulama imzalama anahtarıyla yeniden imzalar | Yalnızca sizde. Depoda yok |
| **Yan yükleme test anahtarı** (`yks-pusula-sideload.p12`) | Telefona doğrudan kurulan test APK'sını imzalar. 1.0 ve 2.0 test sürümleri bununla imzalıydı | 2.1.0'da depodan çıkarıldı; 7 Kasım 2026'da Git geçmişinden de silindi. Depo kısa süre herkese açıkken geçmişte göründüğü için **artık kullanılmaz** |

### 2.1.0'daki güvenlik değişikliği

- `release` derlemesi artık **yalnızca** yükleme anahtarı, gerçek AdMob kimlikleri ve Play açık anahtarı verildiğinde çalışır. Biri eksikse derleme hata verip neyin eksik olduğunu yazar; başka bir anahtara dönmez. Google test reklam kimliğiyle Play derlemesi üretilemez.
- Telefonda denemek için ayrı bir `sideload` derlemesi vardır: Google test reklamları kullanır, satın alma kapalıdır.
- Yan yükleme anahtarı ve parolası kaynak koddan çıkarıldı. **Dikkat:** anahtar 2.0.0'a kadar deponun geçmişinde duruyor. Depo gizli olduğu için risk sınırlıdır; yine de bu anahtarı Play için asla kullanmayın.

### Yükleme anahtarını oluşturma

Parolaları kimseyle paylaşmayın, sohbete yazmayın. Android Studio, anahtar için **en az 25 yıl** geçerlilik önerir.

- Android Studio: **Build > Generate Signed App Bundle/APK > Next > Create new**.
- Komut satırı (JDK kurulu bilgisayarda):

```
keytool -genkeypair -v -keystore yks-upload.jks -alias yks-upload -keyalg RSA -keysize 4096 -validity 10000
```

**Yedek.** `.jks` dosyasını ve parolaları iki ayrı güvenli yerde saklayın (örneğin şifreli bir parola yöneticisi ve bilgisayar dışında bir disk). Play Uygulama İmzalama açıkken kaybolan yükleme anahtarı Play Console'dan sıfırlatılabilir; uygulama imzalama anahtarı Google'da durduğu için güncellemeler devam eder. Sıfırlama Google onayı ister ve zaman alır.

### AAB üretme, yol A: GitHub üzerinden (Android Studio gerekmez)

Deponun **Settings > Secrets and variables > Actions** bölümüne şu gizli değerleri ekleyin:

| Gizli değer | İçerik |
|---|---|
| `UPLOAD_KEYSTORE_B64` | `.jks` dosyasının Base64 hâli (`base64 -w0 yks-upload.jks`; Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("yks-upload.jks"))`) |
| `UPLOAD_STORE_PASSWORD` | Anahtar deposu parolası |
| `UPLOAD_KEY_ALIAS` | `yks-upload` |
| `UPLOAD_KEY_PASSWORD` | Anahtar parolası |
| `ADMOB_APP_ID`, `ADMOB_NATIVE_ID` | AdMob panelinden (bkz. `admob-talimat.md`). `ca-app-pub-3940256099942544` ile başlayan Google test kimlikleri kabul edilmez |
| `PLAY_PUBLIC_KEY` | Play Console > Para kazanma kurulumu > Lisanslama (Base64 RSA açık anahtar). Bu olmadan satın alma doğrulanamaz ve reklamsız hak açılmaz |

Sonra **Actions > yayin-aab > Run workflow**. Bitince çalışmanın **Artifacts** bölümünden `yks-pusula-play-aab` dosyasını indirin (3 gün saklanır). İçinde AAB, aynı anahtarla imzalı APK ve kilitlenme raporlarını okunur yapan `mapping.txt` vardır.

### Yol B: kendi bilgisayarınızda

Proje kökünde `keystore.properties` oluşturun (Git'e eklenmez):

```
UPLOAD_STORE_FILE=/guvenli/yol/yks-upload.jks
UPLOAD_STORE_PASSWORD=...
UPLOAD_KEY_ALIAS=yks-upload
UPLOAD_KEY_PASSWORD=...
ADMOB_APP_ID=...
ADMOB_NATIVE_ID=...
PLAY_PUBLIC_KEY=...
```

`./gradlew bundleRelease` komutu AAB'yi yükleme anahtarıyla imzalar. Play Console'da **Play Uygulama İmzalama**yı açık tutun.

### Yan yükleme test anahtarı

Eski yan yükleme anahtarı ele geçmiş sayılır ve kullanılmaz; `SIDELOAD_P12_B64` gizli değerini eklemeyin. Test APK'sı geçici bir anahtarla imzalanır. Telefondaki eski test sürümünün üstüne kurulamaz: önce uygulamadan yedek alın, eski sürümü kaldırın, yeni APK'yı kurun, yedeği geri yükleyin. İleride kalıcı bir test anahtarı isterseniz yeni bir anahtar oluşturup yalnızca GitHub gizli değerlerinde saklayın.

Not: Play'den kurulan uygulama farklı imzalı olur. Dışarıdan kurulmuş sürüm Play sürümüyle güncellenemez. Önce yedek alın, kaldırın, Play'den kurun, yedeği geri yükleyin.

## 2. Reklamları kaldır ürünü

Play Console > **Para kazanma > Ürünler > Uygulama içi ürünler > Ürün oluştur**

- Ürün kimliği: `remove_ads_lifetime`
- Ad: Reklamları Kaldır — Ömür Boyu
- Tür: Tek seferlik, tüketilemeyen
- Fiyat: siz belirlersiniz, uygulama fiyatı Play'den alır
- Etkinleştir

Bu ürün gerçekten kullanılmak üzere tasarlandı: ücretsiz sürüm reklamlıdır, tek seferlik ödeme yalnızca reklamları kaldırır, hiçbir eğitim özelliği ücretli değildir. Ürünü oluşturmazsanız uygulama "Ürün bilgisi alınamadı" der ve reklamlı çalışmaya devam eder.

**Güvenlik sınırı:** Sunucu kullanılmadığı için satın alma cihazda doğrulanır: Google Play imzası, ürün kimliği, paket adı ve satın alma durumu denetlenir, başarılı satın alma onaylanır (acknowledge). Root'lu veya değiştirilmiş bir cihazda bu denetim atlatılabilir. Sunucu tarafı doğrulama olmadan tam sahtekârlık koruması yoktur.

Ürün yalnızca uygulamanın en az bir AAB'si kapalı veya dahili kanala yüklendikten sonra görünür. **Ayarlar > Lisans testi** bölümüne test hesaplarını ekleyin; gerçek para harcanmaz.

Denenecekler: fiyat, başarılı satın alma, iptal, bekleyen ödeme (test kartı "Yavaş test kartı"), başarısız ödeme, onay, yeniden açma, silip yeniden kurma, uçak modu.

## 3. Uygulama içeriği beyanları

- Gizlilik politikası: `gizlilik-politikasi.html` dosyasındaki `[GELİŞTİRİCİ ADI]` ve `[GELİŞTİRİCİ E-POSTA ADRESİ]` alanlarını doldurup herkese açık bir adreste yayımlayın.
- Reklamlar: Evet, uygulama reklam içeriyor.
- Reklam kimliği: Evet, amaç "Reklam veya pazarlama".
- Hedef kitle ve içerik: `veri-guvenligi-envanteri.md`.
- Veri güvenliği: aynı dosya.
- İçerik derecelendirmesi anketini doldurun.
- Kategori: Eğitim. Reklam içerir ve uygulama içi satın alma etiketleri kendiliğinden görünür.

## 4. Kanallar

1. **Dahili test:** AAB yükleyin, lisans testlerini yapın.
2. **Kapalı test:** 13 Kasım 2023'ten sonra açılan kişisel hesaplarda en az 12 test kullanıcısının 14 gün kesintisiz katılması gerekir. Geri bildirimleri not alın; üretim başvurusunda istenir.
3. **Üretim:** Pano > Üretime başvur. Önce kademeli yayın (%20) önerilir.

## 5. Sürüm numarası

Her yüklemede `app/build.gradle.kts` içindeki `versionCode` artırılır.
