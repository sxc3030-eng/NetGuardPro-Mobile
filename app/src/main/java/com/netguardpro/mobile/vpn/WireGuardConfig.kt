package com.netguardpro.mobile.vpn

data class WgInterface(
    val privateKey: String,
    val address: String,
    val dns: List<String> = listOf("1.1.1.1", "1.0.0.1"),
    val mtu: Int = 1420,
    val listenPort: Int? = null,
)

data class WgPeer(
    val publicKey: String,
    val endpoint: String,
    val allowedIPs: List<String> = listOf("0.0.0.0/0", "::/0"),
    val persistentKeepalive: Int = 25,
    val presharedKey: String? = null,
)

data class WireGuardConfig(
    val interfaceConfig: WgInterface,
    val peers: List<WgPeer>,
) {
    fun toConfString(): String = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = ${interfaceConfig.privateKey}")
        appendLine("Address = ${interfaceConfig.address}")
        appendLine("DNS = ${interfaceConfig.dns.joinToString(", ")}")
        appendLine("MTU = ${interfaceConfig.mtu}")
        interfaceConfig.listenPort?.let { appendLine("ListenPort = $it") }
        appendLine()

        peers.forEach { peer ->
            appendLine("[Peer]")
            appendLine("PublicKey = ${peer.publicKey}")
            appendLine("Endpoint = ${peer.endpoint}")
            appendLine("AllowedIPs = ${peer.allowedIPs.joinToString(", ")}")
            appendLine("PersistentKeepalive = ${peer.persistentKeepalive}")
            peer.presharedKey?.let { appendLine("PresharedKey = $it") }
            appendLine()
        }
    }

    companion object {
        fun parse(confText: String): WireGuardConfig {
            var privateKey = ""
            var address = ""
            var dns = listOf("1.1.1.1", "1.0.0.1")
            var mtu = 1420
            var listenPort: Int? = null

            val peers = mutableListOf<WgPeer>()
            var currentPeerPublicKey = ""
            var currentPeerEndpoint = ""
            var currentPeerAllowedIPs = listOf("0.0.0.0/0")
            var currentPeerKeepalive = 25
            var currentPeerPsk: String? = null
            var inPeer = false

            fun commitPeer() {
                if (currentPeerPublicKey.isNotEmpty()) {
                    peers.add(
                        WgPeer(
                            publicKey = currentPeerPublicKey,
                            endpoint = currentPeerEndpoint,
                            allowedIPs = currentPeerAllowedIPs,
                            persistentKeepalive = currentPeerKeepalive,
                            presharedKey = currentPeerPsk,
                        )
                    )
                }
                currentPeerPublicKey = ""
                currentPeerEndpoint = ""
                currentPeerAllowedIPs = listOf("0.0.0.0/0")
                currentPeerKeepalive = 25
                currentPeerPsk = null
            }

            confText.lines().forEach { rawLine ->
                val line = rawLine.trim()
                when {
                    line.isEmpty() || line.startsWith("#") -> {}
                    line == "[Interface]" -> {
                        if (inPeer) commitPeer()
                        inPeer = false
                    }
                    line == "[Peer]" -> {
                        if (inPeer) commitPeer()
                        inPeer = true
                    }
                    else -> {
                        val eqIndex = line.indexOf('=')
                        if (eqIndex < 0) return@forEach
                        val key = line.substring(0, eqIndex).trim()
                        val value = line.substring(eqIndex + 1).trim()
                        if (inPeer) {
                            when (key) {
                                "PublicKey" -> currentPeerPublicKey = value
                                "Endpoint" -> currentPeerEndpoint = value
                                "AllowedIPs" -> currentPeerAllowedIPs = value.split(",").map { it.trim() }
                                "PersistentKeepalive" -> currentPeerKeepalive = value.toIntOrNull() ?: 25
                                "PresharedKey" -> currentPeerPsk = value
                            }
                        } else {
                            when (key) {
                                "PrivateKey" -> privateKey = value
                                "Address" -> address = value
                                "DNS" -> dns = value.split(",").map { it.trim() }
                                "MTU" -> mtu = value.toIntOrNull() ?: 1420
                                "ListenPort" -> listenPort = value.toIntOrNull()
                            }
                        }
                    }
                }
            }
            if (inPeer) commitPeer()

            return WireGuardConfig(
                interfaceConfig = WgInterface(
                    privateKey = privateKey,
                    address = address,
                    dns = dns,
                    mtu = mtu,
                    listenPort = listenPort,
                ),
                peers = peers,
            )
        }
    }
}

data class VpnServer(
    val name: String,
    val location: String,
    val endpoint: String,
    val publicKey: String,
    val flagEmoji: String = "",
) {
    companion object {
        val DEFAULT_SERVERS = listOf(
            VpnServer("US East", "New York, US", "us-east.netguardpro.com:51820", "PUBLIC_KEY_US_EAST", "\uD83C\uDDFA\uD83C\uDDF8"),
            VpnServer("US West", "Los Angeles, US", "us-west.netguardpro.com:51820", "PUBLIC_KEY_US_WEST", "\uD83C\uDDFA\uD83C\uDDF8"),
            VpnServer("EU Central", "Frankfurt, DE", "eu-central.netguardpro.com:51820", "PUBLIC_KEY_EU_CENTRAL", "\uD83C\uDDE9\uD83C\uDDEA"),
            VpnServer("EU West", "Amsterdam, NL", "eu-west.netguardpro.com:51820", "PUBLIC_KEY_EU_WEST", "\uD83C\uDDF3\uD83C\uDDF1"),
            VpnServer("Asia East", "Tokyo, JP", "asia-east.netguardpro.com:51820", "PUBLIC_KEY_ASIA_EAST", "\uD83C\uDDEF\uD83C\uDDF5"),
            VpnServer("Asia South", "Singapore, SG", "asia-south.netguardpro.com:51820", "PUBLIC_KEY_ASIA_SOUTH", "\uD83C\uDDF8\uD83C\uDDEC"),
        )
    }
}
