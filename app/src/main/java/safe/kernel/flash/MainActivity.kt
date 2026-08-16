package safe.kernel.flash

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import safe.kernel.flash.common.LanguageManager
import safe.kernel.flash.ui.components.AnimatedConfirmDialog
import safe.kernel.flash.ui.components.DialogButton
import safe.kernel.flash.ui.components.GlassNavigationBar
import safe.kernel.flash.ui.components.NavItem
import safe.kernel.flash.ui.screens.RefreshableScreen
import safe.kernel.flash.ui.screens.autobackup.AutoBackupContent
import safe.kernel.flash.ui.screens.autobackup.AutoBackupViewModel
import safe.kernel.flash.ui.screens.backups.BackupsContent
import safe.kernel.flash.ui.screens.backups.FullBackupContent
import safe.kernel.flash.ui.screens.backups.SlotBackupsContent
import safe.kernel.flash.ui.screens.error.ErrorScreen
import safe.kernel.flash.ui.screens.history.HistoryContent
import safe.kernel.flash.ui.screens.history.HistoryViewModel
import safe.kernel.flash.ui.screens.main.FlashHomeContent
import safe.kernel.flash.ui.screens.main.MainContent
import safe.kernel.flash.ui.screens.main.MainViewModel
import safe.kernel.flash.ui.screens.main.SettingsContent
import safe.kernel.flash.ui.screens.main.AutoBackupSettingsContent
import safe.kernel.flash.ui.screens.main.LanguageSettingsContent
import safe.kernel.flash.ui.screens.main.LogSettingsContent
import safe.kernel.flash.ui.screens.main.AdvancedSettingsContent
import safe.kernel.flash.ui.screens.repo.RepoContent
import safe.kernel.flash.ui.screens.reboot.RebootContent
import safe.kernel.flash.ui.screens.reboot.RebootViewModel
import safe.kernel.flash.ui.screens.slot.SlotContent
import safe.kernel.flash.ui.screens.slot.SlotFlashContent
import safe.kernel.flash.ui.screens.updates.UpdatesAddContent
import safe.kernel.flash.ui.screens.updates.UpdatesChangelogContent
import safe.kernel.flash.ui.screens.updates.UpdatesContent
import safe.kernel.flash.ui.screens.updates.UpdatesViewContent
import safe.kernel.flash.ui.screens.toolbox.PayloadDumperContent
import safe.kernel.flash.ui.screens.toolbox.PayloadDumperExtractContent
import safe.kernel.flash.ui.screens.toolbox.ToolboxContent
import safe.kernel.flash.ui.screens.toolbox.UnpackRecordsContent
import safe.kernel.flash.ui.screens.toolbox.UnpackHubContent
import safe.kernel.flash.ui.screens.toolbox.DiagPortContent
import safe.kernel.flash.ui.screens.toolbox.RkpFixContent
import safe.kernel.flash.ui.theme.KernelFlasherTheme
import safe.kernel.flash.ui.theme.LiquidGlassSupport
import safe.kernel.flash.ui.theme.optionalLayerBackdrop
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File
import kotlin.math.abs
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import kotlin.system.exitProcess

object SharedViewModels {
    @OptIn(ExperimentalSerializationApi::class)
    lateinit var mainViewModel: MainViewModel
}

