package com.netguardpro.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.netguardpro.mobile.ui.navigation.Screen
import com.netguardpro.mobile.ui.screens.DashboardScreen
import com.netguardpro.mobile.ui.theme.BrandBackground
import com.netguardpro.mobile.ui.theme.BrandCyan
import com.netguardpro.mobile.ui.theme.BrandOnSurfaceVariant
import com.netguardpro.mobile.ui.theme.BrandSurface
import com.netguardpro.mobile.ui.theme.NetGuardProTheme
import com.netguardpro.mobile.vpn.VpnScreen
import com.netguardpro.mobile.firewall.FirewallScreen
import com.netguardpro.mobile.cleaner.CleanerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetGuardProTheme {
                NetGuardProApp()
            }
        }
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun NetGuardProApp() {
    val navController = rememberNavController()

    val navItems = listOf(
        BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        BottomNavItem(Screen.Vpn, "VPN", Icons.Filled.VpnKey, Icons.Outlined.VpnKey),
        BottomNavItem(Screen.Firewall, "Firewall", Icons.Filled.Security, Icons.Outlined.Security),
        BottomNavItem(Screen.Cleaner, "Cleaner", Icons.Filled.CleaningServices, Icons.Outlined.CleaningServices),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BrandBackground,
        bottomBar = {
            NavigationBar(
                containerColor = BrandSurface,
                contentColor = BrandCyan,
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandCyan,
                            selectedTextColor = BrandCyan,
                            unselectedIconColor = BrandOnSurfaceVariant,
                            unselectedTextColor = BrandOnSurfaceVariant,
                            indicatorColor = BrandCyan.copy(alpha = 0.15f),
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Vpn.route) { VpnScreen() }
            composable(Screen.Firewall.route) { FirewallScreen() }
            composable(Screen.Cleaner.route) { CleanerScreen() }
        }
    }
}
