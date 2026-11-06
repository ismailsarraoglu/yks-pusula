# Sizin yapmanız gereken işler

Bu işler hesabınıza, kimliğinize veya gerçek cihaza bağlı olduğu için tarafımdan yapılamaz. Sırayla ilerleyin.

## Yayın öncesi

1. **Google Play Geliştirici hesabı** açın ve kimlik doğrulamasını tamamlayın.
2. **Yükleme anahtarı** oluşturun ve iki ayrı yerde yedekleyin. Adımlar: `play/play-yayin-talimat.md`. 2.1.0'dan itibaren Play derlemesi bu anahtar olmadan üretilemez.
   - İsteğe bağlı: size gönderilen `yks-pusula-sideload-YEDEK.p12` dosyasını güvenli bir yerde saklayın. Telefonunuzdaki eski test sürümünün üstüne kurulum için GitHub'a `SIDELOAD_P12_B64` ve `SIDELOAD_STORE_PASSWORD` gizli değerlerini ekleyebilirsiniz (aynı belgede).
3. **AdMob hesabı** açın; uygulama, yerel reklam birimi ve GDPR iletisi oluşturun. Adımlar: `play/admob-talimat.md`. Gerçek reklam kimlikleri bu teslimde yoktur.
4. **Gizlilik politikası:** `play/gizlilik-politikasi.html` içindeki `[GELİŞTİRİCİ ADI]` ve `[GELİŞTİRİCİ E-POSTA ADRESİ]` alanlarını doldurun, herkese açık bir adreste yayımlayın, adresi Play Console'a girin.
5. **Kendi anahtarınızla AAB üretin.** GitHub'daki `yayin-aab` işi veya kendi bilgisayarınız. Gereken yedi değer: yükleme anahtarı dosyası ve üç parola/ad, `ADMOB_APP_ID`, `ADMOB_NATIVE_ID`, `PLAY_PUBLIC_KEY`. Bu teslimde Play'e yüklenecek AAB yoktur; anahtarınız olmadan üretilemez.
6. **Play Console'da uygulama oluşturun.** Mağaza metinleri `play/magaza-metinleri.md`, görseller `play/gorseller/`. Ekran görselleri örnek veriyle hazırlandı.
7. **`remove_ads_lifetime` ürününü** oluşturun ve fiyatını belirleyin.
8. **Beyanları doldurun:** veri güvenliği, reklam kimliği, hedef kitle (öneri: 16-17 ve 18+), içerik derecelendirmesi. Dayanak: `play/veri-guvenligi-envanteri.md`.

## Gerçek cihaz ve Play ile denenmesi gerekenler

Bunlar otomatik testlerde denenemedi. Dahili test kanalında lisans test hesabıyla yapın, gerçek para harcanmaz.

- [ ] Fiyat Play'den doğru görünüyor
- [ ] Satın alma başarılı, reklamlar kayboluyor
- [ ] Satın alma iptal edilince hak açılmıyor
- [ ] Yavaş test kartıyla bekleyen ödemede hak açılmıyor, ödeme tamamlanınca açılıyor
- [ ] Uygulama silinip yeniden kurulunca satın alma geri yükleniyor
- [ ] Uçak modunda reklamsız hak korunuyor
- [ ] Avrupa Ekonomik Alanı konumunda rıza formu çıkıyor, Profil'den yeniden açılıyor
- [ ] Gerçek reklam kimlikleriyle reklam görünüyor (kendi reklamınıza tıklamayın)
- [ ] Test reklamı görünürken parmağınızı reklamın üstüne koyup sayfayı hızlıca kaydırın, reklam açılmamalı. Bunu Google test kimlikli sürümle yapın, gerçek reklamda denemeyin
- [ ] Yanlış defterinde fotoğraf seçme, PDF ekleme, yedeği dosyaya kaydetme ve dosyadan geri yükleme (Android dosya seçicisi)
- [ ] Ana ekrana dört bileşeni ekleme
- [ ] Bildirimlerin seçilen saatte gelmesi, telefon yeniden başlayınca da devam etmesi
- [ ] Rapor paylaşımı (WhatsApp, Drive vb.)
- [ ] Bildirim izni reddedildiğinde uygulama normal çalışıyor
- [ ] Sürüm 1.0.0 kurulu telefonda sürüm 2 APK'sı üstüne kuruluyor, veriler duruyor
- [ ] Android 15 veya başka denenmemiş sürümlerde genel kullanım
- [ ] Sürüm 2.0.0 kurulu telefonda 2.1.0 üstüne kuruluyor, veriler duruyor (aynı imzayla kurulmuşsa)
- [ ] Telefonun saat dilimini değiştirince hatırlatma yeni saatte geliyor
- [ ] Uygulama gece açık kalıp sabah açıldığında yeni günün görevleri görünüyor
- [ ] Denemede yanlış konu girince yanlış defterinde görünüyor

## Yayın kuralı

13 Kasım 2023'ten sonra açılan kişisel hesaplarda üretime çıkmadan önce kapalı testte en az 12 kişinin 14 gün kesintisiz katılması gerekir. Takvimi buna göre planlayın.

## Yıllık bakım

- 2027 sınav tarihleri haber kaynaklarından alındı. ÖSYM kılavuzu yayımlanınca `Sınav ayarları` verisini karşılaştırın.
- OBP katsayılarından ×0,12, ×0,06 ve ek puan MEB 2026 rehberinden doğrulandı. Diploma notu ×5 yalnızca ikincil kaynaklarda bulundu; kılavuzla doğrulayın.
- Üniversite verisi uygulamaya gömülü değildir; kullanıcı girer veya kaynaklı içerik paketi yükler.

## Sürüm 1.0 kullanıcıları

Daha önce dışarıdan kurulan APK, Play sürümüyle güncellenemez çünkü imzalar farklıdır. Yedek alıp kaldırın, Play'den kurun, yedeği geri yükleyin. Sürüm 2 APK'sı aynı yan yükleme imzasıyla sürüm 1'in üstüne kurulabilir.
