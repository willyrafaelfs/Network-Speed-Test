package io.github.willyrafaelfs.speedtest.testing

import io.github.willyrafaelfs.speedtest.domain.model.NetworkInfo
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.repository.NetworkInfoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** NetworkInfoRepository yang nilainya bisa diubah test. */
class FakeNetworkInfoRepository(
    initial: NetworkInfo = NetworkInfo(NetworkType.WIFI, "Lab", "203.0.113.7", isConnected = true),
) : NetworkInfoRepository {
    val info = MutableStateFlow(initial)

    override fun observeNetworkInfo(): StateFlow<NetworkInfo> = info
}
