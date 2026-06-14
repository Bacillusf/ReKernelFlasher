package safe.kernel.flash

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager

class FilesystemService : RootService() {
    inner class FilesystemIPC : IFilesystemService.Stub() {
        override fun getFileSystemService(): IBinder {
            return FileSystemManager.getService()
        }
    }
    override fun onBind(intent: Intent): IBinder {
        return FilesystemIPC()
    }
}
