# Test raporu (2.1.0)

Sürüm 2.1.0 (versionCode 3). Test edilen kod: `035482b`, tam tur `42304857639`, 6-7 Kasım 2026.
Testler GitHub Actions üzerindeki Android emülatörlerinde çalıştı. **Gerçek telefonda ve gerçek Google Play bağlantısıyla hiçbir test yapılmadı.** 2.0.0 raporu: `docs/test-raporu-2.0.md`.

İşaretler: ✅ otomatik test geçti · ⚠ kısmen denendi, sınırı yanında yazıyor · ❌ denenmedi

## Son tam tur

| Test | Ortam | Sonuç |
|---|---|---|
| Birim testleri | Bilgisayar | 70/70 (plan süresi 19, ürün mantığı 27, genel mantık 19, analiz 5) |
| Tam cihaz testleri | Android 11 | 54/54 |
| Ana düğmeler, art arda 5 kez | Android 11 | 5/5 |
| Geçiş sırasında kapanma stresi, 3 tur × 35 geçiş | Android 11 | 3/3 |
| Ekran taraması: 5 yapılandırma, açık ve koyu tema | Android 11 | Tamamı açıldı, 467 görüntü (yeni: Öncelikli konular, karışık kart tekrarı) |
| Veri yazma, zorla durdurma, yeniden başlatma | Android 11, 14, 16 | Hepsi geçti |
| Veri, geçiş, yedek güvenliği, öğrenme akışı | Android 8 ve 13 | 46/46, ilk denemede |
| Aynısı ve arayüz testleri | Android 14 ve 16 | 52/52, ilk denemede |
| Room geçişi | Android 14 ve 16 Play imajı | 3/3 (1 → 2, 2 → 3, 1 → 3) |
| Çevrimiçi test reklamı ve reklamsız hak sonrası kapanma | Android 14 ve 16 Play imajı | İkisi de 1. denemede geçti |
| Yan yükleme (release ayarlı) temiz kurulum ve açılış | Android 8, 11, 13, 14, 16 | Kuruldu, açıldı, uygulama çökme satırı 0 |
| Art arda temiz kurulum | Android 14 ve 16 Play imajı | 6/6 ve 6/6 |
| Play derlemesi anahtarsız | CI | Beklenen hatayla durdu; eksik 7 değeri listeledi, AAB üretmedi |

Ölçümler (Android 11 emülatörü): soğuk açılış 1272, 1014, 1062 ms; bellek 71,1 MB PSS; ANR 0; test APK'sı 4,8 MB.

Android 8'de uçak modu komutu çalışmadı; bu sürümde Wi-Fi ve mobil veri kapatılarak test edildi. Android 9, 10, 12 ve 15 denenmedi.

## Bu turdan önce bulunan ve düzeltilen sorunlar

| Sorun | Neden | Düzeltme |
|---|---|---|
| Yedek güvenlik testleri hiç çalışmadı (1. tur) | İki test metodu `Unit` yerine değer döndürüyordu; JUnit sınıfı reddetti | Düzeltildi |
| Aynı adlı ZIP girişi testi kurulamadı (2. tur) | Java'nın ZIP yazıcısı aynı adı iki kez yazmıyor | Test dosyası bayt düzeyinde hazırlanıyor |
| Android 14 ve 16'da yol aşımı testi "dosya bozuk" mesajı aldı (2. tur) | Android 14 ve sonrası `..` içeren ZIP girişini kendisi reddediyor; uygulama genel mesaj gösteriyordu. Veri değişmiyordu | Bu durumda da "Yedekte geçersiz dosya yolu var" mesajı gösteriliyor |
| Öncelik listesi başlanmamış bütün konuları alfabetik sıralıyordu (ekran incelemesinde görüldü) | "Başlanmadı" tek başına puan veriyordu | Yalnızca gerçek sinyali olan konular listelenir |

## Bölüm 20 listesi

### İş mantığı

| Madde | Durum | Dayanak |
|---|---|---|
| Plan süresi hesaplama | ✅ | 30, 45, 60, 90, 120, 180, 240, 300 dk: her gün planlanan = hedef |
| Mola hesaplama | ✅ | Molalar süreye eklenmiyor, görevler üst üste binmiyor; plan ekranında ayrı gösteriliyor |
| Çalışma hedefi | ✅ | Saat yetersiz gün, en fazla süre sınırı, haftalık hedef, tamamlanmış görevler düşülerek (birim ve cihaz testi) |
| Dinlenme günü, sınava az kalan gün | ✅ | Birim testleri |
| Günlük ve haftalık ilerleme | ⚠ | Ana sayfa hesabı kod incelemesi ve ekran taramasıyla; ayrı otomatik testi yok |
| Deneme neti | ✅ | Birim testi; arayüzde 28,00 net |
| Konu bazlı analiz, öncelik sırası | ✅ | Birim testi ve cihaz testi |
| Tekrar tarihleri | ✅ | 1-3-7-14-30, hatırladım/hatırlayamadım |
| Alana göre ders listesi | ⚠ | Plan ayarlarında alana göre listeleniyor; ekran taraması yalnızca SAY profiliyle |

