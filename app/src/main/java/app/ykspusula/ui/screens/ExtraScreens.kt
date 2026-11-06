package app.ykspusula.ui.screens

import app.ykspusula.ui.currentDay
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.BuildConfig
import app.ykspusula.data.*
import app.ykspusula.report.ReportExporter
import app.ykspusula.ui.LocalNav
import app.ykspusula.ui.app
import app.ykspusula.ui.components.*
import app.ykspusula.ui.longDate
import app.ykspusula.ui.repo
import app.ykspusula.ui.rememberUiScope
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Günün sözü, önceki sözler, favoriler, kopyalama ve paylaşma. */
@Composable
fun MotivationScreen() {
    val r = repo()
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberUiScope()
    val favs by r.user.bookmarkIds("QUOTE").collectAsStateWithLifecycle(emptyList())
    var tab by rememberSaveable { mutableStateOf("TODAY") }
    var index by rememberSaveable { mutableIntStateOf(-1) }
    val date = currentDay()
    val quotes = r.quotes
    val base = remember(date) { r.quoteOfDay(date) }
    val current = if (index in quotes.indices) quotes[index] else base
    val past = remember(date) { r.pastQuotes(30, date) }
    fun toggle(q: Quote) { scope.launch { r.toggleBookmark("QUOTE", q.id, q.id !in favs) } }
    Screen("Motivasyon", { nav.popBackStack() }) {
        LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            item { PillGroup(listOf("TODAY" to "Bugün", "PAST" to "Önceki sözler", "FAV" to "Favoriler (${favs.size})"), tab) { tab = it } }
            when (tab) {
                "TODAY" -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                        MotivationCard(current.text, current.category, current.id in favs, onFavorite = { toggle(current) }, onOpen = {})
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                            SoftButton("Kopyala", Modifier.weight(1f), icon = Icons.Rounded.ContentCopy) {
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("YKS Pusula", current.text))
                            }
                            SoftButton("Paylaş", Modifier.weight(1f), icon = Icons.Rounded.Share) { ReportExporter.shareText(context, current.text + "\n\nYKS Pusula") }
                        }
                        MainButton("Başka söz göster", Modifier.fillMaxWidth(), icon = Icons.Rounded.Refresh) {
                            val i = if (index in quotes.indices) index else quotes.indexOf(base)
                            index = (i + 1) % quotes.size
                        }
                        Muted("Günün sözü gün boyunca aynı kalır. Bütün sözler YKS Pusula tarafından yazıldı.")
                    }
                }
                "PAST" -> items(past, key = { it.first.toString() }) { (d, q) -> QuoteRow(q, longDate(d), q.id in favs) { toggle(q) } }
                else -> {
                    val list = quotes.filter { it.id in favs }
                    if (list.isEmpty()) item { EmptyStateView(Icons.Rounded.StarBorder, "Favori söz yok", "Beğendiğin sözlerdeki yıldıza dokun.") }
                    items(list, key = { it.id }) { q -> QuoteRow(q, q.category, true) { toggle(q) } }
                }
            }
        }
    }
}

@Composable
private fun QuoteRow(q: Quote, detail: String, fav: Boolean, onFav: () -> Unit) {
    YCard(padding = Space.m) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(q.text, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
                Muted(detail)
            }
            IconButton(onFav) { Icon(if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (fav) "Favorilerden çıkar" else "Favorilere ekle", tint = if (fav) Yks.c.warning else Yks.c.muted) }
        }
    }
}

