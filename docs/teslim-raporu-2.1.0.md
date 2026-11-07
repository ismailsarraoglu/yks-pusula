# YKS Pusula 2.1.0 teslim raporu

Sürüm 2.1.0 (versionCode 3). Uygulama kodu: `83130c3`, tam test turu `42304857639`, 6 Kasım 2026. Çalışma dalı `surum-2.1`; 2.0.0 hâli Git geçmişinde `d6ef13c` kaydında duruyor.

Durum etiketleri: **Tamamlandı** · **Kısmen tamamlandı** · **Test edilmesi gerekiyor** · **Kullanıcı işlemi gerekiyor** · **Bu sürüme alınmadı**

**Google Play'e yüklenecek imzalı AAB bu teslimde yok.** Yükleme anahtarınız ortamda olmadığı için üretilemedi; sahte anahtarla da üretilmedi. Derleme anahtar olmadan durur. Kendi anahtarınızla üretme adımları: `play/play-yayin-talimat.md`.

## 1. Rakip araştırması sonucu

Ayrıntı: `docs/COMPETITOR_RESEARCH.md` (6 Kasım 2026).

- 9 rakip ve 2 ek uygulama incelendi. **Google Play sayfaları bu tarihte okunamadı** (araştırma aracı ilgisiz sayfalar getirdi). İndirme ve güncelleme bilgisi AppGoblin'in tahminlerinden, puan ve yorumlar App Store'dan alındı. Yorum incelemesi her uygulamada en fazla 4 görünen yorumla sınırlı.
- Ortak özellikler (sayaç, konu takibi, deneme kaydı, Pomodoro, bileşen, seri) artık fark yaratmıyor.
- Rakiplerin ücretli kısmı genelde yapay zekâ koç, sıralama tahmini veya içerik. Çoğu haftalık, aylık veya yıllık abonelik kullanıyor.
- YKS Pusula'nın farkı: gerekçeli plan, denemeden yanlış defterine bağlantı, gerçek süre ile görev ayrımı, kaynak bağımsız takip, cihazda kalan veri ve güvenilir yedek, tek seferlik reklamsız ödeme.
- Görünen yorumlarda kullanıcıların en çok şikâyet ettiği konular: hatalı cevap anahtarları, YDT desteği eksikliği, plan görevlerinin işaretlenememesi. Bu yüzden doğrulanmamış soru eklenmedi, YDT desteği dürüstçe belirtildi, plan işaretleme testlerle korunuyor.

## 2. Eski projedeki problemler

Kod üzerinde yeniden doğrulandı (`docs/denetim-2.1.md`). Prompttaki eski bulguların hepsi kodda hâlâ vardı:

- Release derlemesi anahtar yoksa yan yükleme anahtarına dönüyordu; anahtar ve parolası depodaydı; release test reklam kimliğine dönüyordu.
- Play açık anahtarı boşken satın alma imzasız geçerli sayılıyordu. Ürün kimliği metin içinde aranıyordu.
- Plan 240 dk hedefe 200 dk, 30 dk hedefe 50 dk koyuyordu.
- Ana sayfa süreyi `maxOf(gerçek, görev tahmini)` ile gösteriyordu.
- Ekranlar tarihi bir kez alıyordu; gece açık kalan uygulama dünü gösteriyordu. Saat dilimi değişince hatırlatmalar yeniden kurulmuyordu.
- Yedek: sınırsız girdi okuma, ZIP dosya sayısı ve toplam boyut sınırı yok, eklerin bütünlüğü yok, güvenlik kopyası fotoğraf ve PDF'leri içermiyordu, eklerin kopyalanması başarısız olursa geri dönüş yoktu.
- Denemedeki zorlanılan konular yanlış defterine girmiyordu.
- İçerik: 25 boş anlatım, 204 anlatım 150 karakterden kısa, İngilizce dışındaki YDT dillerinde not yok.

## 3. Düzeltilen problemler

