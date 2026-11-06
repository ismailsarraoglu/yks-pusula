# Değişiklik raporu: 1.0.0 → 2.0.0

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
- Gizlilik politikası ve kullanım koşulları ekranları; mağaza metinleri, görseller, veri güvenliği envanteri, rakip analizi.

## Rakip araştırmasından gelen eklemeler

Kaynak bazlı takip ve kaçırılan gün düzenleme rakiplerde görülmedi, ana fark olarak öne çıkarıldı. Tek seferlik reklamsız ödeme, aylık abonelikli rakiplere karşı konumlandırıldı. Ayrıntı: `play/rakip-analizi.md`.

## Teknik

AGP 9.5.1, Gradle 9.6.0, Kotlin 2.4.21, compileSdk 37, targetSdk 36, minSdk 26, versionCode 2, versionName 2.0.0, R8 açık, Billing 9.1.0, Google Mobile Ads Next-Gen 1.6.0, UMP 4.0.0, Room 2.8.5 (şema 2).