/** Satın alma: yalnızca reklamları kaldırır. */
@Composable
fun RemoveAdsScreen() {
    val a = app()
    val nav = LocalNav.current
    val context = LocalContext.current
    val ui by a.monetize.billing.ui.collectAsStateWithLifecycle()
    val adsFree by a.monetize.entitlement.hasAdsFreeEntitlement.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { a.monetize.billing.connect() }
    Screen("YKS Pusula Reklamsız", { nav.popBackStack() }) {
        ScrollColumn {
            Text("YKS Pusula Reklamsız", style = MaterialTheme.typography.headlineMedium, color = Yks.c.text)
            Muted("Dikkatini dağıtmadan çalışmaya devam et.")
            PurchaseCard(adsFree)
            if (adsFree) InfoBanner("Reklamlar zaten kaldırılmış. Teşekkürler.", Icons.Rounded.CheckCircle, Yks.c.success)
            else if (!a.monetize.entitlement.purchasesEnabled) {
                InfoBanner("Bu test sürümünde satın alma kapalı. Reklamsız sürüm yalnızca Google Play'den kurulan uygulamada satın alınabilir.", Icons.Rounded.Info, Yks.c.accentLight)
            } else {
                MainButton(if (ui.price != null) "REKLAMLARI KALDIR • ${ui.price}" else "REKLAMLARI KALDIR", Modifier.fillMaxWidth(), enabled = !ui.pending && ui.price != null, icon = Icons.Rounded.WorkspacePremium) {
                    (context as? android.app.Activity)?.let { a.monetize.billing.buy(it) }
                }
                if (ui.price == null) Muted("Fiyat Google Play'den alınır. Bağlantı yoksa veya uygulama Play'den kurulmadıysa görünmez.")
                if (ui.pending) Muted("Ödemen bekleniyor. Tamamlandığında reklamlar kalkacak.")
            }
            if (ui.message.isNotEmpty()) Text(ui.message, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text)
            GroupCard {
                NavRow("Satın almaları geri yükle", "Aynı Google Play hesabıyla", Icons.Rounded.Restore, onClick = { a.monetize.billing.restore() }); Line()
                NavRow("Gizlilik politikası", null, Icons.Rounded.Policy, onClick = { nav.navigate("privacy") }); Line()
                NavRow("Satın alma koşulları", null, Icons.Rounded.Description, onClick = { nav.navigate("terms") })
            }
            Muted("Satın alma yalnızca reklamları kaldırır. Bütün eğitim özellikleri ücretsiz kullanıcılar için de aynıdır.")
        }
    }
}

@Composable
private fun PurchaseCard(owned: Boolean) {
    YCard {
        listOf("Kesintisiz çalışma", "Reklamsız bütün ekranlar", "Daha sade deneyim", "Tek seferlik ödeme", "Ömür boyu reklamsız kullanım").forEach {
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = if (owned) Yks.c.success else Yks.c.accentLight, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Space.s))
                Text(it, style = MaterialTheme.typography.bodyLarge, color = Yks.c.text)
            }
        }
    }
}

const val POLICY_TEXT = """YKS Pusula: Plan ve Takip Gizlilik Politikası

Çalışma verilerin: Profilin, çalışma planın, kaynakların, deneme sonuçların, notların, yanlış kayıtların ve fotoğrafların yalnızca cihazında saklanır. Uygulamanın sunucusu ve kullanıcı hesabı yoktur. Bu veriler geliştiriciye veya üçüncü taraflara gönderilmez.

Google AdMob: Ücretsiz sürümde reklam göstermek için Google AdMob kullanılır. AdMob; reklam kimliği, cihaz bilgisi, yaklaşık konum (IP adresinden) ve reklam etkileşimleri gibi verileri Google'ın politikalarına göre işleyebilir.

Google Kullanıcı Mesajlaşma Platformu: Gerekli bölgelerde reklam rızanı almak ve yönetmek için kullanılır. Tercihini Profil bölümündeki Gizlilik seçeneklerinden değiştirebilirsin.

Google Play Faturalandırma: Reklamları kaldırma ürününü satın alırsan ödeme Google Play tarafından işlenir. Ödeme bilgilerin uygulamaya ulaşmaz. Uygulama yalnızca satın alma durumunu cihazda saklar.

Yedekleme: Yedek dosyası yalnızca senin seçtiğin yere kaydedilir ve şifresizdir. Android bulut yedeklemesi bu uygulama için kapalıdır.

Bildirimler: Hatırlatıcılar cihazda oluşturulur, internet kullanmaz.

Silme: Verilerini Ayarlar bölümünden silebilirsin. Uygulamayı kaldırdığında cihazdaki verileri de silinir.

Çocuklar: Uygulama lise ve mezun öğrencilere yöneliktir, 13 yaş altındaki çocuklar için tasarlanmamıştır.

İletişim: Sorularını Google Play sayfasındaki geliştirici e-posta adresine gönderebilirsin.

Son güncelleme: 5 Kasım 2026"""

const val TERMS_TEXT = """Reklamları Kaldır — Ömür Boyu

Bu ürün tek seferlik ödemedir, abonelik değildir. Satın alma yalnızca reklamları kaldırır; başka özellik açmaz.

Satın alma Google Play hesabına bağlıdır. Aynı hesapla uygulamayı yeniden kurduğunda Satın almaları geri yükle düğmesiyle veya otomatik olarak geri gelir.

Ödeme ve iade işlemleri Google Play kurallarına tabidir."""

@Composable
fun TextScreen(title: String, text: String) {
    val nav = LocalNav.current
    Screen(title, { nav.popBackStack() }) {
        ScrollColumn { YCard { Text(text.trim(), style = MaterialTheme.typography.bodyMedium, color = Yks.c.text) } }
    }
}

