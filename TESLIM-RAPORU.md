# YKS Pusula: Plan ve Takip, teslim raporu

Sürüm 2.0.0 (versionCode 2). Teslim edilen APK ve AAB: `e6f4882`, tam tur `42242504720`, 6 Kasım 2026.

## Özet

Uygulama yeni sürüme taşındı: soru bankası kaldırıldı, yeni özellikler eklendi, tüm ekranlar Premium Dark tasarıma geçti, reklam ve reklamsız satın alma kodu yazıldı. Otomatik testler Android 8, 11, 13, 14 ve 16 emülatörlerinde geçti.

**Google Play'e bugün yüklenemez.** Gerçek reklam kimlikleri, yükleme anahtarı ve Play ürünü sizin hesaplarınıza bağlıdır. Satın alma, satın alma geri yükleme ve Avrupa rıza formu bu yüzden gerçek Google Play ile denenmedi. Gerçek telefonda da test yapılmadı.

Ayrıntılı sonuçlar: `docs/test-raporu.md`. Size kalan işler: `docs/kalan-manuel-isler.md`.

## Sorularınızın cevapları

| Soru | Cevap |
|---|---|
| Hangi mevcut özellikler korundu? | Profil, 26 ders ve 261 konu, 236 formül, 411 bilgi kartı, kronometre, Pomodoro, plan, notlar, PDF kitaplığı, üniversite hedefleri, net hesabı, OBP, istatistik, rozetler, seri, bildirimler, yedekleme, açık tema, içerik paketi, arama |
| Hangileri geliştirildi? | Konu takibi, plan, denemeler, yanlış defteri, Pomodoro, yedekleme, bildirimler, görünüm, veri sıfırlama. Ayrıntı: `docs/degisiklik-raporu.md` |
| Hangi özellikler kaldırıldı? | 778 soruluk gömülü soru bankası, uygulama içi soru çözme, soru tabanlı deneme, otomatik yanlış ekranları, "İnternet izni yok" beyanı |
| Hangi yenilikler eklendi? | Kişisel Kaynak Merkezi, Pusula Koç, Akıllı Plan 2.0, kaçırılan programı düzenleme, manuel soru performansı, hedef netler, dinlenme günleri, haftalık ve aylık rapor, 4 ana ekran bileşeni, 100 özgün söz, reklam, reklamsız satın alma, rıza akışı, mağaza paketi |
| Veritabanında ne değişti? | Room şema 1 → 2. 5 soru tablosu kaldırıldı, 10 yeni tablo eklendi, eski denemeler ve yanlışlar yeni tablolara taşındı. Ayrıntı: `docs/veritabani-gecisi.md` |
| Kullanıcı verileri korundu mu? | Evet, testte profil, notlar, denemeler, yanlış kayıtları ve soru sayıları korundu. **Bilinen kayıplar:** eski yanlış kayıtlarında soru metni ve açıklama gelir ama sorunun şekli gelmez. Soru bankasına bağlı yer imleri silinir. "Soru çözüldü" konu durumu "çalışıldı" durumuna iner |
| Migration testleri yapıldı mı? | Evet, 7 emülatör kurulumunda geçti. Gerçek 1.0.0 APK'sının üstüne kurarak denenmedi |
| 100 motivasyon sözü var mı? | Evet, tam 100 benzersiz söz, test ile doğrulandı |
| Reklamlar çalışıyor mu? | Google test kimlikleriyle evet, Android 14 ve 16 Play imajlarında. Reklamın üstünden kaydırınca reklamın yanlışlıkla açılması testte bulundu ve düzeltildi. Gerçek kimlikler yok, gerçek reklam denenmedi |
| Satın alma çalışıyor mu? | **Gerçek Play ile denenmedi.** Kod yazıldı, sonuç işleme benzetilmiş sonuçlarla test edildi |
| Satın alma geri yükleniyor mu? | **Gerçek Play ile denenmedi.** Kod yazıldı, Play ürünü olmadan çalıştırılamaz |
| Reklamsız kullanıcı reklam görüyor mu? | Hayır. Reklamsız hak açılınca reklam ekrandan kalktı, yeni istek yapılmadı. Hak benzetimle açıldı |
| İnternetsiz temel işlevler çalışıyor mu? | Evet, veri ve arayüz testleri çevrimdışı çalıştı |
| Bütün ekranlar yeni tasarıma geçti mi? | Evet. Tüm ekranlar 5 yapılandırmada iki temada açıldı, görüntüler örnekleme ile incelendi |
| Hangi testler başarılı? | Birim 44/44, Android 11 32/32, Android 8 ve 13 24/24, Android 14 ve 16 30/30, stres 3/3, düğme 5/5, kalıcılık 3 sürümde, release kurulum 5 sürümde, reklam testi 2 Play imajında, reklam üstü kaydırma 396/396 |
| Hangi testler başarısız? | Son turda uygulama kodundan kaynaklı başarısız test yok. Emülatörden kaynaklı 5 olay oldu, hepsi tekrarda geçti: 4 kez test süreci açılamadı (3'ünün kaydı bunun uygulama kodu çalışmadan önce, aşırı yüklü emülatörde olduğunu gösteriyor; 1'inin kaydı yetersiz), 1 kez Google Play Hizmetleri yeniden başlarken Android uygulamayı da kapattı. Önceki turlarda nedeni bulunamayan 3 tek seferlik olay var. Ayrıntı: `docs/test-raporu.md` |
| Release APK oluştu mu? | Evet, 4,8 MB. Yan yükleme anahtarıyla imzalı, test reklam kimlikli |
| AAB oluştu mu? | Evet, 10,6 MB. **Bu dosya Play'e yüklenmemeli:** yan yükleme anahtarı ve test kimlikleri içerir. Play için kendi anahtarınızla üretin |
| Google Play için ne eksik? | Geliştirici hesabı, yükleme anahtarı, AdMob kimlikleri, Play ürünü, herkese açık gizlilik politikası adresi ve e-postası, Console beyanları, gerçek cihaz testi, 12 kişilik 14 günlük kapalı test |

