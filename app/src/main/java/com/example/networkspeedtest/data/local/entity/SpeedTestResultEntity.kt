package com.example.networkspeedtest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.networkspeedtest.domain.model.NetworkType
import com.example.networkspeedtest.domain.model.SpeedTestResult

/**
 * Representasi baris tabel Room untuk satu hasil test.
 *
 * NetworkType disimpan sebagai String (nama enum) agar tidak perlu TypeConverter;
 * konversi ke/dari domain ditangani mapper di bawah.
 */
@Entity(tableName = "speed_test_results")
data class SpeedTestResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val pingMs: Double,
    val jitterMs: Double,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val networkType: String,
    val networkName: String?,
)

/** Entity (Room) -> model domain. Enum tak dikenal jatuh ke OTHER agar aman. */
fun SpeedTestResultEntity.toDomain(): SpeedTestResult = SpeedTestResult(
    id = id,
    timestamp = timestamp,
    pingMs = pingMs,
    jitterMs = jitterMs,
    downloadMbps = downloadMbps,
    uploadMbps = uploadMbps,
    networkType = runCatching { NetworkType.valueOf(networkType) }.getOrDefault(NetworkType.OTHER),
    networkName = networkName,
)

/** Model domain -> entity (Room). */
fun SpeedTestResult.toEntity(): SpeedTestResultEntity = SpeedTestResultEntity(
    id = id,
    timestamp = timestamp,
    pingMs = pingMs,
    jitterMs = jitterMs,
    downloadMbps = downloadMbps,
    uploadMbps = uploadMbps,
    networkType = networkType.name,
    networkName = networkName,
)
