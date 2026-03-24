# NetGuardPro Mobile ProGuard Rules

# Keep WireGuard tunnel classes
-keep class com.wireguard.** { *; }

# Keep Room entities
-keep class com.netguardpro.mobile.data.** { *; }

# Keep VPN service
-keep class com.netguardpro.mobile.vpn.NetGuardVpnService { *; }
-keep class com.netguardpro.mobile.firewall.FirewallService { *; }
