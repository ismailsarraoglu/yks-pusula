# Test raporu

Sürüm 2.0.0 (versionCode 2). Test edilen uygulama kodu: `de4b185`, tam tur `42242504720`, 6 Kasım 2026.
Testler GitHub Actions üzerindeki Android emülatörlerinde çalıştı. **Gerçek telefonda ve gerçek Google Play bağlantısıyla hiçbir test yapılmadı.**

İşaretler: ✅ otomatik test geçti · ⚠ kısmen denendi, sınırı yanında yazıyor · ❌ denenmedi

## Son tam tur

| Test | Ortam | Sonuç |
|---|---|---|
| Birim testleri | Bilgisayar | 44/44 |
| Tam cihaz testleri | Android 11 | 32/32 |
| Ana düğmeler, art arda 5 kez | Android 11 | 5/5 |
| Geçiş sırasında kapanma stresi, 3 tur × 35 geçiş | Android 11 | 3/3 |
| Ekran taraması: 5 yapılandırma, açık ve koyu tema | Android 11 | Tamamı açıldı, 455 görüntü |
| Veri yazma, zorla durdurma, yeniden başlatma | Android 11, 14, 16 | Hepsi geçti |
| Veri ve geçiş testleri | Android 8 ve 13 | 24/24 |
| Veri, geçiş ve arayüz testleri | Android 14 ve 16 | 30/30, ilk denemede |
| Room geçişi | Android 14 ve 16 Play imajı | 1/1 |
| Çevrimiçi test reklamı ve satın alma sonrası kapanma | Android 14 Play imajı | Geçti, 1. denemede |
| Çevrimiçi test reklamı ve satın alma sonrası kapanma | Android 16 Play imajı | Geçti, 2. denemede. 1. denemede test süreci uygulama kodu çalışmadan önce açılamadı (aşağıda) |
| Reklamın üstünden başlayan kaydırma reklamı açmıyor | Android 16 Play imajı, 4 ayrı açılış × 3 tur | 11/12 tur geçti, toplam 396 kaydırma. 17 kaydırma koruma tarafından durduruldu, Play Store hiç açılmadı. Kalan 1 tur test başlamadan düştü (aşağıda) |
| Release temiz kurulum ve açılış | Android 8, 11, 13, 14, 16 | Kuruldu, açıldı, çökme satırı 0 |
| Release art arda temiz kurulum | Android 14 ve 16 Play imajı | 5/6 ve 6/6. Android 14'teki 1 kayıp Google Play Hizmetleri'nin yeniden başlamasından (aşağıda) |

Ölçümler (Android 11 emülatörü):

- Soğuk açılış 1421, 1216, 1597 ms
- Bellek 70,7 MB PSS
- ANR 0
- Release APK 4,8 MB, AAB 10,6 MB
- Hedef API 36, en düşük API 26, debuggable değil, imza doğrulandı

Android 8'de uçak modu komutu çalışmadı; bu sürümde Wi-Fi ve mobil veri kapatılarak test edildi. Diğer sürümlerde uçak modu açıktı. Android 9, 10, 12 ve 15 denenmedi.

## Bölüm 38 listesi

### Mevcut özellikler

