@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.navigation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.eric.contentfeed.ForegroundCoordinator
import com.eric.contentfeed.ForegroundEffect
import com.eric.contentfeed.R
import com.eric.contentfeed.core.connectivity.ConnectivityStatus
import com.eric.contentfeed.designsystem.component.StatusStrip
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.feed.presentation.contract.ConnectivityContract
import com.eric.contentfeed.feed.presentation.contract.DetailContract
import com.eric.contentfeed.feed.presentation.contract.FeedContract
import com.eric.contentfeed.feed.presentation.contract.SavedContract
import com.eric.contentfeed.feed.presentation.model.DetailTarget
import com.eric.contentfeed.feed.presentation.ui.DetailRoute
import com.eric.contentfeed.feed.presentation.ui.FeedRoute
import com.eric.contentfeed.feed.presentation.ui.SavedRoute
import com.eric.contentfeed.feed.presentation.viewmodel.ConnectivityViewModel
import com.eric.contentfeed.feed.presentation.viewmodel.SavedViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private enum class RootTab {
    Reading,
    Saved,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ContentFeedApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val readingLabel = stringResource(R.string.nav_reading)
    val savedLabel = stringResource(R.string.nav_saved)
    val backOnlineMessage = stringResource(R.string.snackbar_back_online)
    val isExpandedWidth =
        (context as? Activity)?.let { activity ->
            calculateWindowSizeClass(activity).widthSizeClass == WindowWidthSizeClass.Expanded
        } == true
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val readingBackStack = rememberNavBackStack(ContentFeedNavKey.Reading)
    val savedBackStack = rememberNavBackStack(ContentFeedNavKey.Saved)
    var selectedTab by rememberSaveable { mutableStateOf(RootTab.Reading) }
    val activeBackStack =
        when (selectedTab) {
            RootTab.Reading -> readingBackStack
            RootTab.Saved -> savedBackStack
        }
    val connectivityViewModel: ConnectivityViewModel = koinViewModel()
    val connectivityState by connectivityViewModel.state.collectAsStateWithLifecycle()
    val foregroundCoordinator: ForegroundCoordinator = koinInject()
    val savedViewModel: SavedViewModel = koinViewModel()

    LaunchedEffect(connectivityViewModel) {
        connectivityViewModel.effects.collectLatest { effect ->
            when (effect) {
                ConnectivityContract.Effect.BackOnline ->
                    snackbarHostState.showSnackbar(backOnlineMessage)
            }
        }
    }

    LaunchedEffect(foregroundCoordinator) {
        foregroundCoordinator.effects.collect { effect ->
            snackbarHostState.showSnackbar(effect.message(context))
        }
    }

    LaunchedEffect(savedViewModel) {
        savedViewModel.effects.collect { effect ->
            when (effect) {
                is SavedContract.Effect.NavigateToArticle ->
                    savedBackStack.add(ContentFeedNavKey.ArticleDetail(effect.articleId))
                is SavedContract.Effect.ShowUndo -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.snackbar_removed_from_saved),
                            actionLabel = context.getString(R.string.action_undo),
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        savedViewModel.onEvent(
                            SavedContract.Event.UndoRemoval(
                                effect.articleId,
                            ),
                        )
                    } else {
                        savedViewModel.onEvent(
                            SavedContract.Event.UndoWindowElapsed(
                                effect.articleId,
                            ),
                        )
                    }
                }
                SavedContract.Effect.UndoUnavailable ->
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.snackbar_already_removed),
                    )
            }
        }
    }

    val appEntryProvider =
        entryProvider<NavKey> {
            entry<ContentFeedNavKey.Reading> {
                FeedRoute(
                    onEffect = { effect ->
                        when (effect) {
                            is FeedContract.Effect.NavigateToArticle ->
                                readingBackStack.add(
                                    ContentFeedNavKey.ArticleDetail(effect.articleId),
                                )
                            is FeedContract.Effect.NavigateToServiceCard ->
                                readingBackStack.add(
                                    ContentFeedNavKey.ServiceCardDetail(
                                        poolIndex = effect.poolIndex,
                                        assignmentSequence = effect.assignmentSequence,
                                    ),
                                )
                            is FeedContract.Effect.OpenExternalUrl ->
                                context.openExternalUrl(effect.url) { message ->
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                }
                            FeedContract.Effect.ExternalLinkUnavailable ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(
                                            R.string.snackbar_external_link_requires_connection,
                                        ),
                                    )
                                }
                            is FeedContract.Effect.SourceRefreshFailed -> Unit
                        }
                    },
                )
            }
            entry<ContentFeedNavKey.Saved> {
                SavedRoute(viewModel = savedViewModel)
            }
            entry<ContentFeedNavKey.ArticleDetail> { key ->
                DetailRoute(
                    target = DetailTarget.Article(key.articleId),
                    onEffect = { effect ->
                        handleDetailEffect(effect, context) { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    },
                )
            }
            entry<ContentFeedNavKey.ServiceCardDetail> { key ->
                DetailRoute(
                    target =
                        DetailTarget.ServiceCard(
                            poolIndex = key.poolIndex,
                            assignmentSequence = key.assignmentSequence,
                        ),
                    onEffect = { effect ->
                        handleDetailEffect(effect, context) { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    },
                )
            }
        }
    val readingEntries =
        rememberDecoratedNavEntries(
            backStack = readingBackStack,
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                    rememberViewModelStoreNavEntryDecorator<NavKey>(),
                ),
            entryProvider = appEntryProvider,
        )
    val savedEntries =
        rememberDecoratedNavEntries(
            backStack = savedBackStack,
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                    rememberViewModelStoreNavEntryDecorator<NavKey>(),
                ),
            entryProvider = appEntryProvider,
        )
    val isDetailScreen =
        when (activeBackStack.lastOrNull()) {
            is ContentFeedNavKey.ArticleDetail,
            is ContentFeedNavKey.ServiceCardDetail,
            -> true
            else -> false
        }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val isSuccess = data.visuals.message == backOnlineMessage
                Snackbar(
                    snackbarData = data,
                    containerColor =
                        if (isSuccess) {
                            ContentFeedTheme.extendedColors.success
                        } else {
                            MaterialTheme.colorScheme.inverseSurface
                        },
                    contentColor =
                        if (isSuccess) {
                            ContentFeedTheme.extendedColors.onSuccess
                        } else {
                            MaterialTheme.colorScheme.inverseOnSurface
                        },
                )
            }
        },
        topBar = {
            if (isDetailScreen) {
                TopAppBar(
                    title = { Text(stringResource(activeBackStack.detailTitleRes())) },
                    navigationIcon = {
                        IconButton(onClick = { activeBackStack.removeLastOrNull() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            when (selectedTab) {
                                RootTab.Reading -> readingLabel
                                RootTab.Saved -> savedLabel
                            },
                        )
                    },
                )
            }
        },
        bottomBar = {
            if (!isDetailScreen && !isExpandedWidth) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == RootTab.Reading,
                        onClick = {
                            if (selectedTab == RootTab.Reading) {
                                while (readingBackStack.size > 1) readingBackStack.removeLastOrNull()
                            } else {
                                selectedTab = RootTab.Reading
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.AutoStories,
                                contentDescription = readingLabel,
                            )
                        },
                        label = { Text(readingLabel) },
                    )
                    NavigationBarItem(
                        selected = selectedTab == RootTab.Saved,
                        onClick = {
                            if (selectedTab == RootTab.Saved) {
                                while (savedBackStack.size > 1) savedBackStack.removeLastOrNull()
                            } else {
                                selectedTab = RootTab.Saved
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Bookmarks,
                                contentDescription = savedLabel,
                            )
                        },
                        label = { Text(savedLabel) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            if (connectivityState.status == ConnectivityStatus.Offline) {
                StatusStrip(
                    message = stringResource(R.string.status_offline_cached),
                )
            }
            Row(modifier = Modifier.weight(1f)) {
                if (isExpandedWidth && !isDetailScreen) {
                    NavigationRail {
                        NavigationRailItem(
                            selected = selectedTab == RootTab.Reading,
                            onClick = {
                                if (selectedTab == RootTab.Reading) {
                                    while (readingBackStack.size > 1) readingBackStack.removeLastOrNull()
                                } else {
                                    selectedTab = RootTab.Reading
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.AutoStories,
                                    contentDescription = readingLabel,
                                )
                            },
                            label = { Text(readingLabel) },
                        )
                        NavigationRailItem(
                            selected = selectedTab == RootTab.Saved,
                            onClick = {
                                if (selectedTab == RootTab.Saved) {
                                    while (savedBackStack.size > 1) savedBackStack.removeLastOrNull()
                                } else {
                                    selectedTab = RootTab.Saved
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Bookmarks,
                                    contentDescription = savedLabel,
                                )
                            },
                            label = { Text(savedLabel) },
                        )
                    }
                }
                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        if (targetState == RootTab.Saved) {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(280),
                            ) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(280),
                                )
                        } else {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(280),
                            ) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(280),
                                )
                        }
                    },
                    label = "root-tab-content-transition",
                ) { tab ->
                    val tabBackStack =
                        when (tab) {
                            RootTab.Reading -> readingBackStack
                            RootTab.Saved -> savedBackStack
                        }
                    val tabEntries =
                        when (tab) {
                            RootTab.Reading -> readingEntries
                            RootTab.Saved -> savedEntries
                        }
                    NavDisplay(
                        entries = tabEntries,
                        modifier = Modifier.fillMaxSize(),
                        onBack = {
                            if (tabBackStack.size > 1) {
                                tabBackStack.removeLastOrNull()
                            } else if (tab == RootTab.Saved) {
                                selectedTab = RootTab.Reading
                            }
                        },
                        transitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(280),
                            ) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(280),
                                )
                        },
                        popTransitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(280),
                            ) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(280),
                                )
                        },
                        predictivePopTransitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(280),
                            ) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(280),
                                )
                        },
                    )
                }
            }
        }
    }
}

