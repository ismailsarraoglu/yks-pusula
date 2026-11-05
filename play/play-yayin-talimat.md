# Google Play yayın adımları

## 1. İmza anahtarı

Depodaki `app/signing/yks-pusula-sideload.p12` yalnızca dışarıdan kurulum içindir ve parolası kodda yazılıdır. Play için kullanmayın.

Yükleme anahtarını siz oluşturursunuz. Parolaları kimseyle paylaşmayın, sohbete yazmayın.

**Anahtar oluşturma.** İki yoldan birini kullanın:

- Android Studio: **Build > Generate Signed App Bundle > Create new**.
- Komut satırı (JDK kurulu bilgisayarda):

```
keytool -genkeypair -v -keystore yks-upload.jks -alias yks-upload -keyalg RSA -keysize 4096 -validity 10000
```

**Yedek.** `.jks` dosyasını ve parolaları iki ayrı güvenli yerde saklayın (örneğin şifreli bir parola yöneticisi ve bilgisayar dışında bir disk). Play Uygulama İmzalama açıkken kaybolan yükleme anahtarı Play Console'dan sıfırlanabilir, ama bu Google onayı ister ve zaman alır.

**AAB üretme, yol A: GitHub üzerinden (Android Studio gerekmez).** Deponun **Settings > Secrets and variables > Actions** bölümüne şu gizli değerleri ekleyin:

| Gizli değer | İçerik |
|---|---|
| `UPLOAD_KEYSTORE_B64` | `.jks` dosyasının Base64 hali (`base64 -w0 yks-upload.jks`; Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("yks-upload.jks"))`) |
| `UPLOAD_STORE_PASSWORD` | Anahtar deposu parolası |
| `UPLOAD_KEY_ALIAS` | `yks-upload` |
| `UPLOAD_KEY_PASSWORD` | Anahtar parolası |
| `ADMOB_APP_ID`, `ADMOB_NATIVE_ID` | AdMob panelinden (bkz. `admob-talimat.md`) |
| `PLAY_PUBLIC_KEY` | Play Console > Para kazanma > Lisanslama |

Sonra **Actions > yayin-aab > Run workflow**. Bitince çalışmanın **Artifacts** bölümünden `yks-pusula-play-aab` dosyasını indirin (3 gün saklanır). İçinde AAB ve kilitlenme raporlarını okunur yapan `mapping.txt` vardır. Eksik değer varsa iş, hangi değerin eksik olduğunu söyleyerek durur.

**Yol B: kendi bilgisayarınızda.** Proje kökünde `keystore.properties` oluşturun (depoya eklenmez):

```
UPLOAD_STORE_FILE=/guvenli/yol/yks-upload.jks
UPLOAD_STORE_PASSWORD=...
UPLOAD_KEY_ALIAS=yks-upload
UPLOAD_KEY_PASSWORD=...
```

`./gradlew bundleRelease` komutu AAB'yi yükleme anahtarıyla imzalar.

Play Console'da **Play Uygulama İmzalama**yı açık tutun. Bu teslimdeki AAB depodaki yan yükleme anahtarıyla imzalı ve test reklam kimlikleri içerir; Play'e yüklemeyin, yukarıdaki yollardan biriyle kendi anahtarınızla üretin.

Not: Play'den kurulan uygulama farklı imzalı olur. Şu an dışarıdan kurulu sürüm Play sürümüyle güncellenemez. Önce yedek alın, kaldırın, Play'den kurun, yedeği geri yükleyin.

## 2. Reklamları kaldır ürünü

Play Console > **Para kazanma > Ürünler > Uygulama içi ürünler > Ürün oluştur**

- Ürün kimliği: `remove_ads_lifetime`
- Ad: Reklamları Kaldır — Ömür Boyu
- Tür: Tek seferlik, tüketilemeyen
- Fiyat: siz belirlersiniz, uygulama fiyatı Play'den alır
- Etkinleştir

Ürün yalnızca uygulamanın en az bir AAB'si kapalı veya dahili kanala yüklendikten sonra görünür. **Ayarlar > Lisans testi** bölümüne test hesaplarını ekleyin; gerçek para harcanmaz.

Denenecekler: fiyat, başarılı satın alma, iptal, bekleyen ödeme (test kartı "Yavaş test kartı"), başarısız ödeme, onay, yeniden açma, silip yeniden kurma, uçak modu.

## 3. Uygulama içeriği beyanları

- Gizlilik politikası: `gizlilik-politikasi.html` dosyasındaki e-posta alanını doldurup herkese açık bir adreste yayımlayın.
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
