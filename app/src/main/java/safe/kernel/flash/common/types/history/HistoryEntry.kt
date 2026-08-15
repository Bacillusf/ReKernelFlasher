package safe.kernel.flash.common.types.history

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class HistoryEntry(
    val timestamp: String,
    val description: String,
    val kind: String = KIND_NORMAL,
    val rollbackTimestamp: Long = -1L
) {
    companion object {
        const val KIND_NORMAL = "normal"
        const val KIND_AK3 = "ak3"

        fun create(description: String): HistoryEntry =
            HistoryEntry(now(), description)

        fun createAk3(description: String, rollbackTimestamp: Long): HistoryEntry =
            HistoryEntry(now(), description, KIND_AK3, rollbackTimestamp)

        private fun now(): String =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH时"))
    }
}
