# YKS Pusula 2.1.2 teslim raporu

Sürüm 2.1.2 (versionCode 5). Kod: `KOD`, dal `surum-2.1.2`. Son tam test turu: `TUR`, 7 Kasım 2026. Önceki raporlar: `docs/teslim-raporu-2.1.1.md`, `docs/teslim-raporu-2.1.0.md`.

## Durum özeti

| İstek | Durum |
|---|---|
| Doğum tarihi ve yaş ekranını kaldırma | Tamamlandı |
| Reklamı bütün kitleye, Play ve AdMob kurallarıyla | Tamamlandı: Families sertifikalı SDK, her istek çocuk korumalı. **Politika riski var** (bölüm 3) |
| Günlük rota tekrarı ve diğer gerçek hatalar | Tamamlandı (bölüm 2) |
| Tasarım ve çalışan özellikler | Korundu; yeni özellik yok |
| Bütün testler, başarısız test yok | Son turda 9 işin hepsi PASS (bölüm 4) |
| Sürüm 2.1.2, versionCode 5 | Tamamlandı |
| Kaynak kod | Teslim edildi: `yks-pusula-kaynak-2.1.2.zip` |
| **İmzalı Play AAB** | **Üretilemedi.** Yükleme anahtarınız, gerçek AdMob kimlikleri ve Play lisans anahtarı yok. Uydurulmadı. Gerekenler bölüm 5'te |

Bu yüzden teslim, Play Console'a yüklenebilir paketi **henüz içermiyor**. Bölüm 5'teki değerler GitHub'a eklendiğinde AAB, hazır olan `yayin-aab` işiyle üretilir.

## 1. Değişiklikler

Ayrıntı: `docs/degisiklik-raporu-2.1.2.md`.

- Doğum tarihi kartı ve yaş mantığı kaldırıldı; 2.1.1'de girilmiş bilgi cihazdan silinir.
- Reklam SDK'sı `play-services-ads` 25.5.0 oldu (Google Play Families Self-Certified Ads SDK listesinde). Her istek çocuk işlemiyle (TFCD, TFUA), içerik G. Reklam kimliği ve AdServices izinleri kaldırıldı.
- Günlük rota tekrarı düzeltildi.
- Gizlilik politikası, veri güvenliği envanteri ve AdMob talimatı güncellendi.

## 2. Düzeltilen hatalar

| Hata | Neden | Düzeltme |
|---|---|---|
| Günlük rotada aynı önerinin birden fazla görünmesi | Dört ekran rotayı aynı anda oluşturuyordu; kontrol ile yazma arasında yarış vardı | Tek kilit, eşsizleştirme, eski kopyaların temizlenmesi |
| Yarına taşınan öneri yarının rotasını engelliyordu | Gün "rotası var" sayılıyordu | Yalnızca taşınan öğeler varsa gün yeniden oluşturulur |
| Reklam SDK'sı aileler için sertifikalı değildi | 2.1.1'de Next-Gen SDK | Sertifikalı SDK'ya geçildi |

## 3. Reklam ve politika

- **Uygulanan:** her kullanıcı yaşı bilinmeyen sayılır ve çocuklar için gereken kurallarla reklam görür: sertifikalı SDK, kişiselleştirilmemiş reklam, reklam kimliği yok, içerik G.
- **Risk:** Google Play Families politikası, karma kitleli uygulamalar için tarafsız yaş ekranından açıkça söz ediyor. Herkese çocuk koruması uygulamanın bunun yerine kabul edileceğini politika açıkça söylemiyor. Play incelemesi itiraz ederse yaş ekranı geri eklenebilir (2.1.1 kodu Git geçmişinde).
- **Play Console:**
  - Hedef kitle gerçeğe uygun seçilmeli: 16-17, 18+, kullanıcı varsa 13-15.
  - Reklam kimliği beyanı: kullanılmıyor.
  - Reklam içeriyor: evet.
