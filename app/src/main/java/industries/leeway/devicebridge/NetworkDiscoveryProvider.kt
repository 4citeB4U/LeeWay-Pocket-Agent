package industries.leeway.devicebridge

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import org.json.JSONArray
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object NetworkDiscoveryProvider {
    const val CAPABILITY = "device.network.discover"

    private val nsdTypes = listOf(
        "_home-assistant._tcp.",
        "_http._tcp.",
        "_googlecast._tcp.",
        "_airplay._tcp."
    )

    fun discover(context: Context): JSONObject {
        val started = System.currentTimeMillis()
        val ssdp = discoverSsdp(context, 1800L)
        val nsd = discoverNsd(context, 750L)
        val total = ssdp.length() + nsd.length()

        return JSONObject().apply {
            put("providerId", "network-discovery")
            put("capability", CAPABILITY)
            put("supported", true)
            put("available", true)
            put("authorized", true)
            put("active", true)
            put("healthy", true)
            put("verified", true)
            put("discoveryOnly", true)
            put("actuationAvailable", false)
            put("ssdpDeviceCount", ssdp.length())
            put("dnsSdServiceCount", nsd.length())
            put("totalObservationCount", total)
            put("ssdp", ssdp)
            put("dnsSd", nsd)
            put("elapsedMs", System.currentTimeMillis() - started)
            put("authority", "PHONE_LOCAL_ANDROID_NETWORK_DISCOVERY")
        }
    }

    private fun discoverSsdp(context: Context, timeoutMs: Long): JSONArray {
        val results = JSONArray()
        val seen = linkedSetOf<String>()
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = wifi?.createMulticastLock("leeway-device-bridge-ssdp")?.apply {
            setReferenceCounted(false)
        }

        try {
            lock?.acquire()
            DatagramSocket().use { socket ->
                socket.soTimeout = 350
                val target = InetAddress.getByName("239.255.255.250")
                val request = (
                    "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 1\r\n" +
                    "ST: ssdp:all\r\n\r\n"
                ).toByteArray(Charsets.UTF_8)
                socket.send(DatagramPacket(request, request.size, target, 1900))

                val deadline = System.currentTimeMillis() + timeoutMs
                while (System.currentTimeMillis() < deadline) {
                    try {
                        val buffer = ByteArray(8192)
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val headers = parseHeaders(text)
                        val key = headers["usn"] ?: headers["location"] ?: packet.address.hostAddress
                        if (seen.add(key)) {
                            results.put(JSONObject().apply {
                                put("address", packet.address.hostAddress)
                                put("location", headers["location"])
                                put("server", headers["server"])
                                put("serviceType", headers["st"])
                                put("usn", headers["usn"])
                            })
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                    }
                }
            }
        } catch (e: Exception) {
            results.put(JSONObject().apply {
                put("error", e.javaClass.simpleName)
                put("state", "DEGRADED")
            })
        } finally {
            try {
                if (lock?.isHeld == true) lock.release()
            } catch (_: Exception) {
            }
        }

        return results
    }

    private fun discoverNsd(context: Context, perTypeMs: Long): JSONArray {
        val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val records = Collections.synchronizedMap(linkedMapOf<String, JSONObject>())

        for (serviceType in nsdTypes) {
            val stopped = CountDownLatch(1)
            lateinit var listener: NsdManager.DiscoveryListener

            listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {}

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    val key = serviceInfo.serviceName + "|" + serviceInfo.serviceType
                    records[key] = JSONObject().apply {
                        put("name", serviceInfo.serviceName)
                        put("serviceType", serviceInfo.serviceType)
                        put("state", "OBSERVED")
                    }

                    @Suppress("DEPRECATION")
                    manager.resolveService(
                        serviceInfo,
                        object : NsdManager.ResolveListener {
                            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}

                            override fun onServiceResolved(info: NsdServiceInfo) {
                                records[key] = JSONObject().apply {
                                    put("name", info.serviceName)
                                    put("serviceType", info.serviceType)
                                    @Suppress("DEPRECATION")
                                    put("host", info.host?.hostAddress)
                                    put("port", info.port)
                                    put("state", "RESOLVED")
                                }
                            }
                        }
                    )
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    try { manager.stopServiceDiscovery(this) } catch (_: Exception) {}
                    stopped.countDown()
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    stopped.countDown()
                }
                override fun onDiscoveryStopped(serviceType: String) {
                    stopped.countDown()
                }
            }

            try {
                manager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
                Thread.sleep(perTypeMs)
            } catch (_: Exception) {
            } finally {
                try { manager.stopServiceDiscovery(listener) } catch (_: Exception) {}
                stopped.await(250, TimeUnit.MILLISECONDS)
            }
        }

        val array = JSONArray()
        synchronized(records) {
            records.values.forEach(array::put)
        }
        return array
    }

    private fun parseHeaders(response: String): Map<String, String> =
        response.lineSequence()
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) null
                else line.substring(0, idx).trim().lowercase() to line.substring(idx + 1).trim()
            }
            .toMap()
}
