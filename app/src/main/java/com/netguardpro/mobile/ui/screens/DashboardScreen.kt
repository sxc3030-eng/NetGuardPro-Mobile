package com.netguardpro.mobile.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netguardpro.mobile.ui.theme.BrandCyan
import com.netguardpro.mobile.ui.theme.BrandError
import com.netguardpro.mobile.ui.theme.BrandOnSurface
import com.netguardpro.mobile.ui.theme.BrandOnSurfaceVariant
import com.netguardpro.mobile.ui.theme.BrandSuccess
import com.netguardpro.mobile.ui.theme.BrandSurface
import com.netguardpro.mobile.ui.theme.BrandSurfaceVariant
import com.netguardpro.mobile.ui.theme.BrandWarning

@Composable
fun DashboardScreen() {
    var vpnEnabled by remember { mutableStateOf(false) }
    var firewallEnabled by remember { mutableStateOf(false) }
    var dnsEnabled by remember { mutableStateOf(false) }

    val securityScore = remember(vpnEnabled, firewallEnabled, dnsEnabled) {
        var score = 25
        if (vpnEnabled) score += 25
        if (firewallEnabled) score += 25
        if (dnsEnabled) score += 25
        score
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
        )

        SecurityScoreCard(score = securityScore)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "VPN",
                icon = Icons.Filled.VpnKey,
                status = if (vpnEnabled) "Connected" else "Disconnected",
                statusColor = if (vpnEnabled) BrandSuccess else BrandOnSurfaceVariant,
                detail = if (vpnEnabled) "US-East-1 | 10.0.0.2" else "Not connected",
            )
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "Firewall",
                icon = Icons.Filled.Security,
                status = if (firewallEnabled) "Active" else "Inactive",
                statusColor = if (firewallEnabled) BrandSuccess else BrandOnSurfaceVariant,
                detail = if (firewallEnabled) "24 rules | 142 blocked" else "No active rules",
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "DNS Filter",
                icon = Icons.Filled.Dns,
                status = if (dnsEnabled) "Filtering" else "Inactive",
                statusColor = if (dnsEnabled) BrandCyan else BrandOnSurfaceVariant,
                detail = if (dnsEnabled) "1,247 blocked today" else "No filtering active",
            )
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "Cleaner",
                icon = Icons.Filled.CleaningServices,
                status = "487 MB junk",
                statusColor = BrandWarning,
                detail = "12.4 GB / 64 GB used",
            )
        }

        QuickActionsCard(
            vpnEnabled = vpnEnabled,
            firewallEnabled = firewallEnabled,
            dnsEnabled = dnsEnabled,
            onVpnToggle = { vpnEnabled = it },
            onFirewallToggle = { firewallEnabled = it },
            onDnsToggle = { dnsEnabled = it },
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SecurityScoreCard(score: Int) {
    var animatedTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(score) { animatedTarget = score / 100f }
    val animatedProgress by animateFloatAsState(
        targetValue = animatedTarget,
        animationSpec = tween(durationMillis = 1000),
        label = "score",
    )

    val scoreColor = when {
        score >= 75 -> BrandSuccess
        score >= 50 -> BrandCyan
        score >= 25 -> BrandWarning
        else -> BrandError
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp),
            ) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    drawArc(
                        color = BrandSurfaceVariant,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = scoreColor,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}",
                        style = MaterialTheme.typography.displayLarge,
                        color = scoreColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                    )
                    Text(
                        text = "/ 100",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandOnSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column {
                Text(
                    text = "Security Score",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        score >= 75 -> "Your device is well protected. All major security features are active."
                        score >= 50 -> "Good protection level. Enable more features for maximum security."
                        score >= 25 -> "Basic protection only. Consider enabling VPN and Firewall."
                        else -> "Your device is vulnerable. Enable security features now."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandOnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    status: String,
    statusColor: Color,
    detail: String,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = BrandCyan,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelLarge,
                color = statusColor,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = BrandOnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActionsCard(
    vpnEnabled: Boolean,
    firewallEnabled: Boolean,
    dnsEnabled: Boolean,
    onVpnToggle: (Boolean) -> Unit,
    onFirewallToggle: (Boolean) -> Unit,
    onDnsToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(16.dp))
            ToggleRow(
                icon = Icons.Filled.VpnKey,
                label = "VPN Protection",
                checked = vpnEnabled,
                onToggle = onVpnToggle,
            )
            ToggleRow(
                icon = Icons.Filled.Shield,
                label = "Firewall",
                checked = firewallEnabled,
                onToggle = onFirewallToggle,
            )
            ToggleRow(
                icon = Icons.Filled.Dns,
                label = "DNS Filtering",
                checked = dnsEnabled,
                onToggle = onDnsToggle,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (checked) BrandCyan else BrandOnSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = BrandOnSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandCyan,
                uncheckedThumbColor = BrandOnSurfaceVariant,
                uncheckedTrackColor = BrandSurfaceVariant,
                uncheckedBorderColor = BrandOnSurfaceVariant,
            ),
        )
    }
}
