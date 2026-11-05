# Google Play yayın adımları

## 1. İmza anahtarı

Depodaki `app/signing/yks-pusula-sideload.p12` yalnızca dışarıdan kurulum içindir ve parolası kodda yazılıdır. Play için kullanmayın.

Android Studio > **Build > Generate Signed App Bundle > Create new** ile yükleme anahtarı oluşturun. Dosyayı ve parolaları proje dışında, güvenli iki ayrı yerde yedekleyin. Sonra `keystore.properties` dosyasına ekleyin:

```
UPLOAD_STORE_FILE=/guvenli/yol/yks-upload.jks
UPLOAD_STORE_PASSWORD=...
UPLOAD_KEY_ALIAS=...
UPLOAD_KEY_PASSWORD=...
```

Bu dosyayla `./gradlew bundleRelease` çalıştırıldığında AAB yükleme anahtarıyla imzalanır. Play Console'da **Play Uygulama İmzalama**yı açık tutun. Teslim edilen AAB depodaki anahtarla imzalıdır; ilk yüklemeden önce kendi anahtarınızla yeniden derleyin, aksi hâlde o anahtar yükleme anahtarınız olur.

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
