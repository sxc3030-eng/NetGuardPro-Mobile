package com.netguardpro.mobile.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Vpn : Screen("vpn")
    data object Firewall : Screen("firewall")
    data object Cleaner : Screen("cleaner")
}
