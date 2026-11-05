# YKS Pusula teslim raporu

Sürüm 1.0.0. Test edilen kod sürümü: `9ce13fd` (5 Kasım 2026). Bu rapordaki sonuçlar bu sürüme aittir.

## Özet

Uygulama Kotlin, Jetpack Compose, Material 3, Room ve DataStore ile yazıldı. APK dosyaları derlendi,
Android 8, 11 ve 13 emülatörlerinde kuruldu ve açıldı. Tüm otomatik testler geçti.

**Gerçek bir telefonda denenmedi.** Android 14 ve üzeri de denenemedi, çünkü Android 14 emülatörü test ortamında açılmadı.

## Testler

| Test | Ortam | Sonuç |
|---|---|---|
| Hesap testleri (net, tekrar, plan, arama, seri, süre) | Bilgisayar | 20/20 geçti |
| Tam uygulama testleri | Android 11 | 23/23 geçti |
| Veri ve iş akışı testleri | Android 8.0 | 16/16 geçti |
| Veri ve iş akışı testleri | Android 13 | 16/16 geçti |
| Release APK temiz kurulum ve açılış | Android 8, 11, 13 | Kuruldu, açıldı, çökme yok |
| Ağ izni denetimi | Birleşik manifest ve APK | INTERNET ve ACCESS_NETWORK_STATE yok |
| APK imza denetimi | Release APK | v2 imzası doğrulandı |

Testler sırasında uçak modu açıktı, Wi-Fi kapalıydı ve dış ağa erişim denemesi "Network is unreachable" sonucunu verdi.

## 24 kabul maddesi

| # | Madde | Sonuç | Not |
|---|---|---|---|
| 1 | Temiz kurulumda açılıyor | ✅ | Android 8, 11, 13 |
| 2 | İnternetsiz çalışıyor | ✅ | Uçak modunda test edildi |
| 3 | Ana ekran yükleniyor | ✅ | |
| 4 | Menüler açılıyor | ✅ | Beş sekme ve 44 ekran |
| 5 | TYT, AYT, YDT görünüyor | ✅ | |
| 6 | Soru çözme çalışıyor | ✅ | |
| 7 | Doğru ve yanlış doğru hesaplanıyor | ✅ | 4 doğru, 2 yanlış, 2 boş = 3,50 net; 30 doğru, 8 yanlış = 28,00 net |
| 8 | Deneme cevapları otomatik kaydediliyor | ✅ | |
| 9 | Deneme süresi doğru işliyor | ⚠️ | Geri sayım ekranda görüldü. Otomatik bitiş, süresi geçmiş denemeyle test edildi; tam 165 dakika beklenmedi |
| 10 | Uygulama kapanınca deneme sürüyor | ✅ | Ekran yeniden oluşturuldu, uygulama kapatılıp açıldı |
| 11 | Yanlışlar defteri kaydediyor | ✅ | 1-3-7-14-30 gün aralıkları dahil |
| 12 | Çalışma süresi kaydediliyor | ✅ | |
| 13 | Pomodoro çalışıyor | ⚠️ | Başlat, duraklat, devam et, sıfırla tıklandı. Kapalı uygulama senaryosu zaman ileri alınarak test edildi; tam süre beklenmedi |
| 14 | Haftalık plan güncelleniyor | ✅ | |
| 15 | Grafikler gerçek veriyi gösteriyor | ⚠️ | Veri kaynakları test edildi. Grafikler ekran görüntüsünden örnekleme ile incelendi |
| 16 | Koyu tema | ✅ | 44 ekran açıldı; görsel inceleme örneklemeyle yapıldı |
| 17 | Açık tema | ✅ | 44 ekran açıldı ve incelendi |
| 18 | Bildirimler internetsiz çalışıyor | ⚠️ | Bildirim Android 8, 11, 13'te anında üretildi. Saatinde gelmesi ve yeniden başlatma sonrası kurulması beklenerek test edilmedi; izin penceresi de tıklanmadı |
| 19 | Yedekleme ve geri yükleme | ⚠️ | Dışa aktarma, değiştirme, birleştirme ve bozuk dosya reddi test edildi. Android dosya seçicisi elle denenmedi |
| 20 | Yeniden başlatınca veriler korunuyor | ✅ | Zorla durdurma ve cihaz yeniden başlatma |
| 21 | Farklı ekran boyutları | ✅ | 320, 411 ve 914 dp genişlik |
| 22 | Geri tuşu doğru çalışıyor | ✅ | Soru ve deneme akışlarında test edildi |
| 23 | İnternet izni yok | ✅ | |
| 24 | İşlevsiz buton veya sahte ekran yok | ⚠️ | Ana düğmeler tıklandı. Dosya seçici açan düğmeler (PDF ekleme, yedek, içerik ve üniversite paketi) ve bazı küçük düğmeler otomatik olarak tıklanmadı |

⚠️ işareti maddenin başarısız olduğu anlamına gelmez; testin kapsamının sınırlı olduğunu gösterir.

Testlerin yakaladığı ve düzeltilen gerçek hatalar: ilk kurulumda otomatik programın oluşmaması, Ana Sayfa'dan
açılan denemede geri tuşunun bitmiş denemeye dönmesi, küçük ekranda alt menü yazılarının kesilmesi, koyu temada
sistem çubuğunun açık renkte kalması.

## İçerik paketi