## Nihai kabul listesi (Bölüm 41)

✅ tamam · ⚠ kısmen, not var · ❌ yapılamadı

| Madde | Sonuç | Not |
|---|---|---|
| Orijinal kaynak kod üzerinde çalışıldı | ✅ | |
| Mevcut uygulama incelendi | ✅ | |
| Marka adı korundu | ✅ | |
| Soru bankası kaldırıldı | ✅ | |
| Kullanıcı verileri korundu | ✅ | Eski soruların şekilleri ve soru yer imleri gelmez |
| Manuel soru performans takibi | ✅ | |
| Kaynak yönetim sistemi | ✅ | |
| Pusula Koç | ✅ | Yerel kural motoru, yapay zekâ değil |
| Günlük çalışma rotası | ✅ | |
| Akıllı program oluşturma | ✅ | |
| Kaçırılan program düzenleme | ✅ | |
| Konu ustalığı haritası | ✅ | |
| Yanlış defteri | ⚠ | Fotoğraf seçici denenmedi |
| Aralıklı tekrar | ✅ | |
| Deneme yönetimi | ✅ | |
| Deneme analizi | ✅ | |
| OBP | ✅ | Diploma ×5 katsayısı yalnızca ikincil kaynakta bulundu |
| Üniversite hedefleri | ✅ | |
| Formüller korundu | ✅ | |
| Bilgi kartları korundu | ✅ | |
| PDF kaynakları | ⚠ | Dosya seçiciyle ekleme denenmedi |
| Notlar | ✅ | |
| Haftalık rapor | ✅ | |
| Android widget'ları | ⚠ | Çizim test edildi, gerçek ana ekrana eklenmedi |
| Bildirimler | ⚠ | Gösterim test edildi, saatinde gelmesi beklenmedi |
| Tam 100 söz | ✅ | |
| Günün sözü değişimi | ✅ | |
| Favoriler | ✅ | |
| Paylaşım | ⚠ | Dosya üretiliyor, gerçek uygulamaya gönderilmedi |
| Premium Dark tüm ekranlar | ✅ | |
| Denemeler ekranı referansa uygun | ✅ | Başlık görseldeki gibi "Denemeler" |
| Alt navigasyon | ✅ | |
| Ücretsiz kullanıcı reklam görüyor | ⚠ | Test kimliğiyle. Üretim kimliği yok |
| Satın alma sistemi | ❌ | Kod hazır, gerçek Play ile denenmedi |
| Satın alma geri yükleme | ❌ | Kod hazır, gerçek Play ile denenmedi |
| Reklamsız kullanıcı reklam görmüyor | ✅ | Hak benzetimle açıldı |
| UMP ve rıza yönetimi | ⚠ | Avrupa formu denenmedi |
| Temel veriler cihazda | ✅ | Sunucu ve hesap yok |
| İnternetsiz temel özellikler | ✅ | |
| Yedekleme ve geri yükleme | ⚠ | Dosya seçiciyle denenmedi |
| Room migration testleri | ✅ | |
| API 36 hedefi | ✅ | |
| Release derlemesi | ✅ | |
| AAB oluşturuldu | ✅ | Üretim için yeniden üretilmeli |
| Gerçek test raporu | ✅ | |
| Kalan manuel işlemler | ✅ | |