### Veri güvenliği

| Madde | Durum | Dayanak |
|---|---|---|
| Yedek oluşturma ve geri yükleme | ✅ | Fotoğraflı ZIP gidiş-dönüş |
| Bozuk JSON | ✅ | Bütünlük denetimi reddediyor |
| Bozuk ZIP | ✅ | Bozuk ek dosyası, eksik ek, aynı adlı giriş reddediliyor |
| Çok büyük ZIP, ZIP bombası | ✅ | Girdi, tek dosya, açılmış toplam, JSON ve dosya sayısı sınırları (testte küçültülmüş sınırlarla, aynı kod) |
| ZIP yol aşımı | ✅ | Uygulama ve Android 14+ ayrı ayrı reddediyor |
| Eksik PDF, eksik fotoğraf | ✅ | Uyarıyla aktarılıyor, yedek reddedilmiyor |
| Yarım kalan geri yükleme | ⚠ | Güvenlik kopyasından eski hâle dönüş testi geçti. Dosya kopyalama sırasında yapay hata üretilmedi |
| Geçici dosya kalmaması | ✅ | Cihaz testi |
| Room migration | ✅ | 1 → 2, 2 → 3, 1 → 3 |

### Uygulama kullanımı

| Madde | Durum | Dayanak |
|---|---|---|
| İlk kurulum, profil | ✅ | Arayüz akışı |
| Çalışma kaydı, görev tamamlama | ✅ | Veri ve arayüz testleri |
| Deneme oluşturma, yanlış konuların deftere geçmesi | ✅ | `LearningFlowTest` |
| Yanlış defteri kaydı | ✅ | Arayüz ve veri testleri |
| Arka plana alma, yeniden başlatma | ✅ | Zorla durdurma, cihaz yeniden başlatma |
| Gece yarısı tarih değişmesi | ⚠ | Gün yenileme işlevi test edildi; gerçek gece yarısı geçişi beklenmedi |
| Saat dilimi değişikliği | ❌ | Kod var; emülatörde saat dilimi değiştirilerek denenmedi |
| Bildirim izni reddi | ❌ | Denenmedi |
| Widget güncellemesi | ⚠ | Çizim test edildi; gerçek ana ekrana eklenmedi |
| İnternet yokken kullanım | ✅ | Tüm veri ve arayüz testleri uçak modunda |

### Reklam ve satın alma

| Madde | Durum | Dayanak |
|---|---|---|
| Test reklamı | ✅ | Android 14 ve 16 Play imajı |
| Reklam yüklenemedi | ⚠ | Kod yolu var; sunucudan hata üretilmedi |
| UMP rıza durumu | ⚠ | Emülatör konumunda form gerekmedi; Avrupa formu denenmedi |
| Reklam alanı kaydırma davranışı | ✅ | 2.0.0'da 396 kaydırma testi; koruma kodu değişmedi, 3 dokunma testi bu turda geçti |
| İmzasız, başka ürün, tamamlanmamış satın alma reddi | ✅ | Birim ve cihaz testi |
| Açık anahtar eksikliği | ✅ | Hak açılmıyor; satın alma ekranı kapalı olduğunu söylüyor |
| Gerçek satın alma, iptal, bekleyen ödeme, geri yükleme, yeniden sorgu | ❌ | Play ürünü ve lisans test hesabı gerekir. **Para harcanmadı** |

Canlı reklama hiç tıklanmadı; yalnızca Google test kimlikleri kullanıldı.

## Ara sıra görülen olaylar

| Olay | Sıklık | Durum |
|---|---|---|
| Android 14 Play imajında release açılışından sonra uygulama kapatıldı | 1 kez (son tur) | Android kaydı: `depends on provider com.google.android.gms/.fonts.provider.FontsProvider in dying proc`. Google Play Hizmetleri yeniden başlarken Android ona bağlı uygulamaları kapatıyor; uygulama çökmesi değil. Aynı imajda 6/6 temiz kurulum geçti |
| Android 16 Play imajında test süreci açılamadı | Önceki bir turda 1 kez | Aynı anda sistem arayüzü ve Play Hizmetleri de açılamadı (aşırı yüklü emülatör); ikinci denemede geçti. Son turda görülmedi |

## Denenemeyenlerin kısa listesi

Gerçek telefon, Android 9, 10, 12, 15, gerçek satın alma ve geri yükleme, üretim reklam kimlikleri, Avrupa rıza formu, Android dosya seçicileri, ana ekrana bileşen ekleme, bildirimin saatinde gelmesi, saat dilimi değişikliği, bildirim izni reddi, gerçek 2.0.0 kurulumunun üstüne 2.1.0, kendi anahtarınızla `yayin-aab` işi. Yapılacaklar: `docs/kalan-manuel-isler.md`.
