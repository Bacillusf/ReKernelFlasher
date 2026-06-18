package safe.kernel.flash.ui.screens.main

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import safe.kernel.flash.ui.components.DataCard
import safe.kernel.flash.ui.components.DataRow

@ExperimentalMaterial3Api
@Composable
fun ColumnScope.MainContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    AnimatedVisibility(viewModel.updateAvailable) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text("有新的更新 版本: v${viewModel.updateVersion}",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(12.dp, 8.dp, 12.dp, 0.dp))
            if (viewModel.isDownloading) {
                LinearProgressIndicator(progress = { viewModel.downloadProgress },
                    modifier = Modifier.fillMaxWidth().padding(12.dp, 4.dp, 12.dp, 0.dp))
                Text("${(viewModel.downloadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp, 2.dp, 12.dp, 4.dp))
            } else {
                Row(Modifier.padding(12.dp, 4.dp, 12.dp, 8.dp)) {
                    Button(onClick = { viewModel.startDownload() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("下载更新") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { viewModel.updateAvailable = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) { Text("忽略") }
                }
            }
        }
    }

    MiuixStatusCard(viewModel)
    Spacer(Modifier.height(12.dp))

    DataCard(title = "设备") {
        val cardWidth = remember { mutableIntStateOf(0) }
        DataRow("型号", "${Build.MODEL} (${Build.DEVICE})", mutableMaxWidth = cardWidth)
        DataRow("构建版本", Build.ID, mutableMaxWidth = cardWidth)
        DataRow("Android 版本", viewModel.androidVersion, mutableMaxWidth = cardWidth)
        DataRow("应用版本", viewModel.appVersion, mutableMaxWidth = cardWidth)
        DataRow("内核版本", viewModel.kernelVersion, mutableMaxWidth = cardWidth, clickable = true)
        DataRow("Root 管理器", viewModel.rootManager, mutableMaxWidth = cardWidth)
        if (viewModel.isAb) DataRow("槽位后缀", viewModel.slotSuffix, mutableMaxWidth = cardWidth)
        if (viewModel.halInfo != "") DataRow("Boot HAL", viewModel.halInfo, mutableMaxWidth = cardWidth)
        if (viewModel.susfsVersion != "v0.0.0" && viewModel.susfsVersion != "Invalid")
            DataRow("SUSFS 版本", viewModel.susfsVersion, mutableMaxWidth = cardWidth)
    }
}

@Composable
private fun MiuixStatusCard(viewModel: MainViewModel) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF1A3825) else Color(0xFFD6E4FF)
    val iconTint = if (isDark) Color(0xFF81C995) else Color(0xFF4A6FA5)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Filled.CheckCircle, contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text("工作中[${viewModel.rootManager}]",
                    fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(if (isDark) 0xFFE8EAED else 0xFF1A1A1A))
                Spacer(Modifier.height(2.dp))
                Text("App: ${viewModel.appVersion}",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = Color(if (isDark) 0xFF9AA0A6 else 0xFF5F6368))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiuixStatCard("Android", viewModel.androidVersion, isDark)
                MiuixStatCard("型号", "${Build.MODEL} (${Build.DEVICE})", isDark)
            }
        }
    }
}

@Composable
private fun MiuixStatCard(label: String, value: String, isDark: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 10.sp, color = Color(if (isDark) 0xFF9AA0A6 else 0xFF5F6368))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = Color(if (isDark) 0xFFE8EAED else 0xFF1A1A1A))
        }
    }
}