| Problem | Durum |
|---|---|
| Release imzası yalnızca açıkça verilmiş yükleme anahtarıyla; eksikse derleme durur ve neyin eksik olduğunu yazar | **Tamamlandı** (CI'da doğrulandı) |
| Yan yükleme anahtarı ve parolası depodan ve kaynak ZIP'inden çıkarıldı, yedeği gönderildi | **Tamamlandı**. Not: anahtar eski Git geçmişinde duruyor |
| Test ve gerçek reklam kimlikleri ayrıldı; Play derlemesi test kimliğini reddeder | **Tamamlandı** |
| Satın alma doğrulaması: anahtar yoksa hak yok; imza, ürün, paket, durum denetimi | **Tamamlandı** (birim ve cihaz testi); gerçek Play ile **Test edilmesi gerekiyor** |
| Plan süresi hedefe tam eşit, molalar ayrı, saat yetmezse açıklama | **Tamamlandı** (8 süre senaryosu ve 9 ek senaryo) |
| Ana sayfada gerçek süre, görev, soru, haftalık hedef, tekrar ayrı | **Tamamlandı** |
| Gün değişimi, öne gelme, saat ve saat dilimi değişikliği | **Kısmen tamamlandı**: kod ve gün yenileme testi var; gece yarısını ve saat dilimi değişikliğini gerçek cihazda bekleyerek denemek **Test edilmesi gerekiyor** |
| Yedek sınırları, yol aşımı, ZIP bombası, ek bütünlüğü, tam güvenlik kopyası, geri dönüş | **Tamamlandı** (14 cihaz testi); Android dosya seçicisiyle **Test edilmesi gerekiyor** |
| Denemedeki zorlanılan konuların yanlış defterine girmemesi | **Tamamlandı** (yeni özellik, aşağıda) |

## 4. Yeni eklenen özellikler

| Özellik | Durum |
|---|---|
| Denemedeki yanlış ve boş konuları yanlış defterine ve tekrara bağlama | **Tamamlandı** |
| Öncelikli konular (eksik konu sıralaması, gerekçeli) | **Tamamlandı** |
| Sınav öncesi modu | **Tamamlandı** |
| Akıllı plan gerekçeleri (deneme yanlışı, uzun süredir tekrar edilmeyen, hedef net farkı), haftalık hedef, dinlenme günü, aynı gün aynı konu yok | **Tamamlandı** |
| Plan ayarlarında blok süresi, güçlü dersler (alana göre), saat uyarısı | **Tamamlandı** |
| Yanlış defterinde yanlış, boş, tahminle doğru; öğrenme düzeyi; "Bilgiyi karıştırma" nedeni | **Tamamlandı** |
| Kartlarda karışık hızlı tekrar | **Tamamlandı** |
| Odaklanmada "Oturumdan vazgeç" | **Tamamlandı** |
| 12 TYT Matematik konusunda genişletilmiş not | **Kısmen tamamlandı**: 261 konunun 12'si. Diğerleri kısa özet olarak kalıyor ve uygulama bunu söylüyor |
| İçerik paketinde doğrulama bilgisi | **Tamamlandı**. İçerik resmî kaynaklarla satır satır karşılaştırılmadı; uygulama "doğrulama yapılmadı" der |
| YDT dil desteğinin açıkça belirtilmesi | **Tamamlandı** |
| Soru bankası, mini testler, sıralama tahmini, düello, bulut | **Bu sürüme alınmadı** (`docs/gelecek-fikirleri.md`) |
| Şifreli yedek | **Bu sürüme alınmadı** |
| Hedef üniversite ve bölüm | Önceden vardı, değişmedi. Taban puan verisi gömülü değil |

Kullanım: `docs/yeni-ozellikler-kullanim.md`.

## 5. Değiştirilen dosyalar

Toplam 57 dosya (görseller hariç); 2.0.0'a göre ayrıntılı fark Git'te `d6ef13c..surum-2.1`.

| Alan | Dosyalar |
|---|---|
| Derleme ve imza | `app/build.gradle.kts`, `.gitignore`, `.github/workflows/derleme.yml`, `.github/workflows/yayin.yml`; `app/signing/yks-pusula-sideload.p12` silindi |
| Satın alma | `monetize/Entitlement.kt`, `monetize/Billing.kt`, `ui/screens/ExtraScreens.kt` |
| Plan ve analiz | `domain/Logic.kt` (plan motoru, öncelik sırası, deneme eksikleri), `data/Repository.kt` |
| Yedek | `data/Backup.kt`, `ui/screens/ProfileScreens.kt` |
| Veritabanı | `data/Entities.kt`, `data/Migrations.kt`, `data/AppDatabase.kt`, `app/schemas/.../3.json`, `data/ContentPack.kt` |
| Gün ve bildirim | `YksApp.kt`, `MainActivity.kt`, `notify/Notifier.kt`, `AndroidManifest.xml`, `ui/AppRoot.kt` |
| Ekranlar | `HomeScreens.kt`, `StudyScreens.kt`, `ExamScreens.kt`, `PlanScreens.kt`, `LibraryScreens.kt`, `CoachScreens.kt`, `StatsScreens.kt`, `ProfileScreens.kt`, `ExtraScreens.kt` |
| İçerik | `assets/content/base.json` (12 genişletilmiş not, doğrulama bilgisi) |
| Testler | Yeni: `PlanDurationTest`, `AnalysisLogicTest`, `BackupSecurityTest`, `LearningFlowTest`, `TestPurchase`. Güncellenen: `LogicTest`, `ProductLogicTest`, `DataFlowTest`, `MigrationTest`, `AdOnlineTest`, `UiFlowTest`, `tools/*.sh` |
| Belgeler | `docs/COMPETITOR_RESEARCH.md`, `denetim-2.1.md`, `degisiklik-raporu.md`, `test-raporu.md`, `gizlilik-ve-reklam-raporu.md`, `yeni-ozellikler-kullanim.md`, `surum-notlari.md`, `gelecek-fikirleri.md`, `kalan-manuel-isler.md`; `play/` altındaki gizlilik politikası, veri güvenliği, mağaza metinleri, yayın talimatı, görseller |

## 6. Veritabanı ve veri güvenliği

- Room şema 2 → 3, yalnızca sütun ekleme. Yıkıcı geçiş kullanılmadı. Geçiş testleri: 1 → 2, 2 → 3, 1 → 3.
- Yedek biçimi sürüm 3; sürüm 1 ve 2 yedekleri okunur.
- Geri yükleme aşamalı: doğrula, geçici alana aç, dosyaları denetle, tam güvenlik kopyası al ve doğrula, veritabanını tek işlemde yaz, dosyaları taşı, sayıları denetle, geçici dosyaları sil. Dosya taşıma başarısız olursa güvenlik kopyasından eski hâle dönülür.
- Bilinen sınır: PDF kitaplığı 1 GB'ı aşarsa güvenlik kopyası doğrulanamaz ve değiştirerek geri yükleme yapılmaz (birleştirme yapılabilir).

## 7. Reklam ve satın alma durumu

Ayrıntı: `docs/gizlilik-ve-reklam-raporu.md`.

- Reklamlar: Google test kimlikleriyle Android 14 ve 16 Play imajlarında ilk denemede göründü. Gerçek AdMob kimliği yok: **Kullanıcı işlemi gerekiyor**.
- Satın alma (`remove_ads_lifetime`): kod tamam, imzalı test kayıtlarıyla cihazda denendi. Gerçek Play ile: **Kullanıcı işlemi gerekiyor** (Play ürünü ve lisans test hesabı).
- Güvenlik sınırı: sunucu olmadığı için doğrulama cihazdadır, atlatılabilir.

## 8. Gizlilik ve Play Store hazırlığı

| Madde | Durum |
|---|---|
| Gizlilik politikası güncellendi | **Kullanıcı işlemi gerekiyor**: `[GELİŞTİRİCİ ADI]` ve `[GELİŞTİRİCİ E-POSTA ADRESİ]` |
| Veri güvenliği envanteri | **Tamamlandı**; beyan Play Console'da doldurulacak |
| Hedef kitle önerisi (16-17 ve 18+) | **Kullanıcı işlemi gerekiyor** |
| Mağaza metinleri, görseller | **Tamamlandı**; görseller 2.1.0 ekranlarından yeniden üretildi |
| Hedef API 36, R8 | **Tamamlandı** |

## 9. Çalıştırılan testler

Ayrıntı: `docs/test-raporu.md`. Hepsi son turda (`83130c3`) geçti:

- Birim testleri 70/70; bunların 19'u yeni plan süresi senaryoları.
- Android 11 tam cihaz testleri 54/54; düğmeler 5/5; stres 3/3; 467 ekran görüntüsü; veri kalıcılığı.
- Android 8 ve 13: 46/46. Android 14 ve 16: 52/52. Hepsi ilk denemede.
- Geçiş testleri 1 → 2, 2 → 3, 1 → 3.
- Yedek güvenliği: yol aşımı, ZIP bombası, dosya sayısı, toplam boyut, büyük girdi, büyük JSON, aynı adlı giriş, bozuk ve eksik ek, bozuk JSON, eski biçim, eksik fotoğraf, güvenlik kopyası ve geri dönüş, geçici dosya temizliği (14 test).
- Google test reklamı Android 14 ve 16 Play imajlarında; reklamsız hak açılınca reklam kalkıyor.
- Yan yükleme sürümü 5 Android sürümünde kuruldu ve açıldı; çökme satırı 0, ANR 0. Soğuk açılış 1,0-1,3 sn, bellek 71 MB.
- Play derlemesi anahtarsız denendi: beklendiği gibi durdu.

## 10. Başarısız veya yapılamayan testler

Son turda başarısız test yok. Önceki iki turda bulunan test hataları ve bir mesaj sorunu düzeltildi (`docs/test-raporu.md`).

Yapılamayanlar (**Test edilmesi gerekiyor** veya **Kullanıcı işlemi gerekiyor**):

- Gerçek telefon; Android 9, 10, 12, 15.
- Gerçek Google Play satın alma, iptal, bekleyen ödeme, geri yükleme. **Para harcanmadı.**
- Gerçek AdMob kimlikleriyle reklam; Avrupa rıza formu.
- Android dosya seçicisiyle yedek, fotoğraf ve PDF; ana ekrana bileşen ekleme; bildirimin saatinde gelmesi; bildirim izni reddi; saat dilimi değişikliği; gerçek gece yarısı geçişi.
- Telefondaki gerçek 2.0.0 kurulumunun üstüne 2.1.0 (veritabanı geçişi testte doğrulandı).
- `yayin-aab` işi (anahtarınız gerekiyor).

## 11. Bilinen sorunlar

- 261 konunun büyük kısmında not kısa özettir; ders anlatımı yerine geçmez.
- İngilizce dışındaki YDT dillerinde not ve kelime kartı yok.
- Yan yükleme anahtarı eski Git geçmişinde duruyor (depo gizli). Play için kullanılmamalı.
- Test APK'sı GitHub'a yan yükleme anahtarı eklenmediği için geçici anahtarla imzalı; telefondaki 2.0.0'ın üstüne kurulmaz (yedek al, kaldır, kur, geri yükle).
- `TestPurchase.kt` içindeki RSA anahtarı yalnızca otomatik testler içindir; Play derlemesi bu anahtarı kabul etmez.
- Android 14 Play imajında 1 kez Google Play Hizmetleri yeniden başlarken Android uygulamayı da kapattı. Uygulama hatası değil; gerçek telefonda da nadiren olabilir, kayıtlı veri kaybolmaz.
- Ana sayfadaki haftalık hedef yüzdesi ve alana göre ders listesinin ayrı otomatik testi yok; ekran taramasıyla görüldü.

## 12. Yayın için gereken kullanıcı işlemleri

Sırasıyla `docs/kalan-manuel-isler.md`:

1. Google Play geliştirici hesabı.
2. Yükleme anahtarı oluşturup iki yerde yedekleme.
3. AdMob uygulaması, yerel reklam birimi ve GDPR iletisi.
4. Gizlilik politikasındaki iki alanı doldurup yayımlama.
5. `remove_ads_lifetime` ürününü oluşturma; Play açık anahtarını alma.
6. Yedi gizli değerle `yayin-aab` işini çalıştırıp AAB üretme.
7. Beyanlar (veri güvenliği, reklam kimliği, hedef kitle, içerik derecelendirmesi).
8. Dahili testte lisans test hesabıyla satın alma denemeleri (gerçek para harcanmaz).
9. 12 kişiyle 14 gün kapalı test.

## 13. APK ve AAB dosyalarının konumu

| Dosya | Açıklama |
|---|---|
| `yks-pusula-2.1.0-test.apk` | Telefona kurulabilir test sürümü. Release ile aynı küçültme ayarları, Google test reklamları, satın alma kapalı. GitHub'a yan yükleme anahtarı eklenmediği için geçici (debug) anahtarla imzalı |
| Play AAB | **Bu teslimde yok.** Yükleme anahtarınızla `yayin-aab` işinden üretilir |
| `yks-pusula-kaynak-2.1.0.zip` | Kaynak kod. `.jks`, `.p12`, `.keystore`, parola, `local.properties`, derleme çıktısı içermez |
| `yks-pusula-sideload-YEDEK.p12` | Eski yan yükleme test anahtarının yedeği. Play için kullanmayın, paylaşmayın |
| `yks-pusula-play-paketi.zip` | Mağaza metinleri, görseller, gizlilik politikası, veri güvenliği envanteri, yayın talimatları |
| `yks-pusula-raporlar.zip` | Bu rapor ve `docs/` altındaki tüm raporlar |
