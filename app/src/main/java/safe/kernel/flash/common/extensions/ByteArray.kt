package safe.kernel.flash.common.extensions

import kotlin.ByteArray

object ByteArray {
    fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }
}
