package safe.kernel.flash.ui.screens.main

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Smartphone
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import safe.kernel.flash.ui.components.DataCard
import safe.kernel.flash.ui.components.DataRow
import safe.kernel.flash.ui.theme.gradientBackground
import safe.kernel.flash.ui.theme.softShadow
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSerializationApi::class)
@Composable
fun ColumnScope.MainContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    AnimatedVisibility(viewModel.updateAvailable) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .softShadow(cornerRadius = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                "有新的更新 版本: v${viewModel.updateVersion}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 0.dp)
            )
            if (viewModel.isDownloading) {
                LinearProgressIndicator(
                    progress = { viewModel.downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 6.dp, 16.dp, 0.dp)
                )
                Text(
                    "${(viewModel.downloadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp, 2.dp, 16.dp, 6.dp)
                )
            } else {
                Row(Modifier.padding(16.dp, 6.dp, 16.dp, 12.dp)) {
                    Button(
                        onClick = { viewModel.startDownload() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("下载更新") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.updateAvailable = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("忽略") }
                }
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    HeroStatusCard(viewModel)
    Spacer(Modifier.height(16.dp))

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

@OptIn(ExperimentalSerializationApi::class)
@Composable
private fun HeroStatusCard(viewModel: MainViewModel) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(Color(0xFF1A3825), Color(0xFF2C3E5C))
    } else {
        listOf(Color(0xFF4A6FA5), Color(0xFF34A853))
    }
    val onGradient = if (isDark) Color(0xFFE8EAED) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(cornerRadius = 24.dp, alpha = 0.18f, offsetY = 8.dp)
            .gradientBackground(
                colors = gradientColors,
                start = androidx.compose.ui.geometry.Offset.Zero,
                end = androidx.compose.ui.geometry.Offset.Infinite,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = onGradient,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Root 已激活",
                    style = MaterialTheme.typography.labelMedium,
                    color = onGradient.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = viewModel.rootManager,
                    style = MaterialTheme.typography.headlineSmall,
                    color = onGradient,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "App v${viewModel.appVersion}  ·  Android ${viewModel.androidVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = onGradient.copy(alpha = 0.85f)
                )
            }
        }
    }
}
