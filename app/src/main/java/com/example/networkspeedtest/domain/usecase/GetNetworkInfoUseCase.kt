package com.example.networkspeedtest.domain.usecase

import com.example.networkspeedtest.domain.model.NetworkInfo
import com.example.networkspeedtest.domain.repository.NetworkInfoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Mengamati informasi koneksi jaringan terkini secara reaktif. */
class GetNetworkInfoUseCase @Inject constructor(
    private val networkInfoRepository: NetworkInfoRepository,
) {
    operator fun invoke(): Flow<NetworkInfo> = networkInfoRepository.observeNetworkInfo()
}
