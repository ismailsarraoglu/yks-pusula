# Değişiklik raporu: 2.0.0 → 2.1.0

Ayrıntılı başlangıç denetimi: `docs/denetim-2.1.md`.

## Düzeltilen sorunlar

| Sorun | Düzeltme | Dosya |
|---|---|---|
| Play derlemesi yükleme anahtarı yoksa yan yükleme anahtarına dönüyordu | `release` yalnızca yükleme anahtarı, gerçek AdMob kimlikleri ve Play açık anahtarıyla derlenir; eksikse neyin eksik olduğunu yazıp durur. Telefonda deneme için ayrı `sideload` derlemesi | `app/build.gradle.kts`, `.github/workflows/*` |
| Yan yükleme anahtarı ve parolası depoda | Depodan çıkarıldı, `.gitignore` genişletildi; CI anahtarı isteğe bağlı GitHub gizli değerinden alır | `app/signing/` (silindi), `.gitignore` |
| Release derlemesi gerçek reklam kimliği yoksa Google test kimliğine dönüyordu | Debug ve sideload açıkça test kimliği kullanır; Play derlemesi test kimliğini reddeder | `app/build.gradle.kts` |
| Play açık anahtarı boşken satın alma imzasız geçerli sayılıyordu | Anahtar yoksa hak hiçbir zaman açılmaz; imza, ürün kimliği (`productId` çözümlenerek), paket adı ve satın alma durumu denetlenir; doğrulanmayan kayıt onaylanmaz | `monetize/Entitlement.kt`, `monetize/Billing.kt` |
| Plan 240 dk hedefe 200 dk, 30 dk hedefe 50 dk koyuyordu | Bloklar hedefe tam eşit bölünür, son blok kalan süredir, molalar ayrı tutulur, saat penceresi yetmezse neden yazılır | `domain/Logic.kt` `PlanGenerator` |
| Yeniden plan, tamamlanan görevlerin üstüne tam günlük hedef ekliyordu | Tamamlanan ve elle eklenen görevlerin süresi günlük hedeften düşülür | `data/Repository.kt` `planInput` |
| Ana sayfa süreyi `maxOf(gerçek, tamamlanan görev)` ile gösteriyordu | Yalnızca gerçek süre; görev, soru, haftalık hedef ve tekrar sayısı ayrı satırlarda | `ui/screens/HomeScreens.kt` |
| Gece açık kalan uygulama dünü gösteriyordu | Uygulama genelinde gün durumu; gece yarısı, öne gelme, saat ve saat dilimi değişiminde yenilenir (7 ekran) | `YksApp.kt`, `MainActivity.kt`, `ui/AppRoot.kt` |
| Saat veya saat dilimi değişince hatırlatmalar eski saate göre kalıyordu | `TIME_SET` ve `TIMEZONE_CHANGED` yayınlarında yeniden kurulur | `AndroidManifest.xml`, `notify/Notifier.kt` |
| Gelen yedek sınırsız okunuyordu; ZIP'te dosya sayısı ve toplam boyut sınırı yoktu | Girdi 700 MB, JSON 32 MB, tek dosya 150 MB, açılmış toplam 1 GB, 5.000 dosya sınırı; aynı adlı giriş ve yol aşımı reddi | `data/Backup.kt` |
| Eklerin bütünlüğü denetlenmiyordu | Yedek sürüm 3: her fotoğraf ve PDF'in boyutu ve SHA-256 özeti yedekte; uyuşmazsa yedek reddedilir. Eski yedekler uyarıyla kabul edilir | `data/Backup.kt` |
| Güvenlik kopyası yalnızca veritabanıydı; ekler kopyalanırken hata olursa geri dönüş yoktu | Tam ZIP güvenlik kopyası (fotoğraf ve PDF dahil), yazıldıktan sonra doğrulanır; aşamalı geri yükleme, hata olursa güvenlik kopyasından geri dönüş; kaç kayıt ve dosya aktarıldığı gösterilir | `data/Backup.kt`, `ui/screens/ProfileScreens.kt` |