## Teslim dosyaları

| # | Çıktı | Yer |
|---|---|---|
| 1 | Android Studio kaynak kodu | Kaynak ZIP |
| 2-3 | Release AAB ve APK | Ayrı dosyalar |
| 4 | Geçiş dosyaları | `app/.../data/Migrations.kt`, `app/schemas/`, `docs/veritabani-gecisi.md` |
| 5 | Tasarım sistemi | `docs/tasarim-sistemi.md` |
| 6 | Uygulama simgesi | `play/gorseller/simge-512.png` |
| 7 | 8 mağaza ekran görseli | `play/gorseller/` (gerçek ekranlar, örnek veriyle) |
| 8 | Tanıtım görseli | `play/gorseller/tanitim-1024x500.png` |
| 9-10 | Kısa ve uzun açıklama | `play/magaza-metinleri.md` |
| 11 | Gizlilik politikası | `play/gizlilik-politikasi.html` (e-posta alanını doldurun) |
| 12 | Veri güvenliği envanteri | `play/veri-guvenligi-envanteri.md` |
| 13 | AdMob talimatı | `play/admob-talimat.md` |
| 14 | Play ürün talimatı | `play/play-yayin-talimat.md` |
| 15 | Test raporu | `docs/test-raporu.md` |
| 16 | Değişiklik raporu | `docs/degisiklik-raporu.md` |
| 17 | Kalan manuel işler | `docs/kalan-manuel-isler.md` |

Rakip özellik matrisi: `play/rakip-analizi.md`.

## Bilmeniz gereken sınırlar

- **Yayın işi denenmedi.** Play için imzalı AAB üreten `yayin-aab` işi hazır ama çalıştırılmadı, çünkü sizin anahtarınız ve kimlikleriniz gerekiyor.
- **İmza farkı.** Dışarıdan kurulan APK, Play sürümüyle güncellenemez. Yedek alıp kaldırın, Play'den kurup yedeği geri yükleyin.
- **Şifreli yedekleme eklenmedi.** İstek isteğe bağlıydı. Yedek ZIP'i sağlama toplamıyla korunur, şifreli değildir.
- **Banner ve geçiş reklamı yok.** Yalnızca yerel reklam kullanıldı.
- **Play Hizmetleri yeniden başlaması.** Google Play Hizmetleri güncellenirken veya çökerken Android, reklam kullanan uygulamaları da kapatabilir. Bu uygulama hatası değildir, kayıtlı veri kaybolmaz.
- **Tarih ve katsayılar.** 2027 YKS tarihleri haberlerden alındı. ÖSYM kılavuzu çıkınca karşılaştırın.
- **Rakip analizi.** Mağaza yorumlarının yazılı içeriğine erişilemedi. İndirme veya sıralama garantisi verilmez.
- **Teknik seçimler.** Bileşenler Glance yerine RemoteViews, zamanlama WorkManager yerine AlarmManager ile yapıldı.