| Madde | Durum | Dayanak ve sınır |
|---|---|---|
| İlk kurulum | ✅ | Karşılama ve profil akışı; release ilk açılış |
| Ana sayfa | ✅ | Arayüz testi ve ekran taraması |
| Profil | ✅ | Ekran taraması, ayar değişikliği testi |
| Çalış | ✅ | Çalışma kaydı akışı istatistiği güncelliyor |
| Günün rotası | ✅ | Kabul, ret, taşıma kayıtları kalıcı |
| Çalışma planı | ✅ | Görev ekleme, tamamlanan görev korunuyor |
| Otomatik plan | ✅ | Kapasite, bitiş saati, zayıf konu önceliği |
| Planı yeniden düzenleme | ✅ | Taşıma, geçmiş, sığmayan görev bildirimi |
| Kaynak ekleme, güncelleme | ✅ | Düğmeyle ekleme, veri akışı |
| Kaynak çalışma kaydı | ✅ | Kaynak silinince istatistik korunuyor |
| Konu takibi | ✅ | 5 seviye, tek işaretle güçlü olmuyor |
| Yanlış defteri | ⚠ | Elle ekleme ve tekrar geçti. Fotoğraf seçici (Android dosya seçicisi) denenmedi |
| Tekrar sistemi | ✅ | Aralıklar ve Hatırladım akışı |
| Deneme ekleme, sonuçları | ✅ | Süre tutma, yeniden oluşturmaya dayanma, sonuç ekranı |
| Net hesaplama | ✅ | Birim testi; arayüzde 28,00 net doğrulandı |
| OBP | ✅ | Birim testi; 85 diploma notu → 51,00 |
| İstatistik | ✅ | Kayıtlardan besleniyor, ekranda doğrulandı |
| Formüller | ✅ | 236 formül korundu, ekranlar açıldı |
| Bilgi kartları | ✅ | Kart ekleme, çevirme, değerlendirme |
| Notlar | ✅ | Ekleme ve listeleme |
| PDF | ⚠ | Rapor PDF'i üretiliyor. Kitaplığa dosya seçiciyle PDF ekleme denenmedi |
| Üniversite hedefleri | ✅ | Ekleme testi |
| Pomodoro | ✅ | Başlat, duraklat, bitir; uygulama kapalıyken dayanıyor |
| Widget | ⚠ | Dört bileşen gerçek veriyle çizildi. Gerçek ana ekrana eklenmedi |
| Bildirim | ⚠ | Bildirim gösteriliyor, aynı gün tekrar etmiyor. Seçilen saatte gelmesi beklenmedi |
| Yedekleme, geri yükleme | ⚠ | ZIP gidiş dönüş, fotoğraf, bozuk dosya reddi geçti. Android dosya seçicisiyle denenmedi |
| Motivasyon sözleri | ✅ | Tam 100 benzersiz söz, günlük söz sabit, döngüde tekrar yok |
| Favoriler | ✅ | Veri testi |
| Paylaşım | ⚠ | Rapor dosyası üretiliyor. Paylaşım penceresi gerçek uygulamaya gönderilerek denenmedi |

### Soru bankası kaldırma

| Madde | Durum | Dayanak |
|---|---|---|
| Menü ve soru çözme ekranları kaldırıldı | ✅ | Rota listesi, ekran taraması, içerikte soru yok testi |
| Yanlış kayıtları, deneme geçmişi, notlar korundu | ✅ | Geçiş testi |
| Eski veritabanı güvenli taşındı | ✅ | Şema 1 → 2 geçiş testi, 7 sürüm/imaj. Veritabanı şema 1 dosyasından kuruldu; gerçek 1.0.0 APK'sının üstüne kurarak denenmedi |
| Yeni manuel soru istatistikleri | ✅ | Veri akışı testi |
| Foreign key hatası | ✅ | Geçiş testinde hata yok |

### Reklam

| Madde | Durum | Dayanak ve sınır |
|---|---|---|
| Ücretsiz kullanıcı, yerel reklam | ✅ | Google test kimliğiyle Denemeler ekranında reklam göründü (Android 14, 16) |
| Banner | — | Kullanılmıyor. Şartname yerel reklam veya banner seçeneği veriyor |
| Yüklenme hatası | ⚠ | Kod yolu hatayı yakalıyor, alan görünmüyor. Sunucudan hata senaryosu üretilmedi |
| İnternet kesintisi | ✅ | Uçak modunda reklam alanı yok, uygulama çalışıyor |
| Rıza verilmemesi | ⚠ | İzin yokken istek ve alan yok. Gerçek rıza formunda "reddet" denenmedi |
| Gösterilmeyen ekranlar | ✅ | Kod denetimi: reklam alanı yalnızca Ana sayfa, Çalış, Denemeler, İstatistik |
| Reklam sıklığı | ⚠ | Kodda 90 sn alt sınır ve ilk 3 açılışta reklam yok. Otomatik testi yok |
| Satın alma sonrası kapanma | ✅ | Hak açılınca reklam ekrandan kalktı (benzetilmiş satın alma sonucu) |
| Yanlışlıkla tıklama | ✅ | Reklamın üstünden başlayan kaydırma tıklama sayılmıyor. Önceki sürümde 1 kez Play Store açılmıştı, düzeltmeden sonra 396 kaydırmada 0. Dokunuş hâlâ reklama ulaşıyor (3 cihaz testi). Gerçek telefonda denenmedi |

