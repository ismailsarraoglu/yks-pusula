# Tasarım sistemi

Kaynak kod: `app/src/main/java/app/ykspusula/ui/theme/Theme.kt`, `ui/components/Components.kt`, `ui/components/Premium.kt`.
Ekranlar renk ve boyutu doğrudan yazmaz, buradaki jetonları kullanır. Varsayılan tema koyudur; açık tema profil ayarlarında durur.

## Renkler (koyu tema)

| Jeton | Değer | Kullanım |
|---|---|---|
| `bg` | #0B0F18 | Ekran zemini |
| `bg2` | #111827 | Alt gezinme, ikinci zemin |
| `card` | #192235 | Kart |
| `card2` | #202B40 | Kart içi yüzey |
| `accent` | #318BFF | Ana eylem |
| `accentLight` | #58B5FF | Vurgu, seçili sekme |
| `purple` | #8452F5 | İkincil vurgu |
| `purpleLight` | #A47AFF | İkincil vurgu açık |
| `text` | #F7F8FC | Ana metin |
| `muted` | #A7B1C5 | İkincil metin |
| `line` | #303B50 | Kenarlık |
| `success` | #36C98F | Başarı |
| `warning` | #FFB84D | Uyarı |
| `error` | #FF6475 | Hata |

## Biçim

- Kartlar 20-26 dp köşe yarıçapı kullanır.
- Sayılar `NumberStyle` ile tablo rakamıdır; net değerleri alt alta hizalanır.
- Degradeler `Gradients` nesnesindedir ve yalnızca öne çıkan eylemlerde kullanılır.
- Yazı boyutları `AppType` içindedir; sistem yazı boyutu büyütülünce kaydırılabilir düzenler bozulmaz (1,5 kat ile test edildi).

## Bileşenler

PrimaryGradientCard, FeatureShortcutCard, ExamTypeCard, ExpandableToolsCard, RecentExamCard, SectionHeader, ProgressCard, BottomNavigationBar, EmptyStateView, MotivationCard, SourceCard, StudyTaskCard, PurchaseCard, NativeAdContainer (`AdSlot`).
