# Sizin yapmanız gereken işler

Bu işler hesabınıza, kimliğinize veya gerçek cihaza bağlı olduğu için tarafımdan yapılamaz. Sırayla ilerleyin.

## Yayın öncesi

1. **Google Play Geliştirici hesabı** açın ve kimlik doğrulamasını tamamlayın.
2. **Yükleme anahtarı** oluşturun ve iki ayrı yerde yedekleyin. Adımlar: `play/play-yayin-talimat.md`. 2.1.0'dan itibaren Play derlemesi bu anahtar olmadan üretilemez.
   - **Eski yan yükleme test anahtarı (`yks-pusula-sideload-YEDEK.p12`) artık kullanılmaz.** 7 Kasım 2026'da depo kısa süre herkese açıkken bu anahtar eski Git geçmişinde görünür durumdaydı. Geçmiş temizlendi, ama anahtarı ele geçmiş sayın: GitHub'a `SIDELOAD_P12_B64` olarak eklemeyin, test APK'larını onunla imzalamayın. Telefonunuzdaki eski test sürümünü güncellemek için yedek alıp kaldırın, yeni APK'yı kurun, yedeği geri yükleyin. Play sürümü bundan etkilenmez.
3. **AdMob hesabı** açın; uygulama, yerel reklam birimi ve GDPR iletisi oluşturun. Adımlar: `play/admob-talimat.md`. Gerçek reklam kimlikleri bu teslimde yoktur.
4. **Gizlilik politikası:** `play/gizlilik-politikasi.html` içindeki `[GELİŞTİRİCİ ADI]` ve `[GELİŞTİRİCİ E-POSTA ADRESİ]` alanlarını doldurun, herkese açık bir adreste yayımlayın, adresi Play Console'a girin.
5. **Kendi anahtarınızla AAB üretin.** GitHub'daki `yayin-aab` işi veya kendi bilgisayarınız. Gereken yedi değer: yükleme anahtarı dosyası ve üç parola/ad, `ADMOB_APP_ID`, `ADMOB_NATIVE_ID`, `PLAY_PUBLIC_KEY`. Bu teslimde Play'e yüklenecek AAB yoktur; anahtarınız olmadan üretilemez.
6. **Play Console'da uygulama oluşturun.** Mağaza metinleri `play/magaza-metinleri.md`, görseller `play/gorseller/`. Ekran görselleri örnek veriyle hazırlandı.
7. **`remove_ads_lifetime` ürününü** oluşturun ve fiyatını belirleyin.
8. **Beyanları doldurun:** veri güvenliği, reklam kimliği, hedef kitle, içerik derecelendirmesi. Dayanak: `play/veri-guvenligi-envanteri.md`.
   - **Hedef kitle:** gerçek kitlenizi seçin: 16-17 ve 18+; 9. ve 10. sınıf kullanıcılar da varsa 13-15. Families politikası uygulanır. 2.1.2'de yaş ekranı yok; bütün reklamlar çocuk korumasıyla gösterilir. Play incelemesinin bunu tarafsız yaş ekranı yerine kabul edeceği garanti değil (`docs/gizlilik-ve-reklam-raporu.md`).
   - **Reklam kimliği beyanı:** "kullanılmıyor". 2.1.2'de AD_ID izni yok.
   - **İçerik derecelendirmesi anketi:** uygulamada reklam olduğunu belirtin.
   - Families politikası ve yaş konusundaki hukuki yükümlülükler (KVKK, farklı ülkeler) için bir hukukçuya danışın. Google da yaş işlemi seçimi için hukuk danışmanı öneriyor.

## Gerçek cihaz ve Play ile denenmesi gerekenler

Bunlar otomatik testlerde denenemedi. Dahili test kanalında lisans test hesabıyla yapın, gerçek para harcanmaz.

- [ ] Fiyat Play'den doğru görünüyor
- [ ] Satın alma başarılı, reklamlar kayboluyor
- [ ] Satın alma iptal edilince hak açılmıyor
- [ ] Yavaş test kartıyla bekleyen ödemede hak açılmıyor, ödeme tamamlanınca açılıyor
- [ ] Uygulama silinip yeniden kurulunca satın alma geri yükleniyor
- [ ] Uçak modunda reklamsız hak korunuyor
- [ ] Avrupa Ekonomik Alanı konumunda rıza formu çıkıyor, Profil'den yeniden açılıyor
- [ ] Gerçek reklam kimlikleriyle reklam görünüyor (kendi reklamınıza tıklamayın). Reklamlar kişiselleştirilmemiş ve G içerikli olmalı
- [ ] 2.1.1 kurulu telefona 2.1.2 kurulunca doğum tarihi kartı görünmüyor, veriler duruyor
- [ ] YKS bilgi merkezinde ÖSYM kartı telefonunuzun tarayıcısında ÖSYM'nin "Sınav Soru ve Cevapları" sayfasını açıyor; sayfadan YKS yılı seçilip TYT, AYT ve YDT temel soru kitapçıklarına ve cevap anahtarlarına ulaşılabiliyor. Emülatörde yalnızca tarayıcının bu adresle açıldığı doğrulandı; sayfanın içeriği görülmedi
- [ ] Play Billing gerçek bağlantı: emülatörde Google hesabı olmadığı için hiç kurulamadı. Dahili testte fiyatın görünmesi ilk denetim olsun
- [ ] Test reklamı görünürken parmağınızı reklamın üstüne koyup sayfayı hızlıca kaydırın, reklam açılmamalı. Bunu Google test kimlikli sürümle yapın, gerçek reklamda denemeyin
- [ ] Yanlış defterinde fotoğraf seçme, PDF ekleme, yedeği dosyaya kaydetme ve dosyadan geri yükleme (Android dosya seçicisi)
- [ ] Ana ekrana dört bileşeni ekleme
- [ ] Bildirimlerin seçilen saatte gelmesi, telefon yeniden başlayınca da devam etmesi
- [ ] Rapor paylaşımı (WhatsApp, Drive vb.)
- [ ] Bildirim izni reddedildiğinde uygulama normal çalışıyor
- [ ] Sürüm 1.0.0 kurulu telefonda sürüm 2 APK'sı üstüne kuruluyor, veriler duruyor
- [ ] Android 15 gerçek telefonda genel kullanım (2.1.1'de Android 15 yalnızca emülatörde denendi: `docs/android15-test-raporu.md`); Android 9, 10 ve 12 hiç denenmedi
- [ ] Sürüm 2.0.0 veya 2.1.0 kurulu telefonda 2.1.1 üstüne kuruluyor, veriler duruyor (aynı imzayla kurulmuşsa)
- [ ] Geri yükleme sırasında telefon kapanırsa (ör. pil biterse) sonraki açılışta veriler eski hâline dönüyor. Emülatörde işlemin ortasında kesilme benzetimiyle denendi (süreç gerçekten öldürülmedi); gerçek kapanmayla denenmedi
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
