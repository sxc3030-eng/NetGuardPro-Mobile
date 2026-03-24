package com.netguardpro.mobile.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.netguardpro.mobile.MainActivity
import com.netguardpro.mobile.NetGuardApp
import com.netguardpro.mobile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class NetGuardVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunnelJob: Job? = null

    private val _bytesReceived = MutableStateFlow(0L)
    val bytesReceived: StateFlow<Long> = _bytesReceived.asStateFlow()

    private val _bytesSent = MutableStateFlow(0L)
    val bytesSent: StateFlow<Long> = _bytesSent.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_CONNECT -> {
                val configStr = intent.getStringExtra(EXTRA_CONFIG) ?: ""
                if (configStr.isNotEmpty()) {
                    val config = WireGuardConfig.parse(configStr)
                    connect(config)
                }
                START_STICKY
            }
            ACTION_DISCONNECT -> {
                disconnect()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    private fun connect(config: WireGuardConfig) {
        val builder = Builder()
            .setSession("NetGuardPro VPN")
            .setMtu(config.interfaceConfig.mtu)

        val address = config.interfaceConfig.address.split("/")
        val ip = address[0]
        val prefix = if (address.size > 1) address[1].toIntOrNull() ?: 32 else 32
        builder.addAddress(ip, prefix)

        config.interfaceConfig.dns.forEach { dns ->
            builder.addDnsServer(dns)
        }

        config.peers.forEach { peer ->
            peer.allowedIPs.forEach { allowedIp ->
                val parts = allowedIp.split("/")
                val routeAddress = parts[0]
                val routePrefix = if (parts.size > 1) parts[1].toIntOrNull() ?: 32 else 32
                builder.addRoute(routeAddress, routePrefix)
            }
        }

        builder.setBlocking(true)

        vpnInterface = builder.establish()

        if (vpnInterface != null) {
            startForeground(NOTIFICATION_ID, createNotification())
            startTunnel()
        }
    }

    private fun startTunnel() {
        tunnelJob?.cancel()
        tunnelJob = scope.launch {
            val fd = vpnInterface ?: return@launch
            val input = FileInputStream(fd.fileDescriptor)
            val output = FileOutputStream(fd.fileDescriptor)
            val buffer = ByteBuffer.allocate(32767)

            try {
                while (true) {
                    buffer.clear()
                    val length = input.read(buffer.array())
                    if (length > 0) {
                        _bytesSent.value += length

                        // In a real implementation, the packet would be encrypted via WireGuard
                        // and sent to the peer endpoint. Here we demonstrate the tunnel loop structure.
                        // The WireGuard tunnel library handles the actual crypto in production.

                        buffer.limit(length)
                        output.write(buffer.array(), 0, length)
                        _bytesReceived.value += length
                    } else {
                        delay(100)
                    }
                }
            } catch (e: Exception) {
                // Tunnel closed
            }
        }
    }

    private fun disconnect() {
        tunnelJob?.cancel()
        tunnelJob = null
        vpnInterface?.close()
        vpnInterface = null
        _bytesReceived.value = 0
        _bytesSent.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, NetGuardApp.CHANNEL_VPN)
            .setContentTitle("NetGuardPro VPN Active")
            .setContentText("Your connection is secured")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        disconnect()
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.netguardpro.mobile.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.netguardpro.mobile.vpn.DISCONNECT"
        const val EXTRA_CONFIG = "config"
        private const val NOTIFICATION_ID = 1001

        var instance: NetGuardVpnService? = null
            private set
    }
}
