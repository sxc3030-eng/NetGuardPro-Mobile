package com.netguardpro.mobile.cleaner

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netguardpro.mobile.ui.theme.BrandCyan
import com.netguardpro.mobile.ui.theme.BrandError
import com.netguardpro.mobile.ui.theme.BrandOnSurfaceVariant
import com.netguardpro.mobile.ui.theme.BrandPurple
import com.netguardpro.mobile.ui.theme.BrandSuccess
import com.netguardpro.mobile.ui.theme.BrandSurface
import com.netguardpro.mobile.ui.theme.BrandSurfaceVariant
import com.netguardpro.mobile.ui.theme.BrandWarning

@Composable
fun CleanerScreen(viewModel: CleanerViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Device Cleaner",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
        )

        StorageCard(storageInfo = state.storageInfo)

        ScanButton(
            scanState = state.scanState,
            onScan = viewModel::scan,
        )

        if (state.scanState == ScanState.COMPLETED || state.scanState == ScanState.CLEANING) {
            if (state.scanResult.totalCount > 0) {
                Text(
                    text = "Found ${state.scanResult.totalCount} items (${formatSize(state.scanResult.totalSize)})",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandWarning,
                    fontWeight = FontWeight.SemiBold,
                )

                CategoryCard(
                    icon = Icons.Filled.Cached,
                    title = "App Cache",
                    items = state.scanResult.cacheFiles,
                    color = BrandCyan,
                    cleaned = JunkCategory.CACHE in state.cleanedCategories,
                    onClean = { viewModel.cleanCategory(JunkCategory.CACHE) },
                )
                CategoryCard(
                    icon = Icons.Filled.Android,
                    title = "APK Files",
                    items = state.scanResult.apkFiles,
                    color = BrandPurple,
                    cleaned = JunkCategory.APKS in state.cleanedCategories,
                    onClean = { viewModel.cleanCategory(JunkCategory.APKS) },
                )
                CategoryCard(
                    icon = Icons.Filled.FolderOpen,
                    title = "Large Files (>50MB)",
                    items = state.scanResult.largeFiles,
                    color = BrandError,
                    cleaned = JunkCategory.LARGE_FILES in state.cleanedCategories,
                    onClean = { viewModel.cleanCategory(JunkCategory.LARGE_FILES) },
                )
                CategoryCard(
                    icon = Icons.Filled.Description,
                    title = "Temp Files",
                    items = state.scanResult.tempFiles,
                    color = BrandWarning,
                    cleaned = JunkCategory.TEMP_FILES in state.cleanedCategories,
                    onClean = { viewModel.cleanCategory(JunkCategory.TEMP_FILES) },
                )

                if (state.scanResult.totalCount > 0) {
                    Button(
                        onClick = viewModel::cleanAll,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.scanState != ScanState.CLEANING,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandError),
                    ) {
                        Icon(Icons.Filled.AutoDelete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clean All (${formatSize(state.scanResult.totalSize)})",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Clean",
                            tint = BrandSuccess,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your device is clean!",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandSuccess,
                        )
                        if (state.lastCleanedBytes > 0) {
                            Text(
                                text = "Cleaned ${formatSize(state.lastCleanedBytes)} total",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandOnSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StorageCard(storageInfo: StorageInfo) {
    val animatedUsage by animateFloatAsState(
        targetValue = storageInfo.usagePercent,
        animationSpec = tween(1000),
        label = "storage",
    )

    val usageColor = when {
        storageInfo.usagePercent > 0.9f -> BrandError
        storageInfo.usagePercent > 0.7f -> BrandWarning
        else -> BrandCyan
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
                modifier = Modifier.size(100.dp),
            ) {
                Canvas(modifier = Modifier.size(100.dp)) {
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
                        color = usageColor,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedUsage,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
                Text(
                    text = "${(animatedUsage * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = usageColor,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column {
                Text("Storage", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                StorageRow("Used", formatSize(storageInfo.usedBytes), usageColor)
                StorageRow("Free", formatSize(storageInfo.freeBytes), BrandSuccess)
                StorageRow("Total", formatSize(storageInfo.totalBytes), BrandOnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StorageRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandOnSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ScanButton(
    scanState: ScanState,
    onScan: () -> Unit,
) {
    val isScanning = scanState == ScanState.SCANNING

    val pulseTransition = rememberInfiniteTransition(label = "scanPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isScanning) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Button(
        onClick = onScan,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isScanning && scanState != ScanState.CLEANING,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandCyan.copy(alpha = pulseAlpha)),
    ) {
        if (isScanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scanning...", fontWeight = FontWeight.Bold, color = Color.White)
        } else {
            Icon(Icons.Filled.CleaningServices, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (scanState == ScanState.IDLE) "Scan Device" else "Scan Again",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
        }
    }
}

@Composable
private fun CategoryCard(
    icon: ImageVector,
    title: String,
    items: List<JunkFile>,
    color: Color,
    cleaned: Boolean,
    onClean: () -> Unit,
) {
    val totalSize = items.sumOf { it.size }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (cleaned) BrandSuccess else color,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    text = if (cleaned) "Cleaned" else "${items.size} items - ${formatSize(totalSize)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (cleaned) BrandSuccess else BrandOnSurfaceVariant,
                )
            }
            if (!cleaned && items.isNotEmpty()) {
                FilledTonalButton(
                    onClick = onClean,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = color.copy(alpha = 0.2f),
                        contentColor = color,
                    ),
                ) {
                    Text("Clean", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            } else if (cleaned) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Cleaned",
                    tint = BrandSuccess,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1048576 -> "${bytes / 1024} KB"
    bytes < 1073741824 -> String.format("%.1f MB", bytes / 1048576.0)
    else -> String.format("%.2f GB", bytes / 1073741824.0)
}
