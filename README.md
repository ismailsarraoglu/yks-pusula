# YKS Pusula: Plan ve Takip

Sınava hazırlığını tek yerden yönet. Android için yerel çalışan YKS plan ve takip uygulaması.
Hesap ve sunucu yoktur, çalışma verileri cihazda kalır. Temel özellikler internetsiz çalışır.
İnternet yalnızca reklam, Play satın alma, rıza formu ve kullanıcının açtığı resmi bağlantılar içindir.

## Belgeler

| Dosya | İçerik |
|---|---|
| `TESLIM-RAPORU.md` | Son rapor ve kabul listesi |
| `docs/test-raporu.md` | Test sonuçları |
| `docs/degisiklik-raporu.md` | Korunan, geliştirilen, kaldırılan, eklenen |
| `docs/veritabani-gecisi.md` | Room geçişi |
| `docs/tasarim-sistemi.md` | Renkler ve bileşenler |
| `docs/kalan-manuel-isler.md` | Yayın için size kalan işler |
| `play/` | Mağaza metinleri, görseller, gizlilik politikası, veri güvenliği, AdMob ve Play talimatları, rakip analizi |

## Android Studio ile açma

**Open** ile bu klasörü açın. JDK 17 kullanılır. Üretim kimlikleri ve yükleme anahtarı `keystore.properties` dosyasından okunur; bu dosya depoya eklenmez. Dosya yoksa Google test reklam kimlikleri ve `app/signing` içindeki yan yükleme anahtarı kullanılır.

## Test

`./gradlew testDebugUnitTest` birim testlerini, `./gradlew connectedDebugAndroidTest` cihaz testlerini çalıştırır. Bulut derleme `.github/workflows/derleme.yml` içindedir; ileti `[tam]` içerirse sürüm matrisi de çalışır. `.github/workflows/yayin.yml` Play için imzalı AAB üretir.

## Yapı

```
app/src/main/java/app/ykspusula/
  data/       Room varlıkları, geçişler, depo, yedekleme
  domain/     Hesaplama, plan, Pusula Koç, analiz
  monetize/   Reklam, satın alma, hak yönetimi
  notify/     Bildirimler
  report/     PDF ve görsel raporlar
  widget/     Ana ekran bileşenleri
  ui/         Compose ekranları, tema, bileşenler
content/      İçerik kaynakları
tools/        Derleme ve test betikleri
```