## Yeni eklenenler

- **Denemeden yanlış defterine:** Sonuç girerken yanlış ve boş yapılan konular sayılarıyla seçilir; her biri yanlış defterine (tür: yanlış veya boş) ve tekrar takvimine girer. Sonuç düzeltilirse sayılar güncellenir, tekrar aşaması korunur; deneme silinirse bu kayıtlar da silinir.
- **Öncelikli konular:** Deneme yanlışları, açık yanlışlar, tekrar tarihi, doğru oranı, hedef net farkı ve konu seviyesine göre "önce hangi konu" sıralaması, her konunun gerekçesiyle.
- **Sınav öncesi modu:** Sınava 21 gün kala plan yeni konu yerine tekrar ve eksiklere ağırlık verir; ana sayfada ve öncelik ekranında belirtilir.
- **Akıllı plan gerekçeleri:** "Son denemelerde bu konudan 3 yanlış var", "Bu konuyu 20 gündür çalışmadın", "Bu derste son netin 4, hedefin 10" gibi açıklamalar. Aynı gün aynı konu verilmez; haftalık hedef ve dinlenme günleri uygulanır.
- **Plan ayarları:** Tercih edilen blok süresi, güçlü dersler (alana göre TYT, AYT ve YDT dersleri), saat penceresi yetmezse önceden uyarı. Plan ekranında gün başına çalışma ve mola ayrı gösterilir.
- **Yanlış defteri:** Tür seçimi (yanlış, boş, tahminle doğru), "Bilgiyi karıştırma" nedeni, öğrenme düzeyi ve soru sayısı gösterimi.
- **Kartlar:** Karışık hızlı tekrar (deste veya tüm desteler, rastgele 30 kart).
- **Odaklanma:** "Oturumdan vazgeç" seçeneği; süre kaydedilmez.
- **İçerik:** 12 TYT Matematik konusunda notlar tanım, temel bilgiler, dikkat noktaları, sık yapılan hata, çözümlü örnek ve cevaplı kontrol sorularıyla genişletildi. İçerik paketine doğrulama alanı eklendi; doğrulama yapılmadığı açıkça yazar. Not olmayan konularda bunu söyler; YDT'de İngilizce dışındaki dillerde yalnızca konu listesi olduğu belirtilir.
- **Gizlilik politikası:** Üçüncü taraf bileşenler, internet kullanımı, dışa aktarma ve yaş bölümleri genişletildi.

## Veritabanı

Room şema 2 → 3. Yalnızca sütun eklendi, hiçbir tablo silinmedi:

- `UserProfile.blockMinutes` (varsayılan 50)
- `WrongEntry.kind` (varsayılan `WRONG`), `WrongEntry.mockExamId` (boş olabilir) ve dizini
- `ImportedContentPack.verifiedAt`, `ImportedContentPack.verification` (varsayılan boş)

Geçiş testleri: 2 → 3 ve 1 → 3 zinciri. Yedek dosyası sürüm 3; sürüm 1 ve 2 yedekleri okunmaya devam eder.

## Sürüm

`versionCode` 3, `versionName` 2.1.0. `applicationId` değişmedi (`app.ykspusula`).

---

# Önceki sürüm: 1.0.0 → 2.0.0

## Korunanlar

Profil ve sınav hedefi, 26 ders ve 261 konu, 236 formül, 411 bilgi kartı, çalışma kronometresi, Pomodoro, plan, notlar, PDF kitaplığı, üniversite ve bölüm hedefleri, net hesaplama, OBP, istatistikler, rozetler, seri takibi, yerel bildirimler, yedekleme, açık tema, içerik paketi yükleme, arama.

## Geliştirilenler

