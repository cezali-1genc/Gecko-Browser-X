package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.DecryptedCredential
import com.example.data.repository.ReaderArticle
import com.example.i18n.AppTranslations
import com.example.i18n.LocaleHelper
import com.example.ui.state.PendingSaveCredential
import kotlin.math.roundToInt

@Composable
fun SaveCredentialDialog(
    pending: PendingSaveCredential,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.pw_save_prompt)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(pending.origin, style = MaterialTheme.typography.titleSmall)
                Text(pending.username, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.pw_save_btn)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun AutofillBanner(
    credentials: List<DecryptedCredential>,
    onFillCredential: (DecryptedCredential) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val first = credentials.firstOrNull() ?: return
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pw_autofill_prompt, first.host),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = first.username,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(onClick = { onFillCredential(first) }) {
                    Text(stringResource(R.string.pw_autofill_btn))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }
            if (credentials.size > 1) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    credentials.drop(1).take(3).forEach { alt ->
                        OutlinedButton(onClick = { onFillCredential(alt) }) {
                            Text(alt.username, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderModeSheet(
    article: ReaderArticle?,
    isLoading: Boolean,
    appLanguage: String,
    onDismiss: () -> Unit
) {
    val strings = remember(appLanguage) { AppTranslations.forLang(appLanguage) }
    var textSizeSp by remember { mutableIntStateOf(17) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp, max = 560.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ChromeReaderMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.menu_reader_mode), style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { if (textSizeSp > 13) textSizeSp -= 2 }) { Text("A-") }
                    TextButton(onClick = { if (textSizeSp < 26) textSizeSp += 2 }) { Text("A+") }
                    TextButton(onClick = onDismiss) { Text(strings.readerClose) }
                }
            }

            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text(strings.readerLoading, style = MaterialTheme.typography.bodyMedium)
            } else if (article != null) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                    items(article.paragraphs) { para ->
                        Text(
                            text = para,
                            fontSize = textSizeSp.sp,
                            lineHeight = (textSizeSp * 1.48f).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomControlDialog(
    currentZoom: Int,
    appLanguage: String,
    onApplyZoom: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = remember(appLanguage) { AppTranslations.forLang(appLanguage) }
    var sliderVal by remember(currentZoom) { mutableFloatStateOf(currentZoom.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.ZoomIn, contentDescription = null) },
        title = { Text(String.format(strings.zoomTitle, sliderVal.roundToInt())) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Slider(
                    value = sliderVal,
                    onValueChange = { sliderVal = it },
                    valueRange = 50f..200f,
                    steps = 5
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = { sliderVal = 100f }) { Text("100%") }
                    OutlinedButton(onClick = { sliderVal = 125f }) { Text("125%") }
                    OutlinedButton(onClick = { sliderVal = 150f }) { Text("150%") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyZoom(sliderVal.roundToInt())
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateSheet(
    isOnWebPage: Boolean,
    appLanguage: String,
    onTranslatePage: (targetLang: String) -> Unit,
    onTranslateText: (text: String, targetLang: String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = remember(appLanguage) { AppTranslations.forLang(appLanguage) }
    var selectedTarget by remember(appLanguage) { mutableStateOf(LocaleHelper.resolveLocale(appLanguage).language) }
    var textToTranslate by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.menu_translate_page), style = MaterialTheme.typography.titleMedium)
            }

            Text(strings.translateInto, style = MaterialTheme.typography.labelLarge)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LocaleHelper.supportedLanguages.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { lang ->
                            val selected = selectedTarget == lang.tag
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTarget = lang.tag },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            if (isOnWebPage) {
                Button(
                    onClick = {
                        onTranslatePage(selectedTarget)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${stringResource(R.string.menu_translate_page)} (${selectedTarget.uppercase()})")
                }
            }

            HorizontalDivider()

            OutlinedTextField(
                value = textToTranslate,
                onValueChange = { textToTranslate = it },
                label = { Text(strings.translateSelectedHint) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = {
                    if (textToTranslate.isNotBlank()) {
                        onTranslateText(textToTranslate, selectedTarget)
                        onDismiss()
                    }
                },
                enabled = textToTranslate.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.translateAction)
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}
