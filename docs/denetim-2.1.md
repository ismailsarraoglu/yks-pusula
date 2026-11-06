# 2.1.0 başlangıç denetimi

Tarih: 6 Kasım 2026. Başlangıç noktası: `main` dalı, `7c24848` (2.0.0 teslimi). Çalışma dalı: `surum-2.1`. 2.0.0 hâline dönmek gerekirse: `7c24848`.

Prompttaki eski bulgular kod üzerinde yeniden kontrol edildi. "Doğrulandı" sorunun bugün kodda hâlâ olduğunu gösterir.

| # | Bulgu | Dosya | Durum |
|---|---|---|---|
| A1 | Release derlemesi yükleme anahtarı yoksa sessizce yan yükleme anahtarına dönüyor | `app/build.gradle.kts` | Doğrulandı |
| A2 | Yan yükleme anahtarı (`.p12`) ve parolası depoda ve kaynak ZIP'inde | `app/signing/`, `app/build.gradle.kts` | Doğrulandı |
| A3 | Release derlemesi gerçek reklam kimliği yoksa Google test kimliğine dönüyor | `app/build.gradle.kts` | Doğrulandı |
| B1 | Play açık anahtarı boşsa satın alma imzası denetlenmeden geçerli sayılıyor | `monetize/Entitlement.kt` `isValid` | Doğrulandı, güvenlik açığı |
| B2 | Ürün kimliği JSON metninde `contains` ile aranıyor; `productId` alanı çözümlenmiyor | `monetize/Entitlement.kt` | Doğrulandı, zayıf denetim |
| B3 | Bekleyen, tamamlanan, iptal ayrımı; onaylama (acknowledge); açılışta yeniden sorgu | `monetize/Billing.kt` | Kod var, doğru görünüyor. Gerçek Play ile denenmedi |
| C1 | Yerel reklam alanı kaydırılabilir içerikte, üstüne bindirilmiyor; sürükleme koruması 2.0.0'da eklendi | `ui/components/AdSlot.kt`, `AdTouchGuard.kt` | Sorun yok |
| C2 | Reklam içerik düzeyi PG ile sınırlı | `monetize/Ads.kt` | Var |
| D1 | Gizlilik politikasında `[GELİŞTİRİCİ E-POSTA ADRESİ]` yer tutucusu | `play/gizlilik-politikasi.html` | Doğrulandı, kullanıcı dolduracak |
| P1 | Plan motoru blok sayısını `günlük dakika / 50` ile tamsayıya yuvarlıyor: 240 dk hedef 200 dk, 30 dk hedef 50 dk planlıyor; tekrar bloğu da aynı süreden | `domain/Logic.kt` `PlanGenerator.generate` | Doğrulandı |
| P2 | Ana sayfa "Süre" alanı `maxOf(gerçek dakika, tamamlanan görev dakikası)` gösteriyor; çalışılmayan dakika çalışma gibi görünebiliyor | `ui/screens/HomeScreens.kt:206` | Doğrulandı |
| P3 | Ekranlar tarihi `remember { today() }` ile bir kez alıyor; gece açık kalan uygulama dünü gösteriyor | Home, Study, Stats, Coach, Extra, Plan, Profile ekranları | Doğrulandı (7 yer) |
| P4 | Saat dilimi veya saat değişince hatırlatmalar yeniden planlanmıyor | `notify/Notifier.kt`, manifest | Doğrulandı |
| P5 | Deneme sonucunda "zorlandığım konular" tekrar listesine giriyor ama yanlış defterine kayıt açmıyor; yanlış/boş ayrımı yok | `data/Repository.kt` `saveExamResults` | Doğrulandı |
| Y1 | Gelen yedek dosyası boyut sınırı olmadan önbelleğe kopyalanıyor; düz JSON `readText` ile sınırsız okunuyor | `data/Backup.kt` `load` | Doğrulandı |
| Y2 | ZIP içinde dosya sayısı ve açılmış toplam boyut sınırı yok; yalnızca tek dosya 200 MB sınırı var | `data/Backup.kt` | Doğrulandı |
| Y3 | Eklerin (fotoğraf, PDF) bütünlük özeti yok; eksik fotoğraf denetimi yok | `data/Backup.kt` | Doğrulandı |
| Y4 | Geri yükleme öncesi güvenlik kopyası yalnızca veritabanı JSON'u; fotoğraf ve PDF'ler korunmuyor | `data/Backup.kt` `safetyCopy` | Doğrulandı |
| Y5 | Veritabanı değiştirildikten sonra ekler kopyalanıyor; kopyalama hata verirse geri dönüş yok, var olan aynı adlı dosyaların üstüne yazılıyor | `data/Backup.kt` `restore` | Doğrulandı |
| Y6 | Yol aşımı (path traversal) koruması | `data/Backup.kt` `safeEntry` | Var, yeterli |

## İçerik sayımları (`base.json`, yeniden hesaplandı)

| Alan | Sayı |
|---|---|
| Ders | 26 (TYT 10, AYT 11, YDT 5) |
| Konu | 261 |
| Boş konu anlatımı | 25 (Almanca, Fransızca, Rusça, Arapça 6'şar; AYT Biyoloji 1) |
| 150 karakterden kısa anlatım | 204 |
| 150-500 karakter anlatım | 32 |
| 500 karakteri aşan anlatım | 0 |
| Boş örnek | 40 |
| Formül kaydı | 236 |
| Bilgi kartı | 411 (135'i İngilizce kelime) |

Sonuç: konu anlatımları **kısa özettir**, kapsamlı ders anlatımı değildir. Uygulamada bu şekilde adlandırılmalı. İngilizce dışındaki YDT dillerinde konu anlatımı yoktur.

## Soru bankası

2.0.0'da kaldırıldı. Yeniden doğrulandı: `base.json` içinde soru alanı yok, uygulamada soru çözme ekranı yok.
