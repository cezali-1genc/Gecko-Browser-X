package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.OperaCrimson
import com.example.ui.theme.OperaCrimsonDark
import com.example.ui.theme.VerifiedEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onOpenOfficialWebsite: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val noticesText = remember {
        runCatching {
            context.assets.open("notices.txt").bufferedReader().use { it.readText() }
        }.getOrElse { context.getString(R.string.license_notice_body) }
    }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("about_screen_list")
        ) {
            // Formal Opera-inspired App Hero Banner
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.2.dp, OperaCrimson.copy(alpha = 0.55f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        OperaCrimsonDark.copy(alpha = 0.34f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(56.dp),
                                    shape = CircleShape,
                                    color = OperaCrimson.copy(alpha = 0.16f),
                                    border = BorderStroke(1.5.dp, OperaCrimson)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_launcher_foreground),
                                            contentDescription = stringResource(R.string.brand_full_title),
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.brand_full_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "v${BuildConfig.VERSION_NAME} • ${stringResource(R.string.engine_badge)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = OperaCrimson
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AboutPillChip(text = stringResource(R.string.brand_short))
                                AboutPillChip(text = stringResource(R.string.brand_team))
                            }

                            Text(
                                text = stringResource(R.string.start_tagline),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Formal Developer Information Card
            item {
                AboutSectionCard(
                    icon = Icons.Filled.Person,
                    title = stringResource(R.string.about_developer_section),
                    accent = OperaCrimson
                ) {
                    InfoRow(
                        icon = Icons.Filled.VerifiedUser,
                        label = stringResource(R.string.about_developer_name_label),
                        value = "SeRhAt GüNeR"
                    )
                    InfoRow(
                        icon = Icons.Filled.Shield,
                        label = stringResource(R.string.about_brand_label),
                        value = "MσdLU 1αPK • CEZALI 1GENÇ [S-G] ꪜ"
                    )
                    InfoRow(
                        icon = Icons.Filled.Email,
                        label = stringResource(R.string.about_email_label),
                        value = "cezali.1genc@gmail.com"
                    )
                    InfoRow(
                        icon = Icons.Filled.Phone,
                        label = stringResource(R.string.about_phone_label),
                        value = "+90 546 856 55 38"
                    )

                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onOpenOfficialWebsite("https://website-with-animated-background-rzvm.b12sites.com/")
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("about_official_portal_btn")
                        ) {
                            Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.about_open_portal))
                        }
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:cezali.1genc@gmail.com"))
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = stringResource(R.string.about_email_label), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Copyright Notices
            item {
                AboutSectionCard(
                    icon = Icons.Filled.Gavel,
                    title = stringResource(R.string.about_copyright_section),
                    accent = VerifiedEmerald
                ) {
                    Text(
                        text = """
                            © 2026 CEZALI 1GENÇ [S-G] ꪜ • MσdLU 1αPK • SeRhAt GüNeR. Tüm hakları saklıdır / All rights reserved.

                            MADDE 1 — ESER VE HAK SAHİPLİĞİ: ModLU 1APK Browser X yazılımı, arayüz bileşenleri, simge tasarımları ve marka unsurları 5846 sayılı Fikir ve Sanat Eserleri Kanunu (FSEK) ve uluslararası fikri mülkiyet sözleşmeleri kapsamında korunmaktadır.
                            MADDE 2 — İZİNSİZ KULLANIM YASAĞI: Hak sahibinin yazılı izni olmadan uygulamanın ticari kopyalanması, marka kimliğinin taklit edilmesi veya yanıltıcı şekilde yeniden dağıtılması yasaktır.
                            MADDE 3 — KULLANIM LİSANSI: Kullanıcıya yalnızca kişisel ve yasal tarama amacıyla devredilemez kullanım hakkı tanınır.
                            MADDE 4–6 — MARKA KORUMASI: "MσdLU 1αPK", "CEZALI 1GENÇ [S-G] ꪜ" ve "SeRhAt GüNeR" adları yalnızca resmi sürüm için geçerlidir.
                            MADDE 7–9 — HUKUKİ BAŞVURU: İhlal ve telif talepleri için cezali.1genc@gmail.com üzerinden bildirim yapılabilir.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Privacy Policy (KVKK)
            item {
                AboutSectionCard(
                    icon = Icons.Filled.Policy,
                    title = stringResource(R.string.about_privacy_section),
                    accent = VerifiedEmerald
                ) {
                    Text(
                        text = """
                            Yürürlük Tarihi / Effective Date: 19.11.2026
                            Veri Sorumlusu / Data Controller: SeRhAt GüNeR • CEZALI 1GENÇ [S-G] ꪜ (cezali.1genc@gmail.com)

                            MADDE 1–3 — VERİ İŞLEME İLKELERİ: Tarayıcı verileriniz (yer imleri, geçmiş, indirmeler, şifreler) yalnızca cihazınızda yerel Room veritabanında ve Android Keystore AES-256-GCM kasasında saklanır; üçüncü taraf sunuculara aktarılmaz.
                            MADDE 4–6 — GİZLİ OTURUMLAR VE İZLEYİCİ ENGELLEME: Gizli sekmelerde tarama geçmişi, çerezler, form verileri ve şifreler diske yazılmaz; otomatik doldurma ve şifre kaydetme gizli oturumlarda tamamen devre dışıdır.
                            MADDE 7–9 — AĞ VE İZİN KULLANIMI: Kamera (QR tarama), Mikrofon (sesli arama / site izinleri), Konum (site talebiyle) ve yerel DNS VPN (kullanıcı başlattığında) izinleri yalnızca açık kullanıcı onayıyla çalışır.
                            MADDE 10–12 — KVKK HAKLARINIZ: Gizlilik Merkezi üzerinden tüm geçmiş, çerez, site izni ve kayıtlı şifrelerinizi dilediğiniz an tek dokunuşla kalıcı olarak silebilirsiniz.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Open Source & Engine Licenses
            item {
                AboutSectionCard(
                    icon = Icons.Filled.Code,
                    title = stringResource(R.string.license_notice_title),
                    accent = OperaCrimson
                ) {
                    Text(
                        text = noticesText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutPillChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AboutSectionCard(
    icon: ImageVector,
    title: String,
    accent: Color,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            content()
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
