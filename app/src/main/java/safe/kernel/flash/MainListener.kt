package safe.kernel.flash

internal class MainListener(private val callback: () -> Unit) {
    fun resume() {
        callback.invoke()
    }
}