private class RkfMainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return
        navJob?.cancel()
        selectedPage = targetIndex
        isNavigating = true

        val distance = abs(targetIndex - pagerState.currentPage).coerceAtLeast(2)
        val duration = 100 * distance + 100
        val layoutInfo = pagerState.layoutInfo
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        val currentDistanceInPages = targetIndex - pagerState.currentPage - pagerState.currentPageOffsetFraction
        val scrollPixels = currentDistanceInPages * pageSize

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.animateScrollBy(
                    value = scrollPixels,
                    animationSpec = tween(easing = EaseInOut, durationMillis = duration)
                )
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
private fun rememberRkfMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): RkfMainPagerState = remember(pagerState, coroutineScope) {
    RkfMainPagerState(pagerState, coroutineScope)
}

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalSerializationApi
@ExperimentalUnitApi
class MainActivity : ComponentActivity() {
    companion object {
        const val TAG: String = "MainActivity"
        private const val REQUEST_CODE_NOTIFICATIONS = 4101
        init {
            Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER))
        }
    }

    private var rootServiceConnected: Boolean = false
    private var backendInitializationStarted: Boolean = false
    private val startupProgress = mutableFloatStateOf(0.08f)
    private val startupStatusText = mutableStateOf("正在检测依赖")
    private val startupCanRetry = mutableStateOf(false)
    private var backendInitializationFailed: String? = null
    private var openMainWhenBackendReady: Boolean = false
    private var readyFileSystemManager: FileSystemManager? = null
    private var viewModel: MainViewModel? = null
    private lateinit var mainListener: MainListener
    var isAwaitingResult = false
    private var startupPermissionsRequested: Boolean = false

    inner class AidlConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            if (!rootServiceConnected) {
                val ipc: IFilesystemService = IFilesystemService.Stub.asInterface(service)
                val binder: IBinder = ipc.fileSystemService
                onAidlConnected(FileSystemManager.getRemote(binder))
                rootServiceConnected = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            setContent {
                KernelFlasherTheme {
                    ErrorScreen(stringResource(R.string.root_service_disconnected))
                }
            }
        }
    }

    private fun copyAsset(filename: String): File {
        val dest = File(filesDir, filename)
        if (dest.exists() && dest.length() > 0L) return dest
        assets.open(filename).use { inputStream ->
            dest.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        dest.setExecutable(true, false)
        return dest
    }

    private fun copyNativeBinary(filename: String): File {
        val binary = File(applicationInfo.nativeLibraryDir, "lib$filename.so")
        val dest = File(filesDir, filename)
        if (dest.exists() && dest.length() == binary.length()) return dest
        binary.inputStream().use { inputStream ->
            dest.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        dest.setExecutable(true, false)
        return dest
    }

    private fun chmodExecutables(files: List<File>) {
        if (files.isEmpty()) return
        val paths = files.joinToString(" ") { it.absolutePath }
        Shell.cmd("chmod 755 $paths").exec()
    }

    private fun detectRootManager(): String? {
        val out = Shell.cmd(
            "if [ -f /data/adb/ksud ]; then echo KernelSU; " +
                "elif [ -f /data/adb/apd ]; then echo APatch; " +
                "elif [ -f /data/adb/magisk/magisk ]; then echo Magisk; " +
                "else echo none; fi"
        ).exec().out.firstOrNull()
        return out?.takeIf { it != "none" }
    }

    override fun attachBaseContext(newBase: Context) {
        LanguageManager.init(newBase.applicationContext)
        super.attachBaseContext(LanguageManager.applyContextLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LanguageManager.init(this)
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val splashScreen = installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(R.color.window_background)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
        }

        showLauncherStartup()
        runLauncherStartupChecks()
    }

    private fun updateStartupProgress(progress: Float, status: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            startupProgress.floatValue = progress.coerceIn(0f, 1f)
            startupStatusText.value = status
        }
    }

    private fun enterMainAfterStartup(fileSystemManager: FileSystemManager) {
        lifecycleScope.launch(Dispatchers.Main) {
            startupProgress.floatValue = 1f
            startupStatusText.value = "检查通过，正在进入..."
            delay(360)
            showMainContent(fileSystemManager)
        }
    }

    private fun startBackendAfterRootGranted(openMainWhenReady: Boolean) {
        readyFileSystemManager?.let { readyManager ->
            backendInitializationFailed = null
            startupProgress.floatValue = 1f
            startupStatusText.value = "初始化完成"
            if (openMainWhenReady) enterMainAfterStartup(readyManager)
            return
        }

        openMainWhenBackendReady = openMainWhenBackendReady || openMainWhenReady
        if (backendInitializationStarted) return
        backendInitializationStarted = true
        backendInitializationFailed = null
        startupProgress.floatValue = 0.48f
        startupStatusText.value = "检查必要权限"
        rootServiceConnected = false
        val intent = Intent(this@MainActivity, FilesystemService::class.java)
        RootService.bind(intent, AidlConnection())
    }

    private fun showLauncherStartup() {
        setContent {
            KernelFlasherTheme {
                val loadingProgress by startupProgress
                val loadingStatus by startupStatusText
                val canRetry by startupCanRetry
                val animatedLoadingProgress by animateFloatAsState(
                    targetValue = loadingProgress,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    label = "launcherStartupProgress"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 30.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Root · 后端 · 刷写工具",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val filledSegments = (animatedLoadingProgress.coerceIn(0f, 1f) * 4f).toInt().coerceIn(0, 4)
                            repeat(4) { index ->
                                val active = index < filledSegments || animatedLoadingProgress >= 1f
                                Box(
                                    modifier = Modifier
                                        .size(if (active) 40.dp else 28.dp, 7.dp)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(20.dp)
                                        )
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = loadingStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        if (canRetry) {
                            Spacer(Modifier.height(18.dp))
                            Button(onClick = { runLauncherStartupChecks() }) {
                                Text("重试")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestStartupPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_NOTIFICATIONS)
        } else {
            launchAllFilesAccessSettingsIfNeeded()
        }
    }

    private fun launchAllFilesAccessSettingsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            runCatching { startActivity(intent) }
        }
    }

    private fun runLauncherStartupChecks() {
        startupCanRetry.value = false
        startupProgress.floatValue = 0.08f
        startupStatusText.value = "正在检查 Root 权限和后端依赖..."
        openMainWhenBackendReady = false
        lifecycleScope.launch {
            try {
                updateStartupProgress(0.14f, "正在请求 Root 授权...")
                val rootGranted = withContext(Dispatchers.IO) {
                    Shell.getShell()
                    var granted = false
                    for (attempt in 0 until 8) {
                        granted = Shell.isAppGrantedRoot() == true
                        if (granted) break
                        Thread.sleep(180)
                    }
                    granted
                }
                if (!rootGranted) {
                    startupStatusText.value = "Root 权限不可用，请授权后重试。"
                    startupProgress.floatValue = 0f
                    startupCanRetry.value = true
                    return@launch
                }

                updateStartupProgress(0.26f, "正在识别 Root 管理器...")
                val rootManager = withContext(Dispatchers.IO) { detectRootManager() }
                if (rootManager == null) {
                    startupStatusText.value = "未识别到 KernelSU、APatch 或 Magisk 管理器。"
                    startupProgress.floatValue = 0f
                    startupCanRetry.value = true
                    return@launch
                }

                updateStartupProgress(0.34f, "正在检查工作目录...")
                val workDirWritable = withContext(Dispatchers.IO) {
                    filesDir.exists() && filesDir.canWrite()
                }
                if (!workDirWritable) {
                    startupStatusText.value = "本地工作目录不可写。"
                    startupProgress.floatValue = 0f
                    startupCanRetry.value = true
                    return@launch
                }

                startBackendAfterRootGranted(openMainWhenReady = true)
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                startupStatusText.value = e.message ?: "检查失败，请重试。"
                startupProgress.floatValue = 0f
                startupCanRetry.value = true
            }
        }
    }

    private fun showStartupError(message: String) {
        setContent {
            KernelFlasherTheme {
                ErrorScreen(message)
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun handleZipIntent(intent: Intent?) {
        val action = intent?.action ?: return
        val uri = when (action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
            else -> null
        } ?: return

        if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEND) {
            if(uri.scheme == "content" && DocumentsContract.isDocumentUri(this, uri)) {
                val takeFlags =
                    intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                try {
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (se: SecurityException) {
                    Log.e(MainViewModel.Companion.TAG, se.message, se)
                }
            }

            viewModel?.pendingFlashUri = uri
            if(viewModel?.isAb == true)
                viewModel?.showSlotIntentDialog?.value = true
            else {
                viewModel?.slotSuffixForFlash?.value = null
                viewModel?.slotSuffixForFlash?.value = viewModel?.slotSuffix
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (Shell.isAppGrantedRoot() == true) {
            handleZipIntent(intent)
            if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEND) {
                intent.replaceExtras(Bundle()) // Clear any existing data
                setIntent(Intent())            // Replace with empty intent
            }
        }
    }

    fun onAidlConnected(fileSystemManager: FileSystemManager) {
        updateStartupProgress(0.52f, "正在连接文件系统服务")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                updateStartupProgress(0.60f, "正在准备后端工具")
                val executableFiles = mutableListOf<File>()
                updateStartupProgress(0.66f, "正在准备分区工具")
                executableFiles += copyNativeBinary("lptools_static") // v20220825
                executableFiles += copyNativeBinary("httools_static") // v3.2.0
                updateStartupProgress(0.74f, "正在准备刷写工具")
                executableFiles += copyNativeBinary("magiskboot") // v29.0
                executableFiles += copyNativeBinary("bootctl") // aosp_arm64-img-13613025 android14
                updateStartupProgress(0.82f, "正在准备 BusyBox")
                executableFiles += copyNativeBinary("busybox") // BusyBox v1.36.1.1
                updateStartupProgress(0.88f, "正在准备脚本和解包工具")
                executableFiles += copyAsset("mkbootfs")
                executableFiles += copyAsset("ksuinit")
                executableFiles += copyAsset("payload-dumper-go")
                executableFiles += copyAsset("flash_ak3.sh")
                executableFiles += copyAsset("flash_ak3_mkbootfs.sh")
                chmodExecutables(executableFiles.distinctBy { it.absolutePath })
                updateStartupProgress(0.96f, "后端服务和刷写工具已就绪")
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                withContext(Dispatchers.Main) {
                    backendInitializationStarted = false
                    backendInitializationFailed = e.message ?: getString(R.string.root_required)
                    if (openMainWhenBackendReady) {
                        showStartupError(e.message ?: getString(R.string.root_required))
                    }
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                readyFileSystemManager = fileSystemManager
                backendInitializationFailed = null
                startupProgress.floatValue = 1f
                startupStatusText.value = "初始化完成"
                if (openMainWhenBackendReady) {
                    enterMainAfterStartup(fileSystemManager)
                }
            }
        }
    }


    private fun showMainContent(fileSystemManager: FileSystemManager) {
        setContent {
            val navController = rememberNavController()
            viewModel = viewModel {
                val application = checkNotNull(get(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY))
                MainViewModel(application, fileSystemManager, navController)
            }
            val mainViewModel = viewModel!!
            SharedViewModels.mainViewModel = mainViewModel

            val slotSuffix by viewModel!!.slotSuffixForFlash

            handleZipIntent(intent)
            if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEND) {
                intent.replaceExtras(Bundle()) // Clear any existing data
                setIntent(Intent())            // Replace with empty intent
            }

            val context = LocalContext.current
            LaunchedEffect(Unit) {
                if (AppUpdater.hasActiveInternetConnection()) {
                    val info = AppUpdater.checkForUpdate(BuildConfig.VERSION_NAME)
                    if (info != null) {
                        viewModel!!.setUpdateInfo(info.version, info.body, info.downloadUrl)
                    }
                }

                val uri = viewModel?.pendingFlashUri

                if (uri != null) {
                    if (viewModel?.isAb == true && slotSuffix == null) {
                        viewModel?.pendingFlashUri = uri
                        viewModel?.showSlotIntentDialog?.value = true
                    } else {
                        // Already have slot or not AB - flash directly
                        if (viewModel?.isAb == true && slotSuffix == "_b")
                        {
                            viewModel?.slotB?.flashActionType = "flashAk3"
                            viewModel?.slotB?.flashActionURI = uri
                            viewModel?.slotB?.showConfirmDialog()
                        }
                        else
                        {
                            viewModel?.slotA?.flashActionType = "flashAk3"
                            viewModel?.slotA?.flashActionURI = uri
                            viewModel?.slotA?.showConfirmDialog()
                        }
                        navController.navigate("slot${slotSuffix}")
                        navController.navigate("slot${slotSuffix}/flash") {
                            popUpTo("slot${slotSuffix}")
                        }
                        viewModel?.pendingFlashUri = null
                        viewModel?.slotSuffixForFlash?.value = null
                    }
                }
            }

            var showExitDialog by remember { mutableStateOf(false) }

            KernelFlasherTheme {
                if (!mainViewModel.hasError) {
                    mainListener = MainListener {
                        mainViewModel.refresh(this)
                    }
                    val slotViewModelA = mainViewModel.slotA
                    val slotViewModelB = mainViewModel.slotB
                    val backupsViewModel = mainViewModel.backups
                    val updatesViewModel = mainViewModel.updates
                    val rebootViewModel = mainViewModel.reboot
                    val historyViewModel = remember { HistoryViewModel() }
                    val autoBackupViewModel = remember { AutoBackupViewModel() }

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val tabRoutes = listOf("main", "flash", "backups", "settings")
                    val initialTabIndex = tabRoutes.indexOf(currentRoute).coerceAtLeast(0)
                    val isTabRoute = currentRoute in tabRoutes
                    val pagerState = rememberPagerState(initialPage = initialTabIndex) { tabRoutes.size }
                    val mainPagerState = rememberRkfMainPagerState(pagerState)
                    val selectedTabRoute = if (isTabRoute) tabRoutes[mainPagerState.selectedPage] else currentRoute

                    val dpiScale = mainViewModel.dpiScale
                    val density = LocalDensity.current
                    val scaledDensity = Density(density.density * dpiScale, density.fontScale * dpiScale)

                    BackHandler(enabled = !mainViewModel.isRefreshing, onBack = {})
                    // New back handler for exit
                    BackHandler(enabled = true) {
                        showExitDialog = true
                    }
                    val slotContentA: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit = { backStackEntry ->
                        val slotSuffix = "_a"
                        val slotViewModel = slotViewModelA
                        if (slotViewModel.wasFlashSuccess.value != null && listOf("slot{slotSuffix}", "slot").any { navController.currentDestination!!.route.equals(it) }) {
                            slotViewModel.clearFlash(this@MainActivity)
                        }
                        RefreshableScreen(mainViewModel, navController, swipeEnabled = true) {
                            SlotContent(slotViewModel, slotSuffix, navController)
                        }

                    }
                    val slotContentB: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit = { backStackEntry ->
                        val slotSuffix = "_b"
                        val slotViewModel = slotViewModelB
                        if (slotViewModel!!.wasFlashSuccess.value != null && listOf("slot{slotSuffix}", "slot").any { navController.currentDestination!!.route.equals(it) }) {
                            slotViewModel.clearFlash(this@MainActivity)
                        }
                        RefreshableScreen(mainViewModel, navController, swipeEnabled = true) {
                            SlotContent(slotViewModel, slotSuffix, navController)
                        }

                    }
                    val slotContent: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit = { backStackEntry ->
                        val slotSuffix = ""
                        val slotViewModel = slotViewModelA
                        if (slotViewModel.wasFlashSuccess.value != null && listOf("slot{slotSuffix}", "slot").any { navController.currentDestination!!.route.equals(it) }) {
                            slotViewModel.clearFlash(this@MainActivity)
                        }
                        RefreshableScreen(mainViewModel, navController, swipeEnabled = true) {
                            SlotContent(slotViewModel, slotSuffix, navController)
                        }

                    }
                    val slotFlashContentA: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit = { backStackEntry ->
                        val slotSuffix = "_a"
                        val slotViewModel = slotViewModelA
                        RefreshableScreen(mainViewModel, navController) {
                            SlotFlashContent(slotViewModel, slotSuffix, navController)
                        }
                    }
                    val slotFlashContentB: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit = { backStackEntry ->
                        val slotSuffix = "_b"
                        val slotViewModel = slotViewModelB
                        RefreshableScreen(mainViewModel, navController) {
                            SlotFlashContent(slotViewModel!!, slotSuffix, navController)
                        }
                    }
                    val slotFlashContent: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit = { backStackEntry ->
                        val slotSuffix = ""
                        val slotViewModel = slotViewModelA
                        RefreshableScreen(mainViewModel, navController) {
                            SlotFlashContent(slotViewModel, slotSuffix, navController)
                        }
                    }
                    val slotBackupsContentA: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit = { backStackEntry ->
                        val slotSuffix = "_a"
                        val slotViewModel = slotViewModelA
                        if (backStackEntry.arguments?.getString("backupId") != null) {
                            backupsViewModel.currentBackup = backStackEntry.arguments?.getString("backupId")
                        } else {
                            backupsViewModel.clearCurrent()
                        }
                        RefreshableScreen(mainViewModel, navController) {
                            SlotBackupsContent(slotViewModel, backupsViewModel, slotSuffix, navController)
                        }
                    }
                    val slotBackupsContentB: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit = { backStackEntry ->
                        val slotSuffix = "_b"
                        val slotViewModel = slotViewModelB
                        if (backStackEntry.arguments?.getString("backupId") != null) {
                            backupsViewModel.currentBackup = backStackEntry.arguments?.getString("backupId")
                        } else {
                            backupsViewModel.clearCurrent()
                        }
                        RefreshableScreen(mainViewModel, navController) {
                            SlotBackupsContent(slotViewModel!!, backupsViewModel, slotSuffix, navController)
                        }
                    }
                    val slotBackupsContent: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit = { backStackEntry ->
                        val slotSuffix = ""
                        val slotViewModel = slotViewModelA
                        if (backStackEntry.arguments?.getString("backupId") != null) {
                            backupsViewModel.currentBackup = backStackEntry.arguments?.getString("backupId")
                        } else {
                            backupsViewModel.clearCurrent()
                        }
                        RefreshableScreen(mainViewModel, navController) {
                            SlotBackupsContent(slotViewModel, backupsViewModel, slotSuffix, navController)
                        }
                    }
                    val slotBackupFlashContentA: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit = { backStackEntry ->
                        val slotSuffix = "_a"
                        val slotViewModel = slotViewModelA
                        backupsViewModel.currentBackup = backStackEntry.arguments?.getString("backupId")
                        if (backupsViewModel.backups.containsKey(backupsViewModel.currentBackup)) {
                            RefreshableScreen(mainViewModel, navController) {
                                SlotFlashContent(slotViewModel, slotSuffix, navController)
                            }
                        }

                    }
                    val slotBackupFlashContentB: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit = { backStackEntry ->
                        val slotSuffix = "_b"
                        val slotViewModel = slotViewModelB
                        backupsViewModel.currentBackup = backStackEntry.arguments?.getString("backupId")
                        if (backupsViewModel.backups.containsKey(backupsViewModel.currentBackup)) {
                            RefreshableScreen(mainViewModel, navController) {
                                SlotFlashContent(slotViewModel!!, slotSuffix, navController)
                            }
                        }

                    }
                    val slotBackupFlashContent: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit = { backStackEntry ->
                        val slotSuffix = ""
                        val slotViewModel = slotViewModelA
                        backupsViewModel.currentBackup = backStackEntry.arguments?.getString("backupId")
                        if (backupsViewModel.backups.containsKey(backupsViewModel.currentBackup)) {
                            RefreshableScreen(mainViewModel, navController) {
                                SlotFlashContent(slotViewModel, slotSuffix, navController)
                            }
                        }

                    }
                    CompositionLocalProvider(LocalDensity provides scaledDensity) {
                        val startDest = "main"
                        val floatingNavBackground = MaterialTheme.colorScheme.background
                        val floatingNavBackdrop: LayerBackdrop? = if (LiquidGlassSupport.isSupported) {
                            rememberLayerBackdrop {
                                drawRect(floatingNavBackground)
                                drawContent()
                            }
                        } else null
                        @Composable
                        fun MainTabPager() {
                            val currentPage = mainPagerState.pagerState.currentPage
                            LaunchedEffect(currentPage) {
                                mainPagerState.syncPage()
                            }
                            HorizontalPager(
                                state = mainPagerState.pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .optionalLayerBackdrop(floatingNavBackdrop),
                                beyondViewportPageCount = 1,
                                userScrollEnabled = true,
                            ) { page ->
                                val isCurrentPage = page == mainPagerState.pagerState.settledPage
                                when (page) {
                                    0 -> RefreshableScreen(mainViewModel, navController, swipeEnabled = true, bottomContentPadding = 120.dp, actions = {
                                        val showRebootMenu = remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { showRebootMenu.value = true }) {
                                                Icon(Icons.Filled.PowerSettingsNew, contentDescription = "重启")
                                            }
                                            DropdownMenu(expanded = showRebootMenu.value, onDismissRequest = { showRebootMenu.value = false }) {
                                                DropdownMenuItem(text = { Text(stringResource(R.string.reboot)) }, onClick = { showRebootMenu.value = false; rebootViewModel.showConfirm("") })
                                                DropdownMenuItem(text = { Text(stringResource(R.string.reboot_recovery)) }, onClick = { showRebootMenu.value = false; rebootViewModel.showConfirm("recovery") })
                                                DropdownMenuItem(text = { Text(stringResource(R.string.reboot_bootloader)) }, onClick = { showRebootMenu.value = false; rebootViewModel.showConfirm("bootloader") })
                                                DropdownMenuItem(text = { Text(stringResource(R.string.reboot_download)) }, onClick = { showRebootMenu.value = false; rebootViewModel.showConfirm("download") })
                                                DropdownMenuItem(text = { Text(stringResource(R.string.reboot_edl)) }, onClick = { showRebootMenu.value = false; rebootViewModel.showConfirm("edl") })
                                            }
                                        }
                                    }) {
                                        MainContent(mainViewModel, navController)
                                    }
                                    1 -> RefreshableScreen(
                                        mainViewModel,
                                        navController,
                                        bottomContentPadding = 120.dp,
                                        actions = {
                                            IconButton(onClick = { navController.navigate("repo") }) {
                                                Icon(
                                                    Icons.Filled.Cloud,
                                                    contentDescription = "GKI/OKI 仓库",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    ) {
                                        FlashHomeContent(mainViewModel, navController)
                                    }
                                    2 -> {
                                        LaunchedEffect(isCurrentPage) {
                                            if (isCurrentPage) backupsViewModel.clearCurrent()
                                        }
                                        RefreshableScreen(mainViewModel, navController, bottomContentPadding = 120.dp) {
                                            BackupsContent(backupsViewModel, navController)
                                        }
                                    }
                                    3 -> RefreshableScreen(mainViewModel, navController, bottomContentPadding = 120.dp) {
                                        SettingsContent(mainViewModel, navController)
                                    }
                                }
                            }
                        }
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f)) {
                                NavHost(
                                    navController = navController,
                                    startDestination = startDest,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .optionalLayerBackdrop(floatingNavBackdrop)
                                ) {
                                    composable("main") {
                                        MainTabPager()
                                    }
                                    composable("flash") {
                                        MainTabPager()
                                    }
                                    composable("settings") {
                                        MainTabPager()
                                    }
                        if (mainViewModel.isAb) {
                            composable("slot_a", content = slotContentA)
                            composable("slot_a/flash", content = slotFlashContentA)
                            composable("slot_a/flash/ak3", content = slotFlashContentA)
                            composable("slot_a/flash/image", content = slotFlashContentA)
                            composable("slot_a/flash/image/flash", content = slotFlashContentA)
                            composable("slot_a/backup", content = slotFlashContentA)
                            composable("slot_a/backup/backup", content = slotFlashContentA)
                            composable("slot_a/backups", content = slotBackupsContentA)
                            composable("slot_a/backups/{backupId}", content = slotBackupsContentA)
                            composable("slot_a/backups/{backupId}/restore", content = slotBackupsContentA)
                            composable("slot_a/backups/{backupId}/restore/restore", content = slotBackupsContentA)
                            composable("slot_a/backups/{backupId}/flash/ak3", content = slotBackupFlashContentA)

                            composable("slot_b", content = slotContentB)
                            composable("slot_b/flash", content = slotFlashContentB)
                            composable("slot_b/flash/ak3", content = slotFlashContentB)
                            composable("slot_b/flash/image", content = slotFlashContentB)
                            composable("slot_b/flash/image/flash", content = slotFlashContentB)
                            composable("slot_b/backup", content = slotFlashContentB)
                            composable("slot_b/backup/backup", content = slotFlashContentB)
                            composable("slot_b/backups", content = slotBackupsContentB)
                            composable("slot_b/backups/{backupId}", content = slotBackupsContentB)
                            composable("slot_b/backups/{backupId}/restore", content = slotBackupsContentB)
                            composable("slot_b/backups/{backupId}/restore/restore", content = slotBackupsContentB)
                            composable("slot_b/backups/{backupId}/flash/ak3", content = slotBackupFlashContentB)
                        } else {
                            composable("slot", content = slotContent)
                            composable("slot/flash", content = slotFlashContent)
                            composable("slot/flash/ak3", content = slotFlashContent)
                            composable("slot/flash/image", content = slotFlashContent)
                            composable("slot/flash/image/flash", content = slotFlashContent)
                            composable("slot/backup", content = slotFlashContent)
                            composable("slot/backup/backup", content = slotFlashContent)
                            composable("slot/backups", content = slotBackupsContent)
                            composable("slot/backups/{backupId}", content = slotBackupsContent)
                            composable("slot/backups/{backupId}/restore", content = slotBackupsContent)
                            composable("slot/backups/{backupId}/restore/restore", content = slotBackupsContent)
                            composable("slot/backups/{backupId}/flash/ak3", content = slotBackupFlashContent)
                        }
                        composable("backups") {
                                        MainTabPager()
                                    }
                        composable("backups/{backupId}") { backStackEntry ->
                            backupsViewModel.currentBackup = backStackEntry.arguments?.getString("backupId")
                            if (backupsViewModel.backups.containsKey(backupsViewModel.currentBackup)) {
                                RefreshableScreen(mainViewModel, navController) {
                                    BackupsContent(backupsViewModel, navController)
                                }
                            }
                        }
                        composable("updates") {
                            updatesViewModel.clearCurrent()
                            RefreshableScreen(mainViewModel, navController) {
                                UpdatesContent(updatesViewModel, navController)
                            }
                        }
                        composable("updates/add") {
                            RefreshableScreen(mainViewModel, navController) {
                                UpdatesAddContent(updatesViewModel, navController)
                            }
                        }
                        composable("updates/view/{updateId}") { backStackEntry ->
                            val updateId = backStackEntry.arguments?.getString("updateId")!!.toInt()
                            val currentUpdate = updatesViewModel.updates.firstOrNull { it.id == updateId }
                            updatesViewModel.currentUpdate = currentUpdate
                            if (updatesViewModel.currentUpdate != null) {
                                // TODO: enable swipe refresh
                                RefreshableScreen(mainViewModel, navController) {
                                    UpdatesViewContent(updatesViewModel, navController)
                                }
                            }
                        }
                        composable("updates/view/{updateId}/changelog") { backStackEntry ->
                            val updateId = backStackEntry.arguments?.getString("updateId")!!.toInt()
                            val currentUpdate = updatesViewModel.updates.firstOrNull { it.id == updateId }
                            updatesViewModel.currentUpdate = currentUpdate
                            if (updatesViewModel.currentUpdate != null) {
                                RefreshableScreen(mainViewModel, navController) {
                                    UpdatesChangelogContent(updatesViewModel, navController)
                                }
                            }
                        }
                        composable("reboot") {
                            RefreshableScreen(mainViewModel, navController) {
                                RebootContent(rebootViewModel, navController)
                            }
                        }
                        composable("settings/autobackup") {
                            RefreshableScreen(mainViewModel, navController) {
                                AutoBackupSettingsContent(mainViewModel, navController)
                            }
                        }
                        composable("settings/language") {
                            RefreshableScreen(mainViewModel, navController) {
                                LanguageSettingsContent(mainViewModel, navController)
                            }
                        }
                        composable("settings/logs") {
                            RefreshableScreen(mainViewModel, navController) {
                                LogSettingsContent(mainViewModel, navController)
                            }
                        }
                        composable("settings/advanced") {
                            RefreshableScreen(mainViewModel, navController) {
                                AdvancedSettingsContent(mainViewModel, navController)
                            }
                        }
                        composable("toolbox") {
                            RefreshableScreen(mainViewModel, navController) {
                                ToolboxContent(navController)
                            }
                        }
                        composable("toolbox/unpack") {
                            RefreshableScreen(mainViewModel, navController) {
                                UnpackHubContent(navController)
                            }
                        }
                        composable("toolbox/payload?uri={uri}") { backStackEntry ->
                            val uri = backStackEntry.arguments?.getString("uri") ?: ""
                            RefreshableScreen(mainViewModel, navController) {
                                PayloadDumperContent(navController, uri)
                            }
                        }
                        composable("toolbox/payload/extract") {
                            RefreshableScreen(mainViewModel, navController) {
                                PayloadDumperExtractContent(navController)
                            }
                        }
                        composable("toolbox/full_backup") {
                            backupsViewModel.clearCurrent()
                            RefreshableScreen(mainViewModel, navController, swipeEnabled = false) {
                                FullBackupContent(backupsViewModel, navController)
                            }
                        }
                        composable("toolbox/unpack_records") {
                            RefreshableScreen(mainViewModel, navController) {
                                UnpackRecordsContent(navController)
                            }
                        }
                        composable("toolbox/rkp_fix") {
                            RefreshableScreen(mainViewModel, navController) {
                                RkpFixContent(navController)
                            }
                        }
                        composable("toolbox/diag_port") {
                            RefreshableScreen(mainViewModel, navController) {
                                DiagPortContent(navController)
                            }
                        }
                        composable("repo") {
                            RepoContent(navController)
                        }
                        composable("history") {
                            RefreshableScreen(mainViewModel, navController) {
                                HistoryContent(historyViewModel, navController)
                            }
                        }
                        composable("autobackup") {
                            RefreshableScreen(mainViewModel, navController) {
                                AutoBackupContent(autoBackupViewModel, navController)
                            }
                        }
                        composable("error/{error}") { backStackEntry ->
                            val error = backStackEntry.arguments?.getString("error")
                            ErrorScreen(error!!)
                        }
                    }
                                if (isTabRoute) {
                                    GlassNavigationBar(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        items = listOf(
                                            NavItem("main", stringResource(R.string.tab_home), Icons.Filled.Home),
                                            NavItem("flash", stringResource(R.string.tab_flash), Icons.Filled.Build),
                                            NavItem("backups", stringResource(R.string.backups), Icons.Filled.List),
                                            NavItem("settings", stringResource(R.string.tab_settings), Icons.Filled.Settings)
                                        ),
                                        currentRoute = selectedTabRoute,
                                        backdrop = floatingNavBackdrop,
                                        onItemClick = { item ->
                                            val targetIndex = tabRoutes.indexOf(item.route)
                                            if (targetIndex >= 0) {
                                                mainPagerState.animateToPage(targetIndex)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    AnimatedConfirmDialog(
                        visible = rebootViewModel.showConfirmDialog,
                        title = "CAUTION!",
                        message = rebootViewModel.confirmMessage,
                        confirmText = "重启",
                        cancelText = stringResource(R.string.cancel),
                        onConfirm = { rebootViewModel.executeReboot() },
                        onDismiss = { rebootViewModel.hideConfirm() }
                    )

                    LaunchedEffect(mainViewModel.isDownloading) {
                        if (mainViewModel.isDownloading && mainViewModel.updateDownloadUrl.isNotEmpty()) {
                            val progress = mutableStateOf(0f)
                            AppUpdater.downloadWithProgress(
                                context,
                                mainViewModel.updateDownloadUrl,
                                mainViewModel.updateVersion,
                                progress
                            ) { file ->
                                mainViewModel.finishDownload()
                                AppUpdater.installApk(context, file)
                            }
                            while (mainViewModel.isDownloading) {
                                mainViewModel.updateDownloadProgress(progress.value)
                                kotlinx.coroutines.delay(100)
                            }
                        }
                    }
                } else {
                    ErrorScreen(mainViewModel.error)
                }

                if (showExitDialog) {
                    AnimatedConfirmDialog(
                        visible = showExitDialog,
                        title = "退出应用",
                        message = "确定要退出吗？",
                        confirmText = "退出",
                        cancelText = stringResource(R.string.cancel),
                        onConfirm = {
                            (context as? Activity)?.let {
                                it.finishAffinity()
                                exitProcess(0)
                            }
                        },
                        onDismiss = { showExitDialog = false }
                    )
                }

                if (viewModel?.showSlotIntentDialog?.value == true) {
                    AnimatedConfirmDialog(
                        visible = viewModel?.showSlotIntentDialog?.value == true,
                        title = "选择刷写槽位",
                        message = "选择要刷写的槽位",
                        confirmText = "非活跃槽位",
                        cancelText = "活跃槽位",
                        onConfirm = {
                            viewModel?.slotSuffixForFlash?.value = null
                            viewModel?.slotSuffixForFlash?.value = if (viewModel?.slotSuffix == "_a") "_b" else "_a"
                            viewModel?.showSlotIntentDialog?.value = false
                        },
                        onDismiss = {
                            viewModel?.slotSuffixForFlash?.value = null
                            viewModel?.slotSuffixForFlash?.value = viewModel?.slotSuffix
                            viewModel?.showSlotIntentDialog?.value = false
                        }
                    )
                }

                LaunchedEffect(slotSuffix) {
                    val uri = viewModel!!.pendingFlashUri

                    if (uri != null && slotSuffix != null) {
                        if (viewModel?.isAb == true && slotSuffix == "_b")
                        {
                            viewModel?.slotB?.flashActionType = "flashAk3"
                            viewModel?.slotB?.flashActionURI = uri
                            viewModel?.slotB?.showConfirmDialog()
                        }
                        else
                        {
                            viewModel?.slotA?.flashActionType = "flashAk3"
                            viewModel?.slotA?.flashActionURI = uri
                            viewModel?.slotA?.showConfirmDialog()
                        }
                        navController.navigate("slot${slotSuffix}")
                        navController.navigate("slot${slotSuffix}/flash") {
                            popUpTo("slot${slotSuffix}")
                        }
                        viewModel!!.pendingFlashUri = null
                        viewModel!!.slotSuffixForFlash.value = null
                    }
                }
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        if (!startupPermissionsRequested) {
            startupPermissionsRequested = true
            requestStartupPermissions()
        }
        if (this::mainListener.isInitialized) {
            if (!isAwaitingResult) {
                mainListener.resume()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATIONS) {
            launchAllFilesAccessSettingsIfNeeded()
        }
    }
}