### Satın alma

Play Console ürünü henüz yok, bu yüzden gerçek Play bağlantısıyla satın alma denenmedi ve **para harcanmadı**.

| Madde | Durum | Dayanak ve sınır |
|---|---|---|
| Ürün bilgisi, yerelleştirilmiş fiyat | ❌ | Play ürünü gerekir |
| Başarılı, iptal, başarısız ödeme | ⚠ | Sonuçların işlenişi benzetilmiş sonuçlarla test edildi. Gerçek Play akışı yok |
| Pending | ⚠ | Bekleyen ödeme hak vermiyor (benzetim) |
| Acknowledge | ❌ | Kod var, Play olmadan çalıştırılamaz |
| Yeniden başlatma, internet kesilmesi | ⚠ | Hak çevrimdışı korunuyor (benzetim) |
| Satın alma geri yükleme | ❌ | Kod var, Play olmadan çalıştırılamaz |
| Reklamların tamamen kapanması | ✅ | Hak açıkken reklam isteği yapılmıyor |

### Gizlilik

| Madde | Durum | Dayanak ve sınır |
|---|---|---|
| UMP açılışı | ⚠ | Emülatör konumunda form gerekmedi, rıza bilgisi alındı, reklam izni açıldı. Avrupa formu denenmedi |
| Rıza güncellemesi, gizlilik seçenekleri | ❌ | Avrupa konumu ve gerçek AdMob iletisi gerekir |
| Reklam istek kontrolü | ✅ | İzin ve hak yokken istek yapılmıyor |
| Reklam kimliği, test cihazı | ⚠ | Geliştirmede Google test kimlikleri. Üretim kimlikleri yok |
| Veri güvenliği beyanı | ⚠ | Envanter hazır; beyan Play Console'da doldurulacak |

### Cihaz

| Madde | Durum | Dayanak ve sınır |
|---|---|---|
| Android 8, 11, 13, 14, 16 | ✅ | Emülatör. 9, 10, 12, 15 denenmedi |
| Küçük, normal, büyük ekran, tablet | ✅ | 320, 411, 914 dp ekran taraması |
| Düşük RAM | ⚠ | Android 8, 1,5 GB ile çalıştı. Android Go veya düşük bellek profili denenmedi |
| Açık ve koyu | ✅ | Tüm ekranlar iki temada |
| Büyük yazı | ✅ | 1,5 yazı ölçeği |
| Ekran döndürme | ✅ | Yatay tarama, yeniden oluşturma testi |
| Gesture navigation | ✅ | Android 14 ve 16'da hareketli gezinme açık |
| Bildirim izni yok | ❌ | İzin reddedilmiş akış denenmedi |
| İnternet yok | ✅ | Tüm veri ve arayüz testleri çevrimdışı |
| Arka plan, yeniden başlatma | ✅ | Zorla durdurma, cihaz yeniden başlatma |

### Performans

| Madde | Durum | Dayanak ve sınır |
|---|---|---|
| Soğuk açılış | ✅ | Yaklaşık 1,2-1,6 sn |
| Bellek | ✅ | 70 MB |
| ANR, crash | ✅ | Release sürümünde uygulamadan kaynaklı çökme ve ANR 0. Test ortamındaki olaylar ve nedenleri aşağıda |
| Ekran geçişleri | ⚠ | Stres testi geçti, kare süresi ölçülmedi |
| Room sorguları, PDF, grafikler, widget | ⚠ | Çalıştı, süre ölçümü yapılmadı |

## Ara sıra görülen olaylar

Bu olaylar saklanmadı. Her birinin Android kaydındaki gerçek nedeni yazıldı; nedeni bulunamayanlar öyle belirtildi.

### Reklam teşhisi

Android 14 ve 16 Play imajlarında reklam testleri ara sıra başarısız oldu. Nedeni bulmak için 3 teşhis turu yapıldı (yaklaşık 200 test çalıştırması). Bulunanlar:

