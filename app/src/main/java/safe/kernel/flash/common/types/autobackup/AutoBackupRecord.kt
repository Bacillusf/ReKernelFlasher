package safe.kernel.flash.common.types.autobackup

import kotlinx.serialization.Serializable

@Serializable
data class AutoBackupRecord(
    val timestamp: Long,
    val partition: String,
    val slot: String,
    val path: String
) {
    companion object {
        fun create(timestamp: Long, partition: String, slot: String, path: String) =
            AutoBackupRecord(timestamp, partition, slot, path)
    }
}
