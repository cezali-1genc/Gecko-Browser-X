package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.preferences.DnsProvider
import com.example.i18n.AppTranslations
import com.example.ui.state.BrowserViewModel
import com.example.ui.theme.OperaCrimson
import com.example.ui.theme.VerifiedEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsVpnScreen(
    viewModel: BrowserViewModel,
    onRequestVpnStart: () -> Unit,
    onStopVpn: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isVpnActive by viewModel.isVpnActive.collectAsStateWithLifecycle()
    val strings = remember(settings.appLanguage) { AppTranslations.forLang(settings.appLanguage) }

    var selectedProvider by remember(settings.dnsProvider) { mutableStateOf(settings.dnsProvider) }
    var customPrimary by remember(settings.customDnsPrimary) { mutableStateOf(settings.customDnsPrimary) }
    var customSecondary by remember(settings.customDnsSecondary) { mutableStateOf(settings.customDnsSecondary) }
    var customDoh by remember(settings.customDohUri) { mutableStateOf(settings.customDohUri) }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dns_vpn_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isVpnActive) VerifiedEmerald.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.2.dp,
                        if (isVpnActive) VerifiedEmerald else OperaCrimson.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isVpnActive) Icons.Filled.Shield else Icons.Filled.VpnLock,
                                contentDescription = null,
                                tint = if (isVpnActive) VerifiedEmerald else OperaCrimson,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isVpnActive) stringResource(R.string.dns_vpn_active)
                                    else stringResource(R.string.dns_vpn_inactive),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = settings.dnsProvider.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = strings.vpnDisclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                if (isVpnActive) onStopVpn() else onRequestVpnStart()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVpnActive) MaterialTheme.colorScheme.error else OperaCrimson
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vpn_toggle_button")
                        ) {
                            Icon(Icons.Filled.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isVpnActive) stringResource(R.string.dns_vpn_disconnect)
                                else stringResource(R.string.dns_vpn_connect)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = strings.dnsProfile,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(DnsProvider.entries) { provider ->
                val isSelected = selectedProvider == provider
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedProvider = provider
                            if (provider != DnsProvider.CUSTOM) {
                                viewModel.setDnsConfiguration(
                                    provider = provider,
                                    customPrimary = customPrimary,
                                    customSecondary = customSecondary,
                                    customDohUri = customDoh
                                )
                            }
                        }
                        .testTag("dns_provider_${provider.name}"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                selectedProvider = provider
                                if (provider != DnsProvider.CUSTOM) {
                                    viewModel.setDnsConfiguration(
                                        provider = provider,
                                        customPrimary = customPrimary,
                                        customSecondary = customSecondary,
                                        customDohUri = customDoh
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(provider.displayName, style = MaterialTheme.typography.titleSmall)
                            if (provider.primaryIp.isNotBlank()) {
                                Text(
                                    text = "${provider.primaryIp} • ${provider.dohUrl}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (selectedProvider == DnsProvider.CUSTOM) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customPrimary,
                                onValueChange = { customPrimary = it },
                                label = { Text(strings.primaryDns) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = customSecondary,
                                onValueChange = { customSecondary = it },
                                label = { Text(strings.secondaryDns) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = customDoh,
                                onValueChange = { customDoh = it },
                                label = { Text(strings.dohEndpoint) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    viewModel.setDnsConfiguration(
                                        provider = DnsProvider.CUSTOM,
                                        customPrimary = customPrimary,
                                        customSecondary = customSecondary,
                                        customDohUri = customDoh
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(strings.applyDns)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
