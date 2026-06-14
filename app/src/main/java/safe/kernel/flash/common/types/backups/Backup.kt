package safe.kernel.flash.common.types.backups

import safe.kernel.flash.common.types.partitions.Partitions
import kotlinx.serialization.Serializable

@Serializable
data class Backup(
    val name: String,
    val type: String,
    val kernelVersion: String,
    val bootSha1: String? = null,
    val filename: String? = null,
    val hashes: Partitions? = null,
    val hashAlgorithm: String? = null
)
