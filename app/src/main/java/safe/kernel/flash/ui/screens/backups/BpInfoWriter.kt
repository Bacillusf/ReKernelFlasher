package safe.kernel.flash.ui.screens.backups

/**
 * BPINFO.xml（Backup Partition Information）的生成逻辑。
 *
 * 纯函数、不依赖 Android API，便于单测；文件由调用方写入备份目录。
 */
internal object BpInfoWriter {

    const val FILE_NAME: String = "BPINFO.xml"

    fun build(
        device: String,
        platform: String,
        sourceDir: String,
        backupDir: String,
        createdAt: String,
        hashAlgorithm: String,
        entries: List<BackupsViewModel.BpInfoEntry>
    ): String {
        val totalBytes = entries.sumOf { it.sizeBytes }
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<BackupPartitionInformation version=\"1\"")
            append(" device=\"").append(escape(device)).append('"')
            append(" platform=\"").append(escape(platform)).append('"')
            append(" source=\"").append(escape(sourceDir)).append('"')
            append(" backup=\"").append(escape(backupDir)).append('"')
            append(" createdAt=\"").append(escape(createdAt)).append('"')
            append(" hashAlgorithm=\"").append(escape(hashAlgorithm)).append('"')
            append(" partitionCount=\"").append(entries.size).append('"')
            append(" totalSize=\"").append(totalBytes).append("\">\n")
            entries.forEach { entry ->
                append("  <Partition name=\"").append(escape(entry.name)).append('"')
                append(" source=\"").append(escape(entry.sourcePath)).append('"')
                append(" file=\"").append(escape(entry.fileName)).append('"')
                append(" size=\"").append(entry.sizeBytes).append('"')
                append(" hash=\"").append(escape(entry.hash)).append("\"/>\n")
            }
            append("</BackupPartitionInformation>\n")
        }
    }

    fun escape(value: String): String = buildString(value.length) {
        value.forEach { c ->
            when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(c)
            }
        }
    }
}
