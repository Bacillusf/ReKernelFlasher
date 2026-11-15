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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import safe.kernel.flash.R
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
    val downloadRequestItem = viewModel.downloadRequestItem
    val isOfficial = source == RepoSource.OFFICIAL
    val sourceLabel = stringResource(
        if (isOfficial) R.string.repo_source_official else R.string.repo_source_community
    )
    val sourceOptions = listOf(
        stringResource(R.string.repo_source_official),
        stringResource(R.string.repo_source_community)
    )

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

    val allManagersLabel = stringResource(R.string.repo_filter_all_managers)
    val allVersionsLabel = stringResource(R.string.repo_filter_all_versions)
    val managerLabels = listOf(allManagersLabel) + viewModel.managerOptions
    val typeLabel = when (viewModel.typeFilter) {
        TypeFilter.ALL -> stringResource(R.string.repo_filter_type)
        TypeFilter.UNIVERSAL -> stringResource(R.string.repo_filter_type_universal)
        TypeFilter.ONEPLUS -> stringResource(R.string.repo_filter_type_oneplus)
    }
    val seriesLabel = viewModel.versionKey ?: stringResource(R.string.repo_filter_version)
    val managerLabel = viewModel.managerKey ?: stringResource(R.string.repo_filter_manager)
    val seriesLabels = listOf(allVersionsLabel) + viewModel.seriesOptions

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.repo_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Filled.Cloud,
                            contentDescription = stringResource(R.string.repo_cloud),
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
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.repo_search)) },
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
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.repo_refresh))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterDropdown(
                    label = sourceLabel,
                    options = sourceOptions,
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
                sourceLabel = sourceLabel,
                source = source,
                resultCount = items.size,
                universalCount = viewModel.universalCount,
                onePlusCount = viewModel.onePlusCount,
                fetchedAt = viewModel.lastFetchedAt
            )
            Spacer(Modifier.height(10.dp))
            loadError?.let {
                Text(
                    text = stringResource(R.string.repo_load_error, it),
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
                                stringResource(R.string.repo_loading, sourceLabel),
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
                            stringResource(R.string.repo_empty),
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
                                onDownload = {
                                    if (item.source == RepoSource.COMMUNITY) {
                                        viewModel.requestDownload(item)
                                    } else {
                                        viewModel.download(context, item)
                                    }
                                },
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
            title = stringResource(R.string.repo_warn_title),
            message = stringResource(R.string.repo_warn_body),
            confirmText = stringResource(R.string.repo_warn_confirm),
            cancelText = stringResource(R.string.repo_warn_back),
            onConfirm = { showCommunityWarning = false },
            onDismiss = {
                showCommunityWarning = false
                viewModel.selectSource(context, RepoSource.OFFICIAL)
            },
            destructive = true
        )
    }

    infoItem?.let { item ->
        CommunityInfoDialog(
            item = item,
            onDismiss = { viewModel.hideInfo() },
            onDownload = null
        )
    }

    downloadRequestItem?.let { item ->
        CommunityInfoDialog(
            item = item,
            onDismiss = { viewModel.cancelDownloadRequest() },
            onDownload = {
                viewModel.cancelDownloadRequest()
                viewModel.download(context, item)
            }
        )
    }

    val savedToast = stringResource(R.string.repo_saved)
    pendingFlash?.let { item ->
        AnimatedConfirmDialog(
            visible = true,
            title = stringResource(R.string.repo_flash_confirm_title),
            message = stringResource(R.string.repo_flash_confirm_body, item.filename),
            confirmText = stringResource(R.string.flash),
            cancelText = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.clearPending()
                val suffix = viewModel.flashToActiveSlot(item)
                navController.navigate("slot$suffix/flash/ak3") {
                    launchSingleTop = true
                }
            },
            onDismiss = {
                viewModel.clearPending()
                Toast.makeText(context, savedToast, Toast.LENGTH_SHORT).show()
            },
            destructive = true
        )
    }
}

