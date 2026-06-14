package safe.kernel.flash.common.types.partitions

import kotlinx.serialization.Serializable

@Serializable
data class FsMgrFlags(
    val logical: Boolean = false
)
