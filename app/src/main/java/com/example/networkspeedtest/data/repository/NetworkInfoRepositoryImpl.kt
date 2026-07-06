package com.example.networkspeedtest.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import com.example.networkspeedtest.data.remote.SpeedTestConfig
import com.example.networkspeedtest.domain.model.NetworkInfo
import com.example.networkspeedtest.domain.model.NetworkType
import com.example.networkspeedtest.domain.repository.NetworkInfoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

/**
 * Membaca informasi koneksi lewat ConnectivityManager dan memancarkan ulang
 * setiap kali jaringan berubah. IP publik di-fetch async agar tidak memblokir.
 */
class NetworkInfoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
    private val config: SpeedTestConfig,
) : NetworkInfoRepository {

    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val telephonyManager =
        context.getSystemService(TelephonyManager::class.java)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeNetworkInfo(): Flow<NetworkInfo> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(currentInfo()) }
            override fun onLost(network: Network) { trySend(currentInfo()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(currentInfo())
            }
        }
        trySend(currentInfo()) // state awal
        connectivityManager?.registerDefaultNetworkCallback(callback)
        awaitClose {
            runCatching { connectivityManager?.unregisterNetworkCallback(callback) }
        }
    }
        // Untuk tiap perubahan: pancarkan info dasar dulu, lalu lengkapi dengan IP
        // publik. flatMapLatest membatalkan fetch IP lama bila jaringan keburu berubah.
        .flatMapLatest { info ->
            flow {
                emit(info)
                if (info.isConnected) {
                    fetchPublicIp()?.let { ip -> emit(info.copy(publicIp = ip)) }
                }
            }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    /** Membaca jenis & nama jaringan aktif secara sinkron (tanpa IP). */
    private fun currentInfo(): NetworkInfo {
        val cm = connectivityManager ?: return NetworkInfo.Unknown
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkInfo.Unknown
        }
        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
            else -> NetworkType.OTHER
        }
        val name = when (type) {
            NetworkType.WIFI -> readWifiSsid(caps)
            NetworkType.CELLULAR -> telephonyManager?.networkOperatorName?.ifBlank { null }
            else -> null
        }
        return NetworkInfo(type = type, networkName = name, publicIp = null, isConnected = true)
    }

    /**
     * SSID hanya bisa dibaca bila izin ACCESS_FINE_LOCATION diberikan dan layanan
     * lokasi aktif; jika tidak, sistem mengembalikan "<unknown ssid>" yang kita
     * ubah menjadi null.
     */
    @Suppress("DEPRECATION")
    private fun readWifiSsid(caps: NetworkCapabilities): String? {
        val wifiInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            caps.transportInfo as? WifiInfo
        } else {
            wifiManager?.connectionInfo
        }
        val ssid = wifiInfo?.ssid?.removeSurrounding("\"")
        return ssid?.takeIf {
            it.isNotBlank() && it != WifiManager.UNKNOWN_SSID && it != "<unknown ssid>"
        }
    }

    private fun fetchPublicIp(): String? = runCatching {
        client.newCall(Request.Builder().url(config.publicIpUrl).build()).execute().use { response ->
            if (response.isSuccessful) response.body?.string()?.trim()?.ifBlank { null } else null
        }
    }.getOrNull()
}
