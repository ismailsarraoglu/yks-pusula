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

Bu veriler cihazdan çıkmaz, Veri Güvenliği formunda "toplanıyor" sayılmaz: profil adı, sınıf, hedefler, çalışma kayıtları, deneme sonuçları, kaynaklar, yanlış kayıtları ve fotoğraflar, notlar, PDF dosyaları.

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

## Hedef kitle

- Hedef yaş grubu olarak 13 yaş altı seçilmemelidir. Önerilen: 13-15, 16-17 ve 18+.
- Uygulama çocuklara yönelik olarak değerlendirilirse Families politikası gereği reklam kimliği iletilemez ve yalnızca sertifikalı SDK'lar ve kişiselleştirilmemiş reklamlar kullanılabilir. Bu durumda `AdsManager` yapılandırması değiştirilmelidir.
- Reklam içerik sınırı kodda PG olarak ayarlandı. AdMob panelinde de aynı sınır seçilmelidir.
- Yaş bilgisi toplanmaz. Bu beyanlar hukuki danışmanlık değildir; Türkiye'de reşit olmayanların verileriyle ilgili yükümlülükler için bir uzmana danışın.
