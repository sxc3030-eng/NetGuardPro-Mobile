package com.netguardpro.mobile.vpn

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netguardpro.mobile.ui.theme.BrandCyan
import com.netguardpro.mobile.ui.theme.BrandError
import com.netguardpro.mobile.ui.theme.BrandOnSurface
import com.netguardpro.mobile.ui.theme.BrandOnSurfaceVariant
import com.netguardpro.mobile.ui.theme.BrandSuccess
import com.netguardpro.mobile.ui.theme.BrandSurface
import com.netguardpro.mobile.ui.theme.BrandSurfaceVariant
import com.netguardpro.mobile.ui.theme.BrandWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VpnScreen(viewModel: VpnViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "VPN Protection",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
            )
        }

        item {
            ConnectButton(
                connectionState = state.connectionState,
                onToggle = viewModel::toggleConnection,
            )
        }

        item {
            ServerSelector(
                selectedServer = state.selectedServer,
                servers = state.servers,
                enabled = state.connectionState == VpnConnectionState.DISCONNECTED,
                onSelect = viewModel::selectServer,
            )
        }

        if (state.connectionState == VpnConnectionState.CONNECTED) {
            item {
                ConnectionInfo(
                    assignedIp = state.assignedIp,
                    serverName = state.selectedServer.name,
                )
            }
            item {
                StatsCard(stats = state.stats)
            }
        }

        if (state.connectionHistory.isNotEmpty()) {
            item {
                Text(
                    text = "Connection History",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(state.connectionHistory) { entry ->
                HistoryItem(entry)
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ConnectButton(
    connectionState: VpnConnectionState,
    onToggle: () -> Unit,
) {
    val isConnecting = connectionState == VpnConnectionState.CONNECTING
    val isConnected = connectionState == VpnConnectionState.CONNECTED

    val buttonColor by animateColorAsState(
        targetValue = when (connectionState) {
            VpnConnectionState.DISCONNECTED -> BrandSurfaceVariant
            VpnConnectionState.CONNECTING -> BrandWarning
            VpnConnectionState.CONNECTED -> BrandSuccess
            VpnConnectionState.DISCONNECTING -> BrandWarning
        },
        label = "buttonColor",
    )

    val borderColor by animateColorAsState(
        targetValue = when (connectionState) {
            VpnConnectionState.DISCONNECTED -> BrandCyan
            VpnConnectionState.CONNECTING -> BrandWarning
            VpnConnectionState.CONNECTED -> BrandSuccess
            VpnConnectionState.DISCONNECTING -> BrandWarning
        },
        label = "borderColor",
    )

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnecting) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(buttonColor.copy(alpha = 0.15f))
                .border(3.dp, borderColor, CircleShape)
                .clickable(enabled = connectionState != VpnConnectionState.CONNECTING && connectionState != VpnConnectionState.DISCONNECTING) { onToggle() },
        ) {
            Icon(
                imageVector = Icons.Filled.Power,
                contentDescription = "Connect",
                tint = borderColor,
                modifier = Modifier.size(64.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (connectionState) {
                VpnConnectionState.DISCONNECTED -> "Tap to Connect"
                VpnConnectionState.CONNECTING -> "Connecting..."
                VpnConnectionState.CONNECTED -> "Connected"
                VpnConnectionState.DISCONNECTING -> "Disconnecting..."
            },
            style = MaterialTheme.typography.titleMedium,
            color = borderColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ServerSelector(
    selectedServer: VpnServer,
    servers: List<VpnServer>,
    enabled: Boolean,
    onSelect: (VpnServer) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { expanded = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedServer.flagEmoji,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedServer.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Text(
                        text = selectedServer.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandOnSurfaceVariant,
                    )
                }
                Text(
                    text = if (enabled) "Change" else "Locked",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) BrandCyan else BrandOnSurfaceVariant,
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                servers.forEach { server ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(server.flagEmoji)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(server.name, color = Color.White)
                                    Text(server.location, style = MaterialTheme.typography.bodySmall, color = BrandOnSurfaceVariant)
                                }
                            }
                        },
                        onClick = {
                            onSelect(server)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionInfo(assignedIp: String, serverName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Assigned IP", style = MaterialTheme.typography.labelMedium, color = BrandOnSurfaceVariant)
                Text(assignedIp, style = MaterialTheme.typography.bodyLarge, color = BrandCyan, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Server", style = MaterialTheme.typography.labelMedium, color = BrandOnSurfaceVariant)
                Text(serverName, style = MaterialTheme.typography.bodyLarge, color = Color.White)
            }
        }
    }
}

@Composable
private fun StatsCard(stats: VpnStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(
                icon = Icons.Filled.ArrowUpward,
                label = "Upload",
                value = formatBytes(stats.uploadSpeed) + "/s",
                total = formatBytes(stats.uploadBytes),
                color = BrandCyan,
            )
            StatItem(
                icon = Icons.Filled.ArrowDownward,
                label = "Download",
                value = formatBytes(stats.downloadSpeed) + "/s",
                total = formatBytes(stats.downloadBytes),
                color = BrandSuccess,
            )
            StatItem(
                icon = Icons.Filled.Timer,
                label = "Duration",
                value = formatDuration(stats.connectedDurationSeconds),
                total = "",
                color = BrandWarning,
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    total: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = BrandOnSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        if (total.isNotEmpty()) {
            Text(text = total, style = MaterialTheme.typography.labelSmall, color = BrandOnSurfaceVariant)
        }
    }
}

@Composable
private fun HistoryItem(entry: ConnectionHistoryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.serverName, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandOnSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatDuration(entry.durationSeconds), style = MaterialTheme.typography.bodyMedium, color = BrandOnSurface)
                Text(
                    text = "${formatBytes(entry.uploadBytes)} / ${formatBytes(entry.downloadBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandOnSurfaceVariant,
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1048576 -> "${bytes / 1024} KB"
    bytes < 1073741824 -> String.format("%.1f MB", bytes / 1048576.0)
    else -> String.format("%.2f GB", bytes / 1073741824.0)
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}
