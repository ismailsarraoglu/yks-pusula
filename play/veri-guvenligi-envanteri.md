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
| Cihaz veya diğer kimlikler | AdMob'un güncel beyanına göre işaretleyin | AdMob'un güncel beyanına göre | Reklam, sahtecilik önleme | AdMob. **Reklam kimliği (AAID) kullanılmaz:** AD_ID izni yok ve her istek çocuk işlemiyle yapılır |
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
| ~~com.google.android.gms.permission.AD_ID~~ | Reklam SDK'sı ekler, 2.1.2'de manifestten kaldırıldı | Play Console'da reklam kimliği beyanı: kullanılmıyor |
| FOREGROUND_SERVICE, WAKE_LOCK | Reklam SDK'sı bağımlılıkları | Uygulama kodu kullanmaz; SDK kütüphaneleri ekler |
| app.ykspusula.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION | AndroidX | Uygulama içi yayın koruması |

Geniş dosya izni, kamera izni veya konum izni istenmez. Fotoğraf ve PDF sistem seçicisiyle alınır.

## Hedef kitle ve Families politikası (2.1.2)

Ayrıntı ve kaynaklar: `docs/gizlilik-ve-reklam-raporu.md`.

- Hedef kitle gerçeğe göre seçilmeli: 16-17 ve 18+; 9. ve 10. sınıf kullanıcılar da varsa 13-15. Karma kitle olduğu için Families politikası uygulanır.
- Uygulama yaş sormaz. Her kullanıcı yaşı bilinmeyen sayılır ve bütün reklamlar çocuk korumasıyla gösterilir: Families sertifikalı Google Mobile Ads SDK, kişiselleştirilmemiş reklam, reklam kimliği yok, içerik derecesi G.
- **Risk:** Families politikası karma kitle için tarafsız yaş ekranından açıkça söz ediyor; herkese çocuk korumasının bunun yerine kabul edileceği garanti değil.
- Uygulama yaş, kimlik, telefon veya e-posta istemez. Bu beyanlar hukuki danışmanlık değildir.

## Sürüm 2.1.0'da değişen

- Yeni veri türü toplanmadı, yeni SDK veya izin eklenmedi. Saat ve saat dilimi değişikliğinde hatırlatıcıları yeniden kurmak için sistem yayını dinlenir; bu bir izin gerektirmez.
- Satın alma doğrulaması artık Play açık anahtarı olmadan hak vermez. Satın alma kaydı (JSON ve imza) yalnızca cihazda saklanır.

## Sürüm 2.1.2'de değişen

- Yaş ekranı kaldırıldı; cihazda kalan doğum ayı ve yılı silinir.
- Reklam SDK'sı `play-services-ads` (Families sertifikalı). Her istek çocuk işlemiyle, içerik G.
- `AD_ID` izni kaldırıldı: reklam kimliği beyanı "kullanılmıyor".

## Sürüm 2.1.1'de değişen

- Tarafsız yaş ekranı: doğum ayı ve yılı yalnızca cihazda saklanır, yedeğe girmez. Reklam ve UMP rıza SDK'sı yalnızca 18 yaşını doldurmuş kullanıcı için başlatılır.
- Satın alma doğrulaması kapalı varsayılanla çalışır: paket adı, satın alma durumu, ürün kimliği ve belirteç zorunludur ve tutarlı olmalıdır.
- Yeni izin veya SDK eklenmedi.
