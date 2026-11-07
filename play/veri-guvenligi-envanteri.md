# Google Play Veri Güvenliği teknik envanteri

Bu tablo uygulamanın gerçek davranışından çıkarıldı. SDK davranışları Google'ın belgelerine dayanır. Formu doldurmadan önce güncel AdMob ve Play Billing veri beyanlarıyla karşılaştırın.

| Soru | Yanıt | Dayanak |
|---|---|---|
| Veri toplanıyor veya paylaşılıyor mu? | Evet (yalnızca üçüncü taraf SDK'lar) | AdMob, UMP, Play Billing |
| Tüm veriler aktarım sırasında şifreleniyor mu? | Evet | Google SDK'ları HTTPS kullanır; uygulama kendi sunucusuna veri göndermez |
| Kullanıcı veri silme isteyebilir mi? | Evet, uygulama içinden | Verileri sıfırla; uygulamayı kaldırma |
| Hesap oluşturma | Yok | Uygulamada hesap yoktur |

## SDK kaynaklı veri türleri

| Veri türü | Toplanıyor | Paylaşılıyor | Amaç | SDK |
|---|---|---|---|---|
| Cihaz veya diğer kimlikler (reklam kimliği) | Evet | Evet | Reklam, sahtecilik önleme | AdMob |
| Uygulama etkileşimleri (reklam gösterimi, tıklama) | Evet | Evet | Reklam, analiz | AdMob |
| Yaklaşık konum (IP'den) | Evet | Evet | Reklam | AdMob |
| Teşhis verileri (kilitlenme, performans) | Evet | Evet | Reklam, analiz | AdMob |
| Satın alma geçmişi | Evet | Hayır | Uygulama işlevi | Play Billing |

## Cihazda kalan veriler (toplanmıyor)

Bu veriler cihazdan çıkmaz, Veri Güvenliği formunda "toplanıyor" sayılmaz: profil adı, doğum ayı ve yılı (yaş ekranı), sınıf, hedefler, çalışma kayıtları, deneme sonuçları, kaynaklar, yanlış kayıtları ve fotoğraflar, notlar, PDF dosyaları.

## İzinler (APK'dan okunan gerçek liste CI çıktısında `logs/izinler.txt`)

| İzin | Kaynak | Neden |
|---|---|---|
| INTERNET, ACCESS_NETWORK_STATE | Uygulama | Reklam, Play satın alma, rıza formu |
| POST_NOTIFICATIONS | Uygulama | Yerel hatırlatıcılar |
| RECEIVE_BOOT_COMPLETED | Uygulama | Yeniden başlatma sonrası hatırlatıcıları kurmak |
| com.android.vending.BILLING | Play Billing | Tek seferlik satın alma |
| com.google.android.gms.permission.AD_ID | Reklam SDK'sı | Reklam kimliği. Play Console'da "Reklam kimliği kullanılıyor" beyanı gerekir |
| FOREGROUND_SERVICE, WAKE_LOCK | Reklam SDK'sı bağımlılıkları | Uygulama kodu kullanmaz; SDK kütüphaneleri ekler |
| app.ykspusula.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION | AndroidX | Uygulama içi yayın koruması |

Geniş dosya izni, kamera izni veya konum izni istenmez. Fotoğraf ve PDF sistem seçicisiyle alınır.

## Hedef kitle ve Families politikası

Kaynaklar: [Hedef kitle ve uygulama içeriği](https://support.google.com/googleplay/android-developer/answer/9867159?hl=en), [Families politikası](https://support.google.com/googleplay/android-developer/answer/9893335?hl=en), [Families Self-Certified Ads SDK listesi](https://support.google.com/googleplay/android-developer/answer/9283445?hl=en), [Next-Gen SDK yaş işlemi](https://developers.google.com/admob/android/next-gen/targeting). 7 Kasım 2026'da okundu. Ayrıntılı değerlendirme: `docs/gizlilik-ve-reklam-raporu.md`.

- 13-15 ve 16-17 grupları bazı bölgelerde çocuk sayılabilir. YKS kullanıcılarının önemli kısmı 18 yaşından küçük olduğu için hedef kitle karma kitledir. **2.1.0'daki "16-17 ve 18+ seçilirse yaş ekranı gerekmez" önerisi geri çekildi.**
- **Play Console'da gerçek kitleyi seçin:** 16-17 ve 18+; 9. ve 10. sınıf kullanıcılar da hedefleniyorsa 13-15. Families politikası uygulanır.
- Uygulama karma kitle şartları için şunları yapar: tarafsız yaş ekranı (boş ay ve yıl alanı, varsayılan yok, "Belirtmek istemiyorum" seçeneği); reklam ve rıza SDK'sı yalnızca 18 yaşını doldurmuş kullanıcı için başlatılır; 18 altındaki veya yaşını belirtmeyen kullanıcıdan reklam kimliği aktarılmaz.
- Uygulamanın reklam SDK'sı (GMA Next-Gen) Families sertifikalı listede değildir; bu yüzden çocuk sayılabilecek kullanıcılara hiç reklam gösterilmez.
- Yetişkin isteklerinde `AgeRestrictedTreatment.UNSPECIFIED` ve `MAX_AD_CONTENT_RATING_PG` kullanılır. AdMob panelinde de PG seçin.
- Veri Güvenliği formu için: AdMob verileri yalnızca 18+ kullanıcılardan toplanır. Doğum ayı ve yılı cihazdan çıkmaz, "toplanıyor" sayılmaz.
- Bu beyanlar hukuki danışmanlık değildir. Türkiye'de reşit olmayanların verileriyle ilgili yükümlülükler için bir uzmana danışın.

## Sürüm 2.1.0'da değişen

- Yeni veri türü toplanmadı, yeni SDK veya izin eklenmedi. Saat ve saat dilimi değişikliğinde hatırlatıcıları yeniden kurmak için sistem yayını dinlenir; bu bir izin gerektirmez.
- Satın alma doğrulaması artık Play açık anahtarı olmadan hak vermez. Satın alma kaydı (JSON ve imza) yalnızca cihazda saklanır.

## Sürüm 2.1.1'de değişen

- Tarafsız yaş ekranı: doğum ayı ve yılı yalnızca cihazda saklanır, yedeğe girmez. Reklam ve UMP rıza SDK'sı yalnızca 18 yaşını doldurmuş kullanıcı için başlatılır.
- Satın alma doğrulaması kapalı varsayılanla çalışır: paket adı, satın alma durumu, ürün kimliği ve belirteç zorunludur ve tutarlı olmalıdır.
- Yeni izin veya SDK eklenmedi.
