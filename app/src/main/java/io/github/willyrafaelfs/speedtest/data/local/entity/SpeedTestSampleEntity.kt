package io.github.willyrafaelfs.speedtest.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResultDetail
import io.github.willyrafaelfs.speedtest.domain.model.TracePoint

/**
 * Satu titik jejak kecepatan milik sebuah hasil test (skema v3). Terhapus otomatis
 * (CASCADE) saat hasilnya dihapus.
 *
 * @param phase "DOWNLOAD" atau "UPLOAD".
 */
@Entity(
    tableName = "speed_test_samples",
    foreignKeys = [
        ForeignKey(
            entity = SpeedTestResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["resultId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("resultId")],
)
data class SpeedTestSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val resultId: Long,
    val phase: String,
    val progress: Float,
    val mbps: Float,
) {
    companion object {
        const val PHASE_DOWNLOAD = "DOWNLOAD"
        const val PHASE_UPLOAD = "UPLOAD"
    }
}

/** Hasil test beserta seluruh sampelnya (relasi one-to-many Room). */
data class ResultWithSamples(
    @Embedded val result: SpeedTestResultEntity,
    @Relation(parentColumn = "id", entityColumn = "resultId")
    val samples: List<SpeedTestSampleEntity>,
)

fun ResultWithSamples.toDomain(): SpeedTestResultDetail {
    fun trace(phase: String) = samples
        .filter { it.phase == phase }
        .sortedBy { it.progress }
        .map { TracePoint(it.progress, it.mbps) }
    return SpeedTestResultDetail(
        result = result.toDomain(),
        downloadTrace = trace(SpeedTestSampleEntity.PHASE_DOWNLOAD),
        uploadTrace = trace(SpeedTestSampleEntity.PHASE_UPLOAD),
    )
}

fun List<TracePoint>.toSampleEntities(resultId: Long, phase: String) =
    map { SpeedTestSampleEntity(resultId = resultId, phase = phase, progress = it.progress, mbps = it.mbps) }
