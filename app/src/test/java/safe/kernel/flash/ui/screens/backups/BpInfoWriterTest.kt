package safe.kernel.flash.ui.screens.backups

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BpInfoWriterTest {

    private fun entry(
        name: String,
        size: Long,
        hash: String = "deadbeef",
        source: String = "/dev/block/by-name/$name"
    ) = BackupsViewModel.BpInfoEntry(
        name = name,
        sourcePath = source,
        fileName = "$name.img",
        sizeBytes = size,
        hash = hash
    )

    @Test
    fun `writes xml header and root element`() {
        val xml = BpInfoWriter.build(
            device = "PJZ110",
            platform = "高通",
            sourceDir = "/dev/block/bootdevice/by-name",
            backupDir = "/sdcard/ReKernelFlasher/backups/PJZ110字库备份",
            createdAt = "2026-11-27T22:30:00",
            hashAlgorithm = "SHA-256",
            entries = listOf(entry("boot", 100L), entry("init_boot", 200L))
        )

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"))
        assertTrue(xml.contains("<BackupPartitionInformation version=\"1\""))
        assertTrue(xml.trimEnd().endsWith("</BackupPartitionInformation>"))
        assertTrue(xml.contains("device=\"PJZ110\""))
        assertTrue(xml.contains("platform=\"高通\""))
        assertTrue(xml.contains("hashAlgorithm=\"SHA-256\""))
        assertTrue(xml.contains("partitionCount=\"2\""))
        assertTrue(xml.contains("totalSize=\"300\""))
    }

    @Test
    fun `writes every partition attribute`() {
        val xml = BpInfoWriter.build(
            device = "device",
            platform = "高通",
            sourceDir = "/dev/block/by-name",
            backupDir = "/out",
            createdAt = "2026-11-27T22:30:00",
            hashAlgorithm = "SHA-256",
            entries = listOf(entry("init_boot", 8_388_608L, hash = "abc123"))
        )

        assertTrue(
            xml.contains(
                "  <Partition name=\"init_boot\" source=\"/dev/block/by-name/init_boot\" " +
                    "file=\"init_boot.img\" size=\"8388608\" hash=\"abc123\"/>\n"
            )
        )
    }

    @Test
    fun `escapes special characters`() {
        val xml = BpInfoWriter.build(
            device = "a&b<c>\"d\"'e'",
            platform = "高通",
            sourceDir = "/dev/block/by-name",
            backupDir = "/out",
            createdAt = "2026-11-27T22:30:00",
            hashAlgorithm = "SHA-256",
            entries = emptyList()
        )

        assertTrue(xml.contains("device=\"a&amp;b&lt;c&gt;&quot;d&quot;&apos;e&apos;\""))
    }

    @Test
    fun `empty entry list keeps totals at zero`() {
        val xml = BpInfoWriter.build(
            device = "device",
            platform = "高通",
            sourceDir = "/dev/block/by-name",
            backupDir = "/out",
            createdAt = "2026-11-27T22:30:00",
            hashAlgorithm = "SHA-256",
            entries = emptyList()
        )

        assertEquals(true, xml.contains("partitionCount=\"0\""))
        assertEquals(true, xml.contains("totalSize=\"0\""))
    }
}
