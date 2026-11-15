package io.github.willyrafaelfs.speedtest.domain.usecase

import io.github.willyrafaelfs.speedtest.domain.model.NetworkInfo
import io.github.willyrafaelfs.speedtest.domain.repository.NetworkInfoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Mengamati informasi koneksi jaringan terkini secara reaktif. */
class GetNetworkInfoUseCase @Inject constructor(
    private val networkInfoRepository: NetworkInfoRepository,
) {
    operator fun invoke(): Flow<NetworkInfo> = networkInfoRepository.observeNetworkInfo()
}