Sürüm 1.0.0, `2027-YKS-cerceve`, geçerlilik yılı 2027.

| İçerik | Miktar |
|---|---|
| Toplam soru | 778 (416 TYT, 281 AYT, 81 YDT) |
| Elle yazılan soru | 335 |
| Hesaplanarak üretilen soru | 443 |
| Zorluk | 360 kolay, 385 orta, 33 zor |
| Konu ve alt konu | 261 |
| Anlatımı olan konu | 236 |
| Örnek çözümü olan konu | 221 |
| Formül ve bilgi kaydı | 236 |
| Kart | 411 (135'i İngilizce kelime) |

Bilmeniz gerekenler:

- Hesaplanan 443 soruda cevabı program hesaplar, ama birçoğu aynı kalıbın farklı sayılı hâlidir. Yani 778 soru, 778 farklı kalıp demek değildir.
- Elle yazılan soruların cevaplarını ben kontrol ettim. **Bir öğretmen veya alan uzmanı incelemedi.** Öğrencilere yaygın olarak dağıtılmadan önce bu inceleme yapılmalıdır.
- Soru sayıları dersler arasında dengeli değildir. Matematik ve fizik güçlü, sözel derslerde soru sayısı azdır. Örneğin Din Kültürü'nde 6-10 soru vardır.
- Konu anlatımları kısa özet düzeyindedir, ders kitabının yerini tutmaz.
- Kartların bir bölümü konu notlarından otomatik çıkarıldı. Bazıları kısa ve yüzeyseldir.
- Almanca, Fransızca, Rusça ve Arapça için soru yoktur, yalnızca soru türü başlıkları vardır. Bu 24 başlık ve kapsamı belirsiz Elektrostatik sorusuzdur.
- TYT ve YDT genel denemeleri tam soru sayısıyla oluşur. AYT Sosyal Bilimler-2 testinde Coğrafya-2 sorusu 1 eksiktir (39/40); uygulama bunu deneme başlamadan gösterir. Soru bankası küçük olduğu için aynı sorular birkaç denemeden sonra tekrar eder.

## Doğrulanması gereken bilgiler

- **Müfredat:** Konu listesi resmî MEB öğretim programlarıyla satır satır karşılaştırılmadı. Bilgime göre üç başlık işaretlendi: Polinomlar (listede yoktu, eklendi), Elektrostatik (TYT kapsamı belirsiz), Evrim (2018 programında bulunmuyor, kapsam dışı bırakıldı).
- **Maarif Modeli:** İçerik 2027 YKS çerçevesine göre hazırlandı. Sınav yılı 2028 ve sonrası seçilirse uygulama uyarı gösterir. Bu yıllar için ÖSYM ve MEB kapsamı netleşince yeni içerik paketi gerekir.
- **Takvim:** 2027 tarihleri haberlerde ÖSYM takvimine dayandırılıyor. Resmî takvim osym.gov.tr adresinden kontrol edilmelidir. Tarihler Sınav ayarlarından değiştirilebilir.
- **Ders dağılımı:** Deneme içindeki ders başına soru sayıları uygulamanın kullandığı değerlerdir, kılavuzla karşılaştırılmalıdır.
- **OBP:** Kural (diploma notu × 5, katsayı 0,12 veya 0,06) resmî olmayan bir kaynaktan alındı. Kılavuzla karşılaştırılmalıdır.

## Bilerek eklenmeyenler

- Puan ve sıralama tahmini: Resmî yıl verileri uygulamada yok.
- Üniversite taban puanı, kontenjan ve YÖK Atlas verisi: Doğrulanmış veri seti yok. Kullanıcı kaynağı ve sürümü belli bir dosyayı içe aktarabilir.

## Diğer sınırlar

- Yedek dosyası ZIP değil, tek JSON dosyasıdır. Eklenen PDF'ler yedeğe dahil değildir.
- Release APK, depoda duran bir anahtarla imzalıdır. Yalnızca dışarıdan kurulum içindir. Play Store için yeni anahtar gerekir ve depo herkese açık yapılmamalıdır.
- Sınav süresi uygulama kapalıyken de işlemeye devam eder.

## Teslim edilen dosyalar

| Dosya | Açıklama |
|---|---|
| `yks-pusula-release.apk` | Telefona kurulacak sürüm (yaklaşık 2 MB) |
| `yks-pusula-debug.apk` | Test sürümü (yaklaşık 18 MB) |
| `yks-pusula-kaynak.zip` | Android Studio projesi |
| `README.md` | Kurulum talimatı |
| `TESLIM-RAPORU.md` | Bu rapor |

## Kaynaklar

- [Yeni Asır: 2027 YKS tarihleri](https://www.yeniasir.com.tr/yasam/2026/10/28/son-dakika-osym-baskani-duyurdu-2027-yks-tarihi-aciklandi)
- [Ekonomist: 2026 YKS soru sayıları ve süreleri](https://www.ekonomist.com.tr/egitim/2026-yks-ne-zaman-saat-kacta-basliyor-tyt-ayt-ydt-soru-sayilari-ve-sureleri-75753)
- [Unikazan: Maarif Modeli ve YKS](https://unikazan.com/blog/turkiye-yuzyili-maarif-modeli-yksyi-nasil-etkileyecek/)
- [HemenHesap: YKS puan ve OBP rehberi](https://www.hemenhesap.com/blog/yks-puan-nasil-hesaplanir-2026)
