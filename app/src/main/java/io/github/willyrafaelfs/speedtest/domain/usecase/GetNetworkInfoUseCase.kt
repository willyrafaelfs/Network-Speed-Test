package io.github.willyrafaelfs.speedtest.domain.usecase

import io.github.willyrafaelfs.speedtest.domain.model.NetworkInfo
import io.github.willyrafaelfs.speedtest.domain.repository.NetworkInfoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Mengamati informasi koneksi jaringan terkini secara reaktif. */
class GetNetworkInfoUseCase @Inject constructor(
    private val networkInfoRepository: NetworkInfoRepository,
) {
    operator fun invoke(): Flow<NetworkInfo> = networkInfoRepository.observeNetworkInfo()
}
