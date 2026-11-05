# AdMob kurulumu

Kod şu anda Google'ın test kimlikleriyle derlenir. Gerçek reklam için aşağıdakiler gerekir. Kimlikleri uydurmayın.

1. [admob.google.com](https://admob.google.com) üzerinde hesap açın, ödeme bilgilerini tamamlayın.
2. **Uygulamalar > Uygulama ekle**: Android, uygulama adı "YKS Pusula: Plan ve Takip". Uygulama Play'de yayımlandığında mağaza bağlantısıyla eşleştirin.
3. **Reklam birimleri > Yerel gelişmiş (Native advanced)**: bir birim oluşturun. Tek birim dört alanda da kullanılır; alan bazlı rapor isterseniz dört ayrı birim açabilirsiniz.
4. **Gizlilik ve mesajlaşma**: GDPR iletisi oluşturup yayımlayın. UMP formu bu iletiyi gösterir.
5. **Engelleme kontrolleri > İçerik sınırı**: Genel izleyici (PG) seçin. Hassas kategorileri engelleyin.
6. Proje kök klasöründe `keystore.properties` dosyası oluşturun (depoya eklenmez):

```
ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
ADMOB_NATIVE_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
ADMOB_BANNER_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
PLAY_PUBLIC_KEY=Play Console > Para kazanma > Lisanslama bölümündeki Base64 anahtar
```

7. Yeni AAB derleyin. Kendi reklamınıza tıklamayın; geliştirme sırasında test kimliklerini kullanın.

Reklam alanları: Ana Sayfa, Çalış, Denemeler listesi ve İstatistik. İlk üç açılışta reklam gösterilmez. Banner kimliği ileride banner kullanılırsa diye ayrılmıştır; mevcut sürüm yerel reklam kullanır.
