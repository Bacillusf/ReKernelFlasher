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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.serialization.ExperimentalSerializationApi
import safe.kernel.flash.ui.components.Card
import safe.kernel.flash.ui.components.DataCard
import safe.kernel.flash.ui.components.DataRow
import safe.kernel.flash.ui.components.ListItem
import safe.kernel.flash.ui.components.ListItemIconColors
import safe.kernel.flash.ui.theme.softShadow

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun ColumnScope.MainContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    AnimatedVisibility(viewModel.updateAvailable) {
        UpdateBanner(viewModel)
    }

    HeroStatusCard(viewModel)
    Spacer(Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickInfoChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Android,
            title = "Android",
            value = viewModel.androidVersion,
        )
        QuickInfoChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Security,
            title = "Root",
            value = viewModel.rootManager.ifBlank { "Unknown" },
        )
    }
    Spacer(Modifier.height(14.dp))

    DataCard(title = "设备信息") {
        val cardWidth = remember { mutableIntStateOf(0) }
        DataRow("型号", "${Build.MODEL} (${Build.DEVICE})", mutableMaxWidth = cardWidth)
        DataRow("构建版本", Build.ID, mutableMaxWidth = cardWidth)
        DataRow("应用版本", viewModel.appVersion, mutableMaxWidth = cardWidth)
        DataRow("内核版本", viewModel.kernelVersion, mutableMaxWidth = cardWidth, clickable = true)
        if (viewModel.isAb) DataRow("槽位后缀", viewModel.slotSuffix, mutableMaxWidth = cardWidth)
        if (viewModel.halInfo.isNotEmpty()) DataRow("Boot HAL", viewModel.halInfo, mutableMaxWidth = cardWidth)
        if (viewModel.susfsVersion != "v0.0.0" && viewModel.susfsVersion != "Invalid") {
            DataRow("SUSFS 版本", viewModel.susfsVersion, mutableMaxWidth = cardWidth)
        }
    }
    Spacer(Modifier.height(12.dp))

    ListItem(
        title = "Verified Boot",
        subtitle = "Verity: ${viewModel.avbVerityStatus}  ·  Verification: ${viewModel.avbVerificationStatus}",
        leadingIcon = Icons.Filled.Memory,
        leadingColors = ListItemIconColors(
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        enabled = false,
    )
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
private fun UpdateBanner(viewModel: MainViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "发现新版本 v${viewModel.updateVersion}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (viewModel.isDownloading) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { viewModel.downloadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${(viewModel.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        if (!viewModel.isDownloading) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.startDownload() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { Text("下载更新") }
                Button(
                    onClick = { viewModel.updateAvailable = false },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) { Text("忽略") }
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
private fun HeroStatusCard(viewModel: MainViewModel) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(Color(0xFF14251B), Color(0xFF1E2D45), Color(0xFF233B2B))
    } else {
        listOf(Color(0xFF4A6FA5), Color(0xFF34A853), Color(0xFF8FD6A1))
    }
    val onGradient = Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(cornerRadius = 28.dp, alpha = 0.16f, offsetY = 8.dp)
            .background(
                brush = Brush.linearGradient(gradientColors),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = onGradient,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Root 已激活",
                        style = MaterialTheme.typography.labelLarge,
                        color = onGradient.copy(alpha = 0.82f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = viewModel.rootManager,
                        style = MaterialTheme.typography.headlineSmall,
                        color = onGradient,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${viewModel.appVersionFull} · Android ${viewModel.androidVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = onGradient.copy(alpha = 0.86f)
                    )
                }
            }
            Text(
                text = viewModel.kernelVersion.ifBlank { "Kernel version unavailable" },
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = onGradient.copy(alpha = 0.88f),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun QuickInfoChip(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    value: String,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
