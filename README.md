# YKS Pusula

Türkiye'deki üniversite sınavına hazırlanan öğrenciler için tamamen çevrimdışı çalışan Android uygulaması.
Hesap, sunucu, reklam ve takip yoktur. Uygulamanın internet izni bulunmaz.

Ayrıntılı sonuçlar: [TESLIM-RAPORU.md](TESLIM-RAPORU.md)

## Telefona kurulum

1. `yks-pusula-release.apk` dosyasını telefonunuza aktarın.
2. Dosyaya dokunun.
3. Android izin isterse, dosyayı açtığınız uygulama için **Bilinmeyen uygulamaları yükle** seçeneğini açın.
4. **Yükle** düğmesine basın ve uygulamayı açın.

Android 8.0 veya üzeri gerekir. İnternet gerekmez.

Play Store dışından kurulan her uygulama gibi, Android veya Play Protect bir uyarı gösterebilir.

## Güncelleme ve yedek

Yeni sürüm aynı imzayla kurulursa veriler korunur. Telefon değiştirirken veya uygulamayı silmeden önce
**Profil → Yedekleme ve geri yükleme** bölümünden yedek alın. Uygulama silinirse veriler de silinir.
Yedek dosyasına eklenen PDF kaynakları dahil değildir.

## Android Studio ile açma

Android Studio'da **Open** seçip bu klasörü açın. JDK 17 kullanılır. `app/signing` içindeki anahtar yalnızca
dışarıdan kurulum içindir; uygulama mağazada yayımlanacaksa yeni bir anahtar oluşturulmalıdır.

## Proje yapısı

| Klasör | İçerik |
|---|---|
| `app/src/main/java/app/ykspusula/data` | Room tabloları, veri işlemleri, yedekleme |
| `app/src/main/java/app/ykspusula/domain` | Net, tekrar, plan, öneri, arama ve deneme hesapları |
| `app/src/main/java/app/ykspusula/ui` | Compose ekranları |
| `app/src/main/java/app/ykspusula/notify` | Yerel bildirimler |
| `app/src/main/assets/content/base.json` | Uygulamaya gömülü içerik paketi |
| `content/src`, `tools/build_content.py` | İçerik kaynakları ve doğrulama |
| `app/src/test`, `app/src/androidTest` | Birim ve emülatör testleri |

İçeriği güncellemek için `python3 tools/build_content.py` komutu çalıştırılır.

## Derleme

`main` dalına yüklenen her değişiklik GitHub Actions ile derlenir. Commit mesajında `[emu]` varsa Android 8, 11
ve 13 emülatör testleri, `[a14]` varsa Android 14 testleri çalışır. Sonuçlar ve APK dosyaları `ci-results` dalına yazılır.
