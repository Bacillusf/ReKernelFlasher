package safe.kernel.flash.ui.screens.repo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import safe.kernel.flash.ui.components.AnimatedConfirmDialog
import safe.kernel.flash.ui.components.DialogButton
import safe.kernel.flash.ui.theme.softShadow

private val OfficialGreen = Color(0xFF2E7D32)
private val CommunityOrange = Color(0xFFEF6C00)
private val UniversalBlue = Color(0xFF1565C0)
private val OnePlusRed = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoContent(navController: NavController) {
    val viewModel: RepoViewModel = viewModel()
    val context = LocalContext.current
    val source = viewModel.source
    val items = viewModel.items
    val isLoading = viewModel.isLoading
    val loadError = viewModel.loadError
    val pendingFlash = viewModel.pendingFlashItem
    val infoItem = viewModel.infoItem
    val isOfficial = source == RepoSource.OFFICIAL

    var communityWarnShown by remember { mutableStateOf(false) }
    var showCommunityWarning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetch(context)
    }
    LaunchedEffect(source) {
        if (source == RepoSource.COMMUNITY && !communityWarnShown) {
            communityWarnShown = true
            showCommunityWarning = true
        }
    }

    val managerLabels = listOf("全部管理器") + viewModel.managerOptions
    val typeLabel = when (viewModel.typeFilter) {
        TypeFilter.ALL -> "类型"
        TypeFilter.UNIVERSAL -> "通刷"
        TypeFilter.ONEPLUS -> "机型"
    }
    val seriesLabel = viewModel.versionKey ?: "版本"
    val managerLabel = viewModel.managerKey ?: "管理器"
    val seriesLabels = listOf("全部内核版本") + viewModel.seriesOptions

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "内核仓库",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Filled.Cloud,
                            contentDescription = "云端",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = viewModel.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "搜索") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { viewModel.fetch(context, force = true) },
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterDropdown(
                    label = source.label,
                    options = RepoSource.entries.map { it.label },
                    onSelect = { viewModel.selectSource(context, RepoSource.entries[it]) },
                    modifier = Modifier.weight(1f)
                )
                if (isOfficial) {
                    FilterDropdown(
                        label = typeLabel,
                        options = TypeFilter.entries.map { it.label },
                        onSelect = { viewModel.selectTypeFilter(TypeFilter.entries[it]) },
                        modifier = Modifier.weight(0.95f)
                    )
                    FilterDropdown(
                        label = seriesLabel,
                        options = seriesLabels,
                        onSelect = {
                            viewModel.selectVersionFilter(
                                if (it == 0) null else viewModel.seriesOptions.getOrNull(it - 1)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                FilterDropdown(
                    label = managerLabel,
                    options = managerLabels,
                    onSelect = {
                        viewModel.selectManagerFilter(
                            if (it == 0) null else viewModel.managerOptions.getOrNull(it - 1)
                        )
                    },
                    modifier = Modifier.weight(if (isOfficial) 1.1f else 1.4f)
                )
            }
            Spacer(Modifier.height(10.dp))
            SourceBanner(
                source = source,
                resultCount = items.size,
                universalCount = viewModel.universalCount,
                onePlusCount = viewModel.onePlusCount,
                fetchedAt = viewModel.lastFetchedAt
            )
            Spacer(Modifier.height(10.dp))
            loadError?.let {
                Text(
                    text = "加载失败: $it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(6.dp))
            }
            when {
                isLoading && items.isEmpty() -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "正在获取${source.label}列表...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items.isEmpty() -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "暂无匹配的内核包",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(items, key = { it.url }) { item ->
                            RepoCard(
                                item = item,
                                state = viewModel.downloadStates[item.url],
                                onDownload = { viewModel.download(context, item) },
                                onInfo = if (item.source == RepoSource.COMMUNITY) {
                                    { viewModel.showInfo(item) }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCommunityWarning) {
        AnimatedConfirmDialog(
            visible = true,
            title = "社区源提示",
            message = "社区源内的资源由用户上传，未经官方验证。\n\n请谨慎使用，注意鉴别，认准作者。" +
                "刷写前建议先备份 boot / dtbo，并确认机型与内核版本匹配。",
            confirmText = "我已知晓",
            cancelText = "返回官方源",
            onConfirm = { showCommunityWarning = false },
            onDismiss = {
                showCommunityWarning = false
                viewModel.selectSource(context, RepoSource.OFFICIAL)
            },
            destructive = true
        )
    }

    infoItem?.let { item ->
        CommunityInfoDialog(item = item, onDismiss = { viewModel.hideInfo() })
    }

    pendingFlash?.let { item ->
        AnimatedConfirmDialog(
            visible = true,
            title = "下载完成",
            message = "${item.filename}\n\n是否刷写到活跃槽位？",
            confirmText = "刷写",
            cancelText = "取消",
            onConfirm = {
                viewModel.clearPending()
                val suffix = viewModel.flashToActiveSlot(item)
                navController.navigate("slot$suffix/flash/ak3") {
                    launchSingleTop = true
                }
            },
            onDismiss = {
                viewModel.clearPending()
                Toast.makeText(context, "已保存到 /sdcard/Download", Toast.LENGTH_SHORT).show()
            },
            destructive = true
        )
    }
}

@Composable
private fun SourceBanner(
    source: RepoSource,
    resultCount: Int,
    universalCount: Int,
    onePlusCount: Int,
    fetchedAt: Long?
) {
    val accent = if (source == RepoSource.OFFICIAL) OfficialGreen else CommunityOrange
    val title = if (source == RepoSource.OFFICIAL) {
        "官方源 · 内核通刷 $universalCount + OnePlus机型 $onePlusCount"
    } else {
        "社区源 · 用户上传，未经官方验证"
    }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(accent, CircleShape))
            Spacer(Modifier.width(7.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "共 $resultCount 个内核包" +
                (fetchedAt?.let { " · 数据更新于 ${RepoTime.clock(it)}" } ?: ""),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 3.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    }
                )
            }
        }
    }
}

@Composable
private fun TagBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.13f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepoCard(
    item: RepoItem,
    state: DownloadState?,
    onDownload: () -> Unit,
    onInfo: (() -> Unit)?
) {
    val downloading = state?.downloading == true
    val completed = state?.completed == true
    val error = state?.error

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(cornerRadius = 18.dp, alpha = 0.06f, offsetY = 2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when (item.source) {
                    RepoSource.OFFICIAL -> TagBadge("官方", OfficialGreen)
                    RepoSource.COMMUNITY -> TagBadge("社区", CommunityOrange)
                }
                when (item.kind) {
                    RepoKind.UNIVERSAL -> TagBadge("内核通刷", UniversalBlue)
                    RepoKind.ONEPLUS -> TagBadge("OnePlus机型", OnePlusRed)
                    RepoKind.COMMUNITY -> Unit
                }
            }
            if (item.isLatest) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Latest ${item.manager ?: "未知管理器"} Anykernel3",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.filename,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RedTag(item.managerVersion)
                RedTag(item.line2)
            }
            if (item.features.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.features.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            if (item.source == RepoSource.COMMUNITY) {
                val meta = buildList {
                    item.sizeHuman?.let { add(it) }
                    item.authorLogin?.let { add("@$it") }
                    RepoTime.human(item.publishedAt)?.let { add(it) }
                }
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = meta.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (downloading) {
                    Column(Modifier.weight(1f)) {
                        LinearProgressIndicator(
                            progress = { state?.progress ?: 0f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${((state?.progress ?: 0f) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (onInfo != null) {
                    Spacer(Modifier.width(6.dp))
                    FilledTonalButton(
                        onClick = onInfo,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "资源信息",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                FilledTonalButton(
                    onClick = onDownload,
                    enabled = !downloading && !completed,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        if (completed) Icons.Filled.CheckCircle else Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (completed) "已下载" else "下载",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            error?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun RedTag(text: String) {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.errorContainer,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CommunityInfoDialog(item: RepoItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
                .softShadow(cornerRadius = 20.dp, alpha = 0.18f, offsetY = 8.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "资源信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭")
                    }
                }
                Text(
                    text = item.filename,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    InfoRow("适用机型 / 内核版本", item.deviceModel ?: "未填写")
                    InfoRow("管理器", item.manager ?: "无")
                    InfoRow("管理器版本", item.managerVer ?: "—")
                    InfoRow("内核版本", item.kernelVersion ?: "—")
                    InfoRow("特性", item.features.joinToString(" / ").ifEmpty { "未填写" })
                    InfoRow("体积", item.sizeHuman ?: "—")
                    InfoRow("上传时间", RepoTime.full(item.publishedAt))
                    if (!item.notes.isNullOrBlank()) {
                        InfoRow("补充说明", item.notes)
                    }
                    InfoRow(
                        label = "上传者",
                        value = "@" + (item.authorLogin ?: "未知"),
                        onClick = item.authorUrl?.let { url -> { openUrl(context, url) } }
                    )
                    InfoRow("SHA-256", item.sha256 ?: "—", monospace = true)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DialogButton(buttonText = "复制哈希", onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as? ClipboardManager
                        val hash = item.sha256
                        if (clipboard != null && !hash.isNullOrBlank()) {
                            clipboard.setPrimaryClip(ClipData.newPlainText("SHA-256", hash))
                            Toast.makeText(context, "已复制 SHA-256", Toast.LENGTH_SHORT).show()
                        }
                    })
                    item.githubUrl?.let { url ->
                        DialogButton(buttonText = "仓库页", onClick = { openUrl(context, url) })
                    }
                    Spacer(Modifier.weight(1f))
                    DialogButton(buttonText = "关闭", onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    monospace: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (onClick != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}

private fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
    }
}
