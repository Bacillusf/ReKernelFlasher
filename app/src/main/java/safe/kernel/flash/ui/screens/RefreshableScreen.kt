package safe.kernel.flash.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.serialization.ExperimentalSerializationApi
import safe.kernel.flash.R
import safe.kernel.flash.ui.screens.main.MainViewModel

@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalSerializationApi
@Composable
fun RefreshableScreen(
    viewModel: MainViewModel,
    navController: NavController,
    swipeEnabled: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val statusBar = WindowInsets.statusBars.only(WindowInsetsSides.Top).asPaddingValues()
    val navigationBars = WindowInsets.navigationBars.asPaddingValues()
    val context = LocalContext.current
    val state = rememberPullRefreshState(viewModel.isRefreshing, onRefresh = {
        viewModel.refresh(context)
    })
    val scrollState = rememberScrollState()
    val collapsedProgress by remember {
        derivedStateOf { (scrollState.value / 96f).coerceIn(0f, 1f) }
    }
    val collapsedAlpha by animateFloatAsState(
        targetValue = collapsedProgress,
        label = "collapsedTitleAlpha"
    )
    val expandedAlpha by animateFloatAsState(
        targetValue = 1f - collapsedProgress,
        label = "expandedTitleAlpha"
    )
    val hasBack = navController.previousBackStackEntry != null
    val title = stringResource(R.string.app_name)

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .pullRefresh(state, swipeEnabled)
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = statusBar.calculateTopPadding() + 68.dp)
                .padding(bottom = 16.dp + navigationBars.calculateBottomPadding())
                .fillMaxSize()
                .verticalScroll(scrollState),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alpha(expandedAlpha)
                )
                Spacer(Modifier.height(16.dp))
                content()
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBar.calculateTopPadding())
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasBack) {
                AnimatedVisibility(!viewModel.isRefreshing, enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = if (hasBack) 0.dp else 12.dp)
                    .weight(1f)
                    .alpha(collapsedAlpha)
            )
            Row(content = actions)
        }

        PullRefreshIndicator(
            viewModel.isRefreshing,
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBar.calculateTopPadding()),
            backgroundColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            scale = true
        )
    }
}