- **Konu takibi:** 5 seviyeli ustalık. Tek işaretle "güçlü" olunmaz; süresi geçen tekrar seviyeyi düşürür.
- **Plan:** Kapasite, bitiş saati, dinlenme günü, görev önceliği ve gerekçe.
- **Denemeler:** Soru çözdürme yerine dışarıda çözülen denemenin süre tutma, kayıt, analiz, geçmiş filtreleri ve karşılaştırması. OBP yıla göre veri ile hesaplanır.
- **Yanlış defteri:** Soru bankasından bağımsız; fotoğraf (sistem seçicisi), neden listesi, Hatırladım / Kısmen / Hatırlayamadım, tekrar geçmişi.
- **Pomodoro:** Yeniden başlatmaya dayanır, planlanan ve gerçek süreyi ayırır, bitişte performans girişi alır, reklam içermez.
- **Yedekleme:** ZIP biçimi; fotoğraflar ve isteğe bağlı PDF'ler dahil, sağlama toplamı, geri yükleme öncesi güvenlik kopyası. Eski yedekler okunur.
- **Bildirimler:** Sekiz tür, saat, kapatma, sıklık; suçlamayan dil.
- **Görünüm:** Tüm ekranlar Premium Dark tasarıma geçti; beş sekmeli alt gezinme, Denemeler ekranı referans görsele göre yapıldı.
- **Sıfırlama:** Verileri sıfırlama artık kişisel ayarları (diploma notu ve benzerleri) ve paylaşılmış rapor önbelleğini de temizler. Tema, yazı boyutu ve süre tercihleri korunur.

## Kaldırılanlar

- 778 soruluk gömülü soru bankası ve soru çözme, soru tabanlı deneme, otomatik yanlış ekranları.
- "İnternet izni yok" beyanı. Reklam, Play satın alma ve rıza formu için INTERNET izni eklendi; çalışma verisi cihazdan çıkmaz.

## Yeni eklenenler

- Kişisel Kaynak Merkezi: kitap, PDF ve diğer kaynaklar; sayfa, ilerleme, kaynak başına başarı.
- Pusula Koç: yerel, açıklanabilir öneri motoru; Bugünkü Rotan, kabul, ret, düzenle, taşı, sil; haftalık değerlendirme. Yapay zekâ gibi sunulmaz.
- Akıllı Plan 2.0 ve kaçırılan programı yeniden düzenleme: önizleme ve onay, üç seçenek, plan geçmişi.
- Manuel soru performansı kaydı (`PracticeLog`).
- Hedef netler, dinlenme günleri.
- Haftalık ve aylık rapor; PDF ve görsel olarak paylaşma.
- Dört ana ekran bileşeni: geri sayım, günlük hedef, bugünkü görevler, odaklanma.
- 100 özgün motivasyon sözü, 10 kategori, favoriler, kopyalama, paylaşma, isteğe bağlı günlük bildirim.
- AdMob yerel reklamları, UMP rıza akışı, tek seferlik `remove_ads_lifetime` satın alması, satın alma geri yükleme.
- Reklam yanlışlıkla tıklama koruması: reklamın üstünden başlayan kaydırma reklamı açmaz, normal dokunuş açar. Testlerde bulunan bir sorun üzerine eklendi.
- Gizlilik politikası ve kullanım koşulları ekranları; mağaza metinleri, görseller, veri güvenliği envanteri, rakip analizi.

## Rakip araştırmasından gelen eklemeler

Kaynak bazlı takip ve kaçırılan gün düzenleme rakiplerde görülmedi, ana fark olarak öne çıkarıldı. Tek seferlik reklamsız ödeme, aylık abonelikli rakiplere karşı konumlandırıldı. Ayrıntı: `play/rakip-analizi.md`.

## Teknik

AGP 9.5.1, Gradle 9.6.0, Kotlin 2.4.21, compileSdk 37, targetSdk 36, minSdk 26, versionCode 2, versionName 2.0.0, R8 açık, Billing 9.1.0, Google Mobile Ads Next-Gen 1.6.0, UMP 4.0.0, Room 2.8.5 (şema 2).