private fun List<NavKey>.detailTitleRes(): Int =
    when (lastOrNull()) {
        is ContentFeedNavKey.ArticleDetail -> R.string.screen_title_article
        is ContentFeedNavKey.ServiceCardDetail -> R.string.screen_title_service
        else -> R.string.app_name
    }

private fun ForegroundEffect.message(context: Context): String =
    when (this) {
        is ForegroundEffect.SourceRefreshFailed ->
            context.getString(
                when (source) {
                    ForegroundEffect.Source.Articles -> R.string.snackbar_articles_refresh_failed
                    ForegroundEffect.Source.Weather -> R.string.snackbar_weather_refresh_failed
                },
            )
    }

private fun handleDetailEffect(
    effect: DetailContract.Effect,
    context: Context,
    onMessage: (String) -> Unit,
) {
    when (effect) {
        is DetailContract.Effect.OpenExternalUrl -> context.openExternalUrl(effect.url, onMessage)
        DetailContract.Effect.ExternalLinkUnavailable ->
            onMessage(context.getString(R.string.snackbar_external_link_requires_connection))
    }
}

private fun Context.openExternalUrl(
    url: String,
    onUnavailable: (String) -> Unit,
) {
    try {
        CustomTabsIntent.Builder().build().launchUrl(this, url.toUri())
    } catch (_: ActivityNotFoundException) {
        onUnavailable(getString(R.string.snackbar_no_browser_available))
    }
}
