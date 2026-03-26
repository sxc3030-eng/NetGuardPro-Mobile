package com.netguardpro.mobile.firewall

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun FirewallScreen(viewModel: FirewallViewModel = viewModel(factory = FirewallViewModel.Factory)) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Firewall",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(16.dp))

        FirewallHeaderCard(
            isEnabled = state.isEnabled,
            blockedCount = state.blockedTodayCount,
            onToggle = viewModel::toggleFirewall,
        )

        Spacer(modifier = Modifier.height(12.dp))

        SearchAndFilterBar(
            query = state.searchQuery,
            showSystemApps = state.showSystemApps,
            onQueryChange = viewModel::updateSearch,
            onToggleSystem = viewModel::toggleShowSystemApps,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = "Application",
                style = MaterialTheme.typography.labelMedium,
                color = BrandOnSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "WiFi",
                style = MaterialTheme.typography.labelMedium,
                color = BrandOnSurfaceVariant,
                modifier = Modifier.width(48.dp),
            )
            Text(
                text = "Mobile",
                style = MaterialTheme.typography.labelMedium,
                color = BrandOnSurfaceVariant,
                modifier = Modifier.width(48.dp),
            )
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BrandCyan)
            }
        } else if (state.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Error",
                        tint = BrandError,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.errorMessage ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandOnSurfaceVariant,
                    )
                }
            }
        } else if (state.rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No apps found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrandOnSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.rules, key = { it.packageName }) { rule ->
                    AppRuleItem(
                        rule = rule,
                        onWifiToggle = { viewModel.toggleWifi(rule.packageName, it) },
                        onMobileToggle = { viewModel.toggleMobile(rule.packageName, it) },
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun FirewallHeaderCard(
    isEnabled: Boolean,
    blockedCount: Long,
    onToggle: (Boolean) -> Unit,
) {
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
                imageVector = Icons.Filled.Security,
                contentDescription = "Firewall",
                tint = if (isEnabled) BrandSuccess else BrandOnSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnabled) "Firewall Active" else "Firewall Disabled",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Block,
                        contentDescription = "Blocked",
                        tint = BrandError,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$blockedCount connections blocked today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandOnSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = isEnabled,
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
}

@Composable
private fun SearchAndFilterBar(
    query: String,
    showSystemApps: Boolean,
    onQueryChange: (String) -> Unit,
    onToggleSystem: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search apps...", color = BrandOnSurfaceVariant) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = BrandOnSurfaceVariant)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandCyan,
                unfocusedBorderColor = BrandSurfaceVariant,
                cursorColor = BrandCyan,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = BrandSurface,
                unfocusedContainerColor = BrandSurface,
            ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = { onToggleSystem(!showSystemApps) }) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = "Show system apps",
                tint = if (showSystemApps) BrandCyan else BrandOnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppRuleItem(
    rule: FirewallRule,
    onWifiToggle: (Boolean) -> Unit,
    onMobileToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.appName.ifEmpty { rule.packageName },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                )
                if (rule.blockedCount > 0) {
                    Text(
                        text = "${rule.blockedCount} blocked",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandError,
                    )
                }
            }
            Checkbox(
                checked = rule.allowWifi,
                onCheckedChange = onWifiToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = BrandCyan,
                    uncheckedColor = BrandOnSurfaceVariant,
                    checkmarkColor = Color.White,
                ),
            )
            Checkbox(
                checked = rule.allowMobile,
                onCheckedChange = onMobileToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = BrandCyan,
                    uncheckedColor = BrandOnSurfaceVariant,
                    checkmarkColor = Color.White,
                ),
            )
        }
    }
}
