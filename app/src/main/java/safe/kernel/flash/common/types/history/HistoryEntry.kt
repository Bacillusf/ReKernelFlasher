package safe.kernel.flash.common.types.history

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class HistoryEntry(
    val timestamp: String,
    val description: String
) {
    companion object {
        fun create(description: String): HistoryEntry {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH时"))
            return HistoryEntry(now, description)
        }
    }
}
