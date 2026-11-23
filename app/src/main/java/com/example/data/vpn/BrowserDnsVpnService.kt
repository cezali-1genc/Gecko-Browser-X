package com.example.data.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local per-app Android VpnService that binds Browser X traffic through a configured
 * DNS shield profile (Cloudflare, Quad9, AdGuard, Google, or Custom DNS) with transparent
 * foreground status notification.
 */
class BrowserDnsVpnService : VpnService() {

    companion object {
        const val CHANNEL_ID = "browserx_dns_vpn_channel"
        const val NOTIF_ID = 7201
        const val ACTION_START = "com.example.vpn.START"
        const val ACTION_STOP = "com.example.vpn.STOP"
        const val EXTRA_PRIMARY_DNS = "primary_dns"
        const val EXTRA_SECONDARY_DNS = "secondary_dns"
        const val EXTRA_PROVIDER_NAME = "provider_name"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context, primaryDns: String, secondaryDns: String, providerName: String) {
            val intent = Intent(context, BrowserDnsVpnService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PRIMARY_DNS, primaryDns)
                putExtra(EXTRA_SECONDARY_DNS, secondaryDns)
                putExtra(EXTRA_PROVIDER_NAME, providerName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BrowserDnsVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            tearDown()
            return START_NOT_STICKY
        }

        val primary = intent?.getStringExtra(EXTRA_PRIMARY_DNS).orEmpty().ifBlank { "1.1.1.1" }
        val secondary = intent?.getStringExtra(EXTRA_SECONDARY_DNS).orEmpty().ifBlank { "1.0.0.1" }
        val provider = intent?.getStringExtra(EXTRA_PROVIDER_NAME).orEmpty().ifBlank { "Cloudflare" }

        ensureChannel()
        startForeground(NOTIF_ID, buildNotification(provider, primary))

        runCatching {
            vpnInterface?.close()
            val builder = Builder()
                .setSession("ModLU DNS Shield ($provider)")
                .setMtu(1500)
                .addAddress("10.10.0.2", 32)
                .addDnsServer(primary)
            if (secondary.isNotBlank() && secondary != primary) {
                builder.addDnsServer(secondary)
            }
            runCatching { builder.addAllowedApplication(packageName) }
            builder.allowBypass()
            vpnInterface = builder.establish()
            _isRunning.value = vpnInterface != null
        }.onFailure {
            _isRunning.value = false
            tearDown()
        }

        return START_STICKY
    }

    private fun tearDown() {
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        tearDown()
        super.onRevoke()
    }

    override fun onDestroy() {
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        _isRunning.value = false
        super.onDestroy()
    }

    private fun buildNotification(provider: String, primaryDns: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.dns_vpn_active))
            .setContentText("$provider • DNS: $primaryDns")
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    7202,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .addAction(
                0,
                getString(R.string.dns_vpn_disconnect),
                PendingIntent.getService(
                    this,
                    7203,
                    Intent(this, BrowserDnsVpnService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.dns_vpn_title),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }
}