@Composable
fun AboutScreen() {
    val nav = LocalNav.current
    val context = LocalContext.current
    Screen("Uygulama hakkında", { nav.popBackStack() }) {
        ScrollColumn {
            Text("YKS Pusula", style = MaterialTheme.typography.headlineMedium, color = Yks.c.text)
            Muted("Sınava hazırlığını tek yerden yönet.")
            YCard {
                KeyValue("Sürüm", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                KeyValue("Çalışma biçimi", "Yerel, hesap gerekmez")
            }
            YCard { Text(PRIVACY_TEXT, style = MaterialTheme.typography.bodyMedium, color = Yks.c.text) }
            SoftButton("Uygulamayı değerlendir", Modifier.fillMaxWidth(), icon = Icons.Rounded.StarBorder) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            Muted("Bu uygulama ÖSYM, MEB veya YÖK ile bağlantılı değildir. Sınav bilgilerini resmî kaynaklardan doğrulamalısın.")
            Muted("Kullanılan açık kaynak bileşenler: AndroidX, Jetpack Compose, Kotlin, kotlinx (Apache 2.0).")
        }
    }
}

/** Ana ekran bileşenleri: destekleyen launcher'da tek dokunuşla eklenir. */
@Composable
fun WidgetsScreen() {
    val nav = LocalNav.current
    val context = LocalContext.current
    val manager = android.appwidget.AppWidgetManager.getInstance(context)
    val canPin = manager.isRequestPinAppWidgetSupported
    Screen("Ana ekran bileşenleri", { nav.popBackStack() }) {
        ScrollColumn {
            Muted("Bileşenler verilerini cihazından alır, internet kullanmaz.")
            GroupCard {
                listOf(
                    Triple("Sınava kalan gün", app.ykspusula.widget.CountdownWidget::class.java, Icons.Rounded.HourglassBottom),
                    Triple("Günlük çalışma hedefi", app.ykspusula.widget.GoalWidget::class.java, Icons.Rounded.DonutLarge),
                    Triple("Bugünkü görevler", app.ykspusula.widget.TasksWidget::class.java, Icons.Rounded.Checklist),
                    Triple("Hızlı odaklanma", app.ykspusula.widget.FocusWidget::class.java, Icons.Rounded.Timer),
                ).forEachIndexed { i, (t, cls, ic) ->
                    if (i > 0) Line()
                    NavRow(t, if (canPin) "Ana ekrana ekle" else "Ana ekrandan ekleyebilirsin", ic, onClick = {
                        if (canPin) manager.requestPinAppWidget(android.content.ComponentName(context, cls), null, null)
                    })
                }
            }
            if (!canPin) Muted("Telefonunda ana ekrana uzun bas, Bileşenler bölümünden YKS Pusula'yı seç.")
        }
    }
}

/** YKS bilgi merkezi: sınav yapısı ve resmî bağlantılar. Çıkmış sorular uygulamaya gömülmez. */
@Composable
fun InfoScreen() {
    val r = repo()
    val nav = LocalNav.current
    val context = LocalContext.current
    val configs by r.user.examConfigs().collectAsStateWithLifecycle(emptyList())
    fun open(url: String) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    Screen("YKS bilgi merkezi", { nav.popBackStack() }) {
        ScrollColumn {
            configs.sortedBy { it.code }.forEach { c ->
                YCard {
                    Text("${c.title} • ${c.examYear}", style = MaterialTheme.typography.titleMedium, color = Yks.c.text)
                    Spacer(Modifier.height(4.dp))
                    KeyValue("Tarih", longDate(LocalDate.parse(c.examDate)))
                    KeyValue("Saat", c.examTime)
                    KeyValue("Süre", "${c.durationMin} dakika")
                    KeyValue("Soru", "${c.questionCount}")
                    KeyValue("Net kuralı", "${c.wrongDivisor.toInt()} yanlış 1 doğruyu götürür")
                    r.sectionsOf(c).forEach { s -> KeyValue(s.name, "${s.parts.sumOf { it.count }} soru") }
                }
            }
            Muted("Bilgiler sınav yılı kılavuzuyla karşılaştırılmalıdır. Resmî olmayan veya açıklanmamış gelecek yıl bilgileri gösterilmez.")
            Section("Resmî bağlantılar")
            GroupCard {
                NavRow("ÖSYM", "Takvim, kılavuz ve çıkmış sorular", Icons.AutoMirrored.Rounded.OpenInNew, onClick = { open("https://www.osym.gov.tr") }); Line()
                NavRow("YÖK Atlas", "Program, kontenjan ve taban verileri", Icons.AutoMirrored.Rounded.OpenInNew, onClick = { open("https://yokatlas.yok.gov.tr") })
            }
            Muted("Bu bağlantılar tarayıcıda açılır ve internet gerektirir. İndirdiğin PDF'leri PDF kaynaklarına ekleyebilirsin.")
        }
    }
}
