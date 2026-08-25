@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package studio.gooduse.kitchenprep

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.gooduse.kitchenprep.monetization.BillingController
import studio.gooduse.kitchenprep.monetization.ConsentController
import studio.gooduse.kitchenprep.monetization.NativeTestBanner

typealias Translate = (String, String) -> String

@Composable
fun KitchenPrepClosedTestApp(
    activity: MainActivity,
    billingController: BillingController,
    consentController: ConsentController,
    viewModel: KitchenViewModel = viewModel(),
) {
    val billing by billingController.state.collectAsStateWithLifecycle()
    val consent by consentController.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val boards by viewModel.boards.collectAsStateWithLifecycle()
    val selectedBoard by viewModel.selectedBoard.collectAsStateWithLifecycle()
    val selectedTasks by viewModel.selectedTasks.collectAsStateWithLifecycle()
    val liveLane by viewModel.liveLane.collectAsStateWithLifecycle()
    val createDraft by viewModel.createDraft.collectAsStateWithLifecycle()
    val createStep by viewModel.createStep.collectAsStateWithLifecycle()
    val pasteText by viewModel.pasteText.collectAsStateWithLifecycle()
    val showSafety by viewModel.safetyConfirmation.collectAsStateWithLifecycle()
    val undoTaskId by viewModel.lastUndoTaskId.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val strings = remember { KitchenStrings.load(context) }
    val languageTag = settings.languageTag.ifBlank { "en" }
    val tr: Translate = remember(strings, languageTag) {
        { key, fallback -> strings.text(languageTag, key, fallback) }
    }
    val rtl = strings.isRtl(languageTag)

    SideEffect {
        activity.window.statusBarColor = KitchenColors.Canvas.toArgb()
        activity.window.navigationBarColor = KitchenColors.Surface.toArgb()
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        val keepOn = settings.keepAwake && screen == AppScreen.LIVE
        if (keepOn) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler {
        if (!viewModel.backToHome()) activity.finish()
    }

    KitchenTheme {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val profile = kitchenWindowProfile(
                    width = maxWidth,
                    height = maxHeight,
                    fontScale = LocalDensity.current.fontScale,
                )
                val layoutType =
                    if (profile.useRail) NavigationSuiteType.NavigationRail
                    else NavigationSuiteType.NavigationBar
                val navigationColors = kitchenNavigationSuiteColors()
                val navigationItemColors = kitchenNavigationSuiteItemColors()

                var adLoaded by remember { mutableStateOf(false) }
                val adEligible =
                    billing.verifiedThisSession && !billing.active && consent.canRequestAds
                LaunchedEffect(adEligible) {
                    if (!adEligible) adLoaded = false
                }

                val scaffoldInsets = when {
                    profile.useRail && adLoaded -> WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                    )
                    profile.useRail -> WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top + WindowInsetsSides.Bottom
                    )
                    else -> WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                }
                val bannerInsets = if (profile.useRail && adLoaded) {
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                } else {
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                }

                NavigationSuiteScaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    layoutType = layoutType,
                    navigationSuiteColors = navigationColors,
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationSuiteItems = {
                        item(
                            selected = screen == AppScreen.HOME,
                            onClick = { viewModel.navigate(AppScreen.HOME) },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text(tr("home", "Home"), maxLines = 2) },
                            colors = navigationItemColors,
                        )
                        item(
                            selected = screen == AppScreen.LIVE,
                            onClick = { viewModel.openLive() },
                            icon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                            label = { Text(tr("live", "Live"), maxLines = 2) },
                            colors = navigationItemColors,
                        )
                        item(
                            selected = screen == AppScreen.BOARDS,
                            onClick = { viewModel.navigate(AppScreen.BOARDS) },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                            label = { Text(tr("boards", "Boards"), maxLines = 2) },
                            colors = navigationItemColors,
                        )
                        if (profile.useRail) {
                            item(
                                selected = screen == AppScreen.CREATE,
                                onClick = { viewModel.startNewBoard() },
                                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                                label = { Text(tr("new", "New"), maxLines = 2) },
                                colors = navigationItemColors,
                            )
                        }
                        item(
                            selected = screen == AppScreen.SETTINGS,
                            onClick = { viewModel.navigate(AppScreen.SETTINGS) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text(tr("settings", "Settings"), maxLines = 2) },
                            colors = navigationItemColors,
                        )
                    },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        Scaffold(
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.background,
                            contentWindowInsets = scaffoldInsets,
                            topBar = {
                                if (!profile.useRail) {
                                    KitchenTopBar(profile = profile)
                                }
                            },
                        ) { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(inner)
                                    .consumeWindowInsets(inner),
                            ) {
                                when (screen) {
                                    AppScreen.HOME -> HomeScreen(
                                        boards = boards,
                                        selectedBoard = selectedBoard,
                                        tasks = selectedTasks,
                                        profile = profile,
                                        tr = tr,
                                        onContinue = { viewModel.openLive(selectedBoard?.id) },
                                        onLane = { viewModel.openLive(selectedBoard?.id, it) },
                                        onRepeat = { viewModel.repeatMostRecent(selectedBoard?.id) },
                                        onNew = viewModel::startNewBoard,
                                        onPaste = viewModel::openPaste,
                                        onAll = { viewModel.navigate(AppScreen.BOARDS) },
                                        onOpenBoard = { viewModel.openLive(it) },
                                    )

                                    AppScreen.CREATE -> CreateScreen(
                                        draft = createDraft,
                                        step = createStep,
                                        profile = profile,
                                        tr = tr,
                                        onName = viewModel::updateName,
                                        onArea = viewModel::updateArea,
                                        onNotes = viewModel::updateNotes,
                                        onTime = viewModel::updateTargetMinutes,
                                        onTiming = viewModel::updateTimingMode,
                                        onAddTask = viewModel::addTask,
                                        onDeleteTask = viewModel::deleteTask,
                                        onUpdateTask = viewModel::updateTask,
                                        canAdvance = viewModel.canAdvanceCreate(),
                                        onBack = viewModel::previousCreateStep,
                                        onNext = viewModel::nextCreateStep,
                                    )

                                    AppScreen.PASTE -> PasteScreen(
                                        text = pasteText,
                                        profile = profile,
                                        tr = tr,
                                        onText = viewModel::setPasteText,
                                        onBack = { viewModel.navigate(AppScreen.HOME) },
                                        onImport = viewModel::importPaste,
                                    )

                                    AppScreen.BOARDS -> BoardsScreen(
                                        boards = boards,
                                        profile = profile,
                                        tr = tr,
                                        onNew = viewModel::startNewBoard,
                                        onOpen = { viewModel.openLive(it) },
                                    )

                                    AppScreen.LIVE -> LiveScreen(
                                        board = selectedBoard,
                                        tasks = selectedTasks,
                                        lane = liveLane,
                                        profile = profile,
                                        settings = settings,
                                        tr = tr,
                                        undoAvailable = undoTaskId != null,
                                        onLane = viewModel::selectLane,
                                        onPause = viewModel::setBoardPaused,
                                        onToggleTimer = { task ->
                                            if (!task.timerRunning && settings.alerts) {
                                                activity.ensureNotificationPermission()
                                            }
                                            viewModel.toggleTimer(task)
                                        },
                                        onDone = { viewModel.moveTask(it, LiveLane.DONE) },
                                        onCheck = viewModel::checkWaiting,
                                        onNow = { viewModel.moveTask(it, LiveLane.NOW) },
                                        onPriority = viewModel::togglePriority,
                                        onUndo = viewModel::undoLastTask,
                                        onClearUndo = viewModel::clearUndo,
                                        onRepeat = { viewModel.repeatMostRecent(selectedBoard?.id) },
                                    )

                                    AppScreen.SETTINGS -> SettingsScreen(
                                        settings = settings,
                                        strings = strings,
                                        profile = profile,
                                        billingPrice = billing.formattedPrice,
                                        consentPrivacyRequired = consent.privacyOptionsRequired,
                                        tr = tr,
                                        onLanguage = viewModel::setLanguage,
                                        onAlerts = viewModel::setAlerts,
                                        onAwake = viewModel::setAwake,
                                        onCompact = viewModel::setCompact,
                                        onHaptics = viewModel::setHaptics,
                                        onPrivacy = { activity.openExternal(BuildConfig.PRIVACY_POLICY_URL) },
                                        onTerms = { activity.openExternal(BuildConfig.TERMS_URL) },
                                        onSupport = { activity.openExternal(BuildConfig.SUPPORT_URL) },
                                        onSafety = { activity.openExternal(BuildConfig.SAFETY_URL) },
                                        onPrivacyChoices = { consentController.showPrivacyOptions(activity) },
                                        onRemoveAds = { billingController.purchase(activity) },
                                        onManageSubscription = { billingController.openManageSubscription(activity) },
                                        onDelete = viewModel::clearAllData,
                                    )
                                }
                            }
                        }

                        if (adEligible) {
                            NativeTestBanner(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(bannerInsets),
                                onLoadedChanged = { adLoaded = it },
                            )
                        }
                    }
                }

                if (showSafety) {
                    AlertDialog(
                        onDismissRequest = viewModel::dismissSafetyConfirmation,
                        title = { Text(tr("safety", "Safety")) },
                        text = {
                            Text(
                                tr(
                                    "foodSafetyDisclaimer",
                                    "This app is an organizational aid only. It does not guarantee food safety or doneness.",
                                )
                            )
                        },
                        dismissButton = {
                            TextButton(onClick = viewModel::dismissSafetyConfirmation) {
                                Text(tr("cancel", "Cancel"))
                            }
                        },
                        confirmButton = {
                            Button(onClick = viewModel::confirmSafetyAndCreate) {
                                Text(tr("continue", "Continue"))
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun KitchenTopBar(profile: KitchenWindowProfile) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                    )
                )
                .heightIn(min = profile.topBarHeight)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = Modifier
                    .size(if (profile.compactHeight) 36.dp else 40.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(11.dp))
            Text(
                "Kitchen Prep Board",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 18.sp,
                maxLines = 2,
            )
        }
    }
}

fun MainActivity.openExternal(url: String) {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
    if (uri.scheme != "https") return
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        // Core app remains usable if no browser is installed.
    }
}
