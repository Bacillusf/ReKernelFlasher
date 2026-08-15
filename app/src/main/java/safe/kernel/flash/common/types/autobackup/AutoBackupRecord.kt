package safe.kernel.flash.common.types.autobackup

import kotlinx.serialization.Serializable

@Serializable
data class AutoBackupItem(
    val partition: String,
    val slot: String,
    val source: String = "",
    val path: String
)

@Serializable
data class AutoBackupRecord(
    val timestamp: Long,
    val partition: String = "",
    val slot: String = "",
    val path: String = "",
    val kind: String = KIND_IMAGE,
    val items: List<AutoBackupItem> = emptyList()
) {
    companion object {
        const val KIND_IMAGE = "image"
        const val KIND_AK3 = "ak3"

        fun create(timestamp: Long, partition: String, slot: String, path: String) =
            AutoBackupRecord(timestamp, partition, slot, path, KIND_IMAGE, emptyList())

        fun createAk3(timestamp: Long, items: List<AutoBackupItem>) =
            AutoBackupRecord(timestamp, "", "", "", KIND_AK3, items)
    }
}
