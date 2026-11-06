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

## Hedef kitle ve Families politikası

Kaynak: [Hedef kitle ve uygulama içeriği ayarları](https://support.google.com/googleplay/android-developer/answer/9867159?hl=en), 6 Kasım 2026'da okundu.

- Google'a göre 5 yaş altı, 6-8 ve 9-12 grupları çoğu ülkede çocuk sayılır. 13-15 ve 16-17 grupları bazı ülkelerde çocuk sayılabilir.
- Hedef kitlede çocuk yaş grubu varsa Families şartları uygulanır: reklamlar yalnızca Families sertifikalı reklam SDK'larından gelmelidir. Çocuk ve büyükler birlikte hedefleniyorsa tarafsız yaş ekranı gerekir.
- **Öneri: hedef yaş grubu olarak 16-17 ve 18+ seçin.** YKS öğrencilerinin büyük kısmı bu gruptadır. Bu seçimle uygulama çocuklara yönelik sayılmaz, tarafsız yaş ekranı gerekmez ve uygulamaya yaş ekranı eklenmedi.
- 13-15 grubunu da seçmek isterseniz (9. ve 10. sınıf öğrencileri), bazı ülkelerde bu grup çocuk sayılabileceği için AdMob'da "çocuklara yönelik işlem" ve "reşit olmayan kullanıcı" etiketlerini ve Families şartlarını yeniden değerlendirin. Bu durumda `AdsManager` yapılandırması değiştirilmelidir.
- Mağaza görselleri ve metinleri çocuklara hitap eden çizgi karakter içermez.
- Reklam içerik sınırı kodda PG (`MAX_AD_CONTENT_RATING_PG`) olarak ayarlı. AdMob panelinde de aynı sınırı seçin.
- Uygulama yaş, kimlik, telefon veya e-posta istemez.
- Bu beyanlar hukuki danışmanlık değildir; Türkiye'de reşit olmayanların verileriyle ilgili yükümlülükler için bir uzmana danışın.

## Sürüm 2.1.0'da değişen

- Yeni veri türü toplanmadı, yeni SDK veya izin eklenmedi. Saat ve saat dilimi değişikliğinde hatırlatıcıları yeniden kurmak için sistem yayını dinlenir; bu bir izin gerektirmez.
- Satın alma doğrulaması artık Play açık anahtarı olmadan hak vermez. Satın alma kaydı (JSON ve imza) yalnızca cihazda saklanır.