- **Gelir:** reklam herkese gösterilir, ama kişiselleştirilmemiş ve G olduğu için reklam başına gelir düşüktür.
- Hukuki görüş değildir. Ayrıntı ve kaynaklar: `docs/gizlilik-ve-reklam-raporu.md`.

## 4. Testler

Ayrıntı: `docs/test-raporu.md`.

TEST_OZETI

## 5. Play'e yüklenebilir AAB için sizden gerekenler

`yayin-aab` işini 7 Kasım'da çalıştırdım (`42376915043`). Yedi gizli değerin yedisi de eksik olduğu için iş durdu.

Bu değerleri **bana yazmayın**. Doğrudan GitHub'da girin: depo > Settings > Secrets and variables > Actions > New repository secret.

| Gizli değer | Nereden |
|---|---|
| `UPLOAD_KEYSTORE_B64` | Kendi oluşturacağınız yükleme anahtarı dosyasının (.jks) Base64 hâli. Adımlar: `play/play-yayin-talimat.md`. Dosyayı iki ayrı yerde yedekleyin |
| `UPLOAD_STORE_PASSWORD` | Anahtar deposu parolası |
| `UPLOAD_KEY_ALIAS` | Anahtar adı |
| `UPLOAD_KEY_PASSWORD` | Anahtar parolası |
| `ADMOB_APP_ID` | AdMob > Uygulamalar: `ca-app-pub-…~…` |
| `ADMOB_NATIVE_ID` | AdMob > Reklam birimleri > Yerel gelişmiş: `ca-app-pub-…/…` |
| `PLAY_PUBLIC_KEY` | Play Console > uygulama > Para kazanma kurulumu > Lisanslama: Base64 açık anahtar. Önce Play Console'da uygulamayı oluşturmanız gerekir |

Ayrıca gizlilik politikasında iki alan var: `play/gizlilik-politikasi.html` dosyasında geliştirici adı ve e-posta. Politikayı herkese açık bir adreste yayımlamanız gerekir.

Değerleri ekleyip bana haber verdiğinizde yapacaklarım:
1. `yayin-aab` işini çalıştırmak.
2. İmzayı ve sürümü (2.1.2, versionCode 5) doğrulamak.
3. AAB'yi size teslim etmek.

## 6. Denenmeyenler

- **Gerçek cihaz:**
  - Hiçbir test gerçek telefonda yapılmadı. Android 9, 10 ve 12 denenmedi.
  - ÖSYM sayfasının içeriği görüntülenip denetlenmedi.
- **AdMob:**
  - Gerçek kimliklerle reklam denenmedi.
  - Avrupa rıza formu denenmedi.
  - Yalnızca Google test kimlikleri kullanıldı; reklama tıklanmadı.
- **Play satın alma:**
  - Gerçek bağlantı kurulamadı; emülatörde Google hesabı yok.
  - Satın alma, iptal, bekleyen ödeme ve geri yükleme denenmedi. Para harcanmadı.
- **Play incelemesi:** hedef kitle ve reklam kararını ancak Google'ın incelemesi kesinleştirir.

Adımlar: `docs/kalan-manuel-isler.md`.

## 7. Teslim dosyaları

| Dosya | Açıklama |
|---|---|
| `yks-pusula-kaynak-2.1.2.zip` | Kaynak kod. Anahtar, parola, `local.properties`, `.env`, derleme çıktısı içermez |
| `yks-pusula-2.1.2-test.apk` | Telefonda denemek için (release ayarlı, Google test reklamları, satın alma kapalı, geçici anahtarla imzalı). **Play'e yüklenmez** |
| Bu rapor ve `docs/` | Değişiklik, test, gizlilik ve reklam raporları |

Depo şu an herkese açık. Testler bitti; gizli yapabilirsiniz: Settings > General > Danger Zone > Change visibility > Private. AAB işi için tekrar açmanız gerekmez: o tek iş gizli depoda da birkaç dakikalık ücretsiz kotayla çalışır. Kota bu ay bittiyse yenilenmesini bekleyin ya da o iş için depoyu kısa süre açın.