@Composable
private fun SourceBanner(
    sourceLabel: String,
    source: RepoSource,
    resultCount: Int,
    universalCount: Int,
    onePlusCount: Int,
    fetchedAt: Long?
) {
    val accent = if (source == RepoSource.OFFICIAL) OfficialGreen else CommunityOrange
    val title = if (source == RepoSource.OFFICIAL) {
        stringResource(R.string.repo_banner_official, universalCount, onePlusCount)
    } else {
        stringResource(R.string.repo_banner_community)
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
            text = stringResource(R.string.repo_count, resultCount) +
                (fetchedAt?.let { " · " + stringResource(R.string.repo_updated_at, RepoTime.clock(it)) } ?: ""),
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
                    RepoSource.OFFICIAL -> TagBadge(stringResource(R.string.repo_badge_official), OfficialGreen)
                    RepoSource.COMMUNITY -> TagBadge(stringResource(R.string.repo_badge_community), CommunityOrange)
                }
                when (item.kind) {
                    RepoKind.UNIVERSAL -> TagBadge(stringResource(R.string.repo_kind_universal), UniversalBlue)
                    RepoKind.ONEPLUS -> TagBadge(stringResource(R.string.repo_kind_oneplus), OnePlusRed)
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
                        text = stringResource(
                            R.string.repo_latest_label,
                            item.manager ?: stringResource(R.string.repo_info_manager)
                        ),
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
            if (item.featureLabels.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.featureLabels.forEach { tag ->
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
                            contentDescription = stringResource(R.string.repo_info_title),
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
                        text = if (completed) stringResource(R.string.repo_downloaded) else stringResource(R.string.download),
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
private fun CommunityInfoDialog(
    item: RepoItem,
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)?
) {
    val context = LocalContext.current
    val hashCopiedToast = stringResource(R.string.repo_hash_copied)
    val linkErrorToast = stringResource(R.string.repo_link_error)
    val scrollState = rememberScrollState()
    val reachedBottom by remember {
        derivedStateOf {
            scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue - 12
        }
    }
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
                        text = stringResource(R.string.repo_info_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.repo_close))
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
                        .heightIn(max = 420.dp)
                        .verticalScroll(scrollState)
                ) {
                    InfoRow(stringResource(R.string.repo_info_device), item.deviceModel ?: stringResource(R.string.repo_info_empty))
                    InfoRow(stringResource(R.string.repo_info_manager), item.manager ?: stringResource(R.string.repo_info_none))
                    InfoRow(stringResource(R.string.repo_info_manager_version), item.managerVer ?: "—")
                    InfoRow(stringResource(R.string.kernel_version), item.kernelVersion ?: "—")
                    InfoRow(stringResource(R.string.repo_info_features), item.featureLabels.joinToString(" / ").ifEmpty { stringResource(R.string.repo_info_empty) })
                    InfoRow(stringResource(R.string.repo_info_size), item.sizeHuman ?: "—")
                    InfoRow(stringResource(R.string.repo_info_upload_time), RepoTime.full(item.publishedAt))
                    if (!item.notes.isNullOrBlank()) {
                        InfoRow(stringResource(R.string.repo_info_notes), item.notes)
                    }
                    InfoRow(
                        label = stringResource(R.string.repo_info_author),
                        value = "@" + (item.authorLogin ?: stringResource(R.string.repo_unknown)),
                        onClick = item.authorUrl?.let { url -> { openUrl(context, url, linkErrorToast) } }
                    )
                    InfoRow("SHA-256", item.sha256 ?: "—", monospace = true)
                }
                if (onDownload != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (reachedBottom) {
                            stringResource(R.string.repo_gate_download_done)
                        } else {
                            stringResource(R.string.repo_gate_download_hint)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reachedBottom) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DialogButton(buttonText = stringResource(R.string.repo_copy_hash), onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as? ClipboardManager
                        val hash = item.sha256
                        if (clipboard != null && !hash.isNullOrBlank()) {
                            clipboard.setPrimaryClip(ClipData.newPlainText("SHA-256", hash))
                            Toast.makeText(context, hashCopiedToast, Toast.LENGTH_SHORT).show()
                        }
                    })
                    item.githubUrl?.let { url ->
                        DialogButton(buttonText = stringResource(R.string.repo_repo_page), onClick = { openUrl(context, url, linkErrorToast) })
                    }
                    Spacer(Modifier.weight(1f))
                    DialogButton(buttonText = stringResource(R.string.repo_close), onClick = onDismiss)
                    if (onDownload != null) {
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = onDownload,
                            enabled = reachedBottom,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.download), fontWeight = FontWeight.SemiBold)
                        }
                    }
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

private fun openUrl(context: Context, url: String, errorMessage: String) {
    if (url.isBlank()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}