| Bulgu | Nasıl anlaşıldı | Sonuç |
|---|---|---|
| **Reklamın üstünden başlayan hızlı kaydırma reklamı açıyordu** | 432 kaydırmada 1 kez Play Store açıldı (`market://details`), uygulama arka plana geçti | **Uygulama hatası, düzeltildi.** Reklam çevresine koruma eklendi: parmak başladığı yerden uzaklaştıysa kaldırma tıklama sayılmaz. Son turda 396 kaydırmada 17'si korumaya takıldı, Play Store 0 kez açıldı |
| Google Play Hizmetleri yeniden başlayınca uygulama kapatıldı | Android kaydı: `depends on provider com.google.android.gms/.fonts.provider.FontsProvider in dying proc com.google.android.gms.persistent` | Uygulama çökmesi değil. Android, ölen Play Hizmetleri sürecine bağlı uygulamaları da kapatır. Bağlantı reklam ve WebView tarafından geliyor, uygulamanın kendi kodunda yazı tipi sağlayıcısı kullanımı yok. Kaydedilmiş veri kaybolmaz. Son turda Android 14 release kurulumunda 1 kez görüldü. Önceki bir turda aynı belirti (release süreci 15 sn sonra yok) 1 kez görülmüştü; büyük ihtimalle aynı neden, o turun kaydı bunu kanıtlamaya yetmedi |
| Test süreci "açılışı tamamlayamadı" | Android kaydı: `bg anr ... failed to complete startup`. Süreç başladıktan 28 sn sonra bile uygulama sınıfı (`YksApp`) oluşturulmamıştı; aynı anda telefon, Play Hizmetleri ve sistem arayüzü de yanıt veremedi, işlemci baskısı %85 üstündeydi | Uygulama kodu çalışmadan önce oluyor; yeni açılmış, yükü çok yüksek emülatöre ait. Son turda 3 kez kayıtla doğrulandı, 1 kez (Android 16 geçiş testi) kayıt yetersiz ama aynı anda Play Hizmetleri ve telefon da açılamadı. Hepsi ikinci denemede geçti |
| Rıza adımının hiç başlamaması | Önceki bir testin satın alma benzetimi temizlenmeden yarıda kalması, uygulamanın doğru biçimde reklamsız davranması | Test hatası, düzeltildi |
| Gezinme hazır olmadan test ekran değiştirdi | `DebugNav.nav` boş | Test hatası, düzeltildi |
| Reklam sunucusu reklam vermedi (`NO_FILL`) | Sunucu yanıtı | Normal durum; uygulama reklam alanını göstermez |

### Diğer olaylar

| Olay | Sıklık | Durum |
|---|---|---|
| Android 11'de Compose iç hatası: test sonunda etkinlik kapanırken | 1 kez, önceki bir turda | Uygulama kodu iz kaydında yok. Son iki turda 5/5 düğme ve 105 geçişlik stres testi temiz. Yalnızca teste ait olduğu kanıtlanmadı |
| Android 11'de "Detected multithreaded access" | 1 kez, önceki sürüm | Tekrarlamadı, neden belirlenemedi |
| Android 16'da 503 çökme satırı | 1 kez, yoğun yeniden başlatma | Bellek ve bekleme ayarından sonra tekrarlamadı |
| Android 11 emülatörü yeniden başlatmada kapandı | 1 kez | Emülatör hatası; test betiği artık emülatörü aynı diskle açıyor |
| Android 8'de test uygulaması kurulu değil hatası, tüm tur geçersiz | 1 kez | Test betiği hatası, düzeltildi |

## Denenemeyenlerin kısa listesi

Gerçek telefon, gerçek satın alma ve geri yükleme, üretim reklam kimlikleri, Avrupa rıza formu, Android dosya seçicileri, ana ekrana bileşen ekleme, zamanlı bildirimin saatinde gelmesi, bildirim izni reddi, gerçek 1.0.0 üstüne güncelleme, kendi anahtarınızla `yayin-aab` işi. Yapılacaklar `docs/kalan-manuel-isler.md` içindedir.
