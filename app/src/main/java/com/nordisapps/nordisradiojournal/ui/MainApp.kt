@file:Suppress("AssignedValueIsNeverRead")

package com.nordisapps.nordisradiojournal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.nordisapps.nordisradiojournal.R
import com.nordisapps.nordisradiojournal.data.model.AdminState
import com.nordisapps.nordisradiojournal.ui.components.FullPlayer
import com.nordisapps.nordisradiojournal.ui.components.MiniPlayer
import com.nordisapps.nordisradiojournal.ui.home.SnowOverlay
import com.nordisapps.nordisradiojournal.ui.theme.ThemeMode
import com.nordisapps.nordisradiojournal.viewmodel.AdminViewModel
import com.nordisapps.nordisradiojournal.viewmodel.AnnouncementsViewModel
import com.nordisapps.nordisradiojournal.viewmodel.FavouritesViewModel
import com.nordisapps.nordisradiojournal.viewmodel.PlayerViewModel
import com.nordisapps.nordisradiojournal.viewmodel.RadioFactsViewModel
import com.nordisapps.nordisradiojournal.viewmodel.RecentlyPlayedViewModel
import com.nordisapps.nordisradiojournal.viewmodel.StationsViewModel

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    playerViewModel: PlayerViewModel,
    stationsViewModel: StationsViewModel,
    recentlyPlayedViewModel: RecentlyPlayedViewModel,
    favouritesViewModel: FavouritesViewModel,
    adminViewModel: AdminViewModel,
    announcementsViewModel: AnnouncementsViewModel,
    userPhotoUrl: String?,
    userName: String?,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onLanguageChange: (String) -> Unit,
    currentLanguage: String,
    initialTab: Int,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    factsViewModel: RadioFactsViewModel
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var showUserMenu by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab) }
    val uiState by stationsViewModel.uiStateFlow.collectAsState()
    val playerState by playerViewModel.uiStateFlow.collectAsState()


    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            val route =
                                navController.currentBackStackEntryAsState().value?.destination?.route

                            Text(
                                text = buildString {
                                    append(
                                        when {
                                            route == "settings" -> stringResource(R.string.settings_title)
                                            route == "player_settings" -> stringResource(R.string.player_settings_title)
                                            route == "admin_panel" -> "Админ панель"
                                            route == "about" -> stringResource(R.string.about_app_title)
                                            route?.startsWith("edit_station_screen") == true -> {
                                                val stationId =
                                                    navController.currentBackStackEntry
                                                        ?.arguments
                                                        ?.getString("stationId")
                                                if (stationId == null) "Новая станция" else "Редактирование"
                                            }

                                            else -> stringResource(R.string.app_name)
                                        }
                                    )
                                    if (announcementsViewModel.isChristmas.value) {
                                        append(" 🎄")
                                    }
                                }
                            )
                        },
                        navigationIcon = {
                            val route =
                                navController.currentBackStackEntryAsState().value?.destination?.route
                            when {
                                route == "settings" || route == "player_settings" || route == "admin_panel" || route == "about" || route?.startsWith(
                                    "edit_station_screen"
                                ) == true -> {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            val route =
                                navController.currentBackStackEntryAsState().value?.destination?.route

                            if (route?.startsWith("edit_station_screen") != true) {
                                IconButton(onClick = {
                                    navController.navigate("settings") {
                                        launchSingleTop = true
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Настройки"
                                    )
                                }

                                Box {
                                    if (userPhotoUrl == null) {
                                        IconButton(onClick = onSignInClick) {
                                            Icon(
                                                Icons.Default.AccountCircle,
                                                contentDescription = "Войти"
                                            )
                                        }
                                    } else {
                                        IconButton(onClick = { showUserMenu = true }) {
                                            AsyncImage(
                                                model = userPhotoUrl,
                                                contentDescription = stringResource(R.string.profile),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showUserMenu,
                                            onDismissRequest = { showUserMenu = false }
                                        ) {
                                            userName?.let {
                                                DropdownMenuItem(
                                                    text = { Text(it) },
                                                    onClick = {},
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Default.AccountCircle,
                                                            contentDescription = null
                                                        )
                                                    },
                                                    enabled = false
                                                )
                                                HorizontalDivider()
                                            }
                                            if (uiState.adminState is AdminState.Admin) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.admin_button)) },
                                                    onClick = {
                                                        navController.navigate("admin_panel") {
                                                            launchSingleTop = true
                                                        }
                                                        showUserMenu = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Default.AdminPanelSettings,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.sign_out)) },
                                                onClick = {
                                                    showSignOutDialog = true
                                                    showUserMenu = false
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.ExitToApp,
                                                        contentDescription = null
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            },
            bottomBar = {
                val route = navController.currentBackStackEntryAsState().value?.destination?.route
                if (route == "home") {
                    NavigationBar {
                        val tabLabels = listOf(
                            R.string.nav_home,
                            R.string.nav_search,
                            R.string.nav_favorites,
                            R.string.nav_listen
                        )
                        val tabIcons = listOf(
                            Icons.Filled.Home to Icons.Outlined.Home,
                            Icons.Filled.Search to Icons.Outlined.Search,
                            Icons.Filled.Star to Icons.Outlined.StarBorder,
                            Icons.Filled.Headphones to Icons.Outlined.Headphones
                        )
                        tabLabels.forEachIndexed { index, labelRes ->
                            val (filledIcon, outlinedIcon) = tabIcons[index]
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == index) filledIcon else outlinedIcon,
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(labelRes)) },
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                    navController.navigate("home") {
                                        launchSingleTop = true
                                        popUpTo("home") { inclusive = false }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .background(Color.Transparent)
            ) {
                AppNavigation(
                    navController = navController,
                    stationsViewModel = stationsViewModel,
                    recentlyPlayedViewModel = recentlyPlayedViewModel,
                    favouritesViewModel = favouritesViewModel,
                    adminViewModel = adminViewModel,
                    announcementsViewModel = announcementsViewModel,
                    playerViewModel = playerViewModel,
                    factsViewModel = factsViewModel,
                    uiState = uiState,
                    selectedTab = selectedTab,
                    currentLanguage = currentLanguage,
                    onLanguageChange = onLanguageChange,
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange,
                    context = context
                )

                if (navController.currentBackStackEntryAsState().value?.destination?.route == "home") {
                    playerState.currentStation?.let { station ->
                        MiniPlayer(
                            station = station,
                            trackTitle = playerState.currentTrackTitle,
                            isPlaying = playerState.isPlaying,
                            onPlayPauseClick = { playerViewModel.togglePlayPause() },
                            onClose = { playerViewModel.closePlayer() },
                            onExpandClick = { showFullPlayer = true },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showFullPlayer,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            if (playerState.currentStation != null) {
                FullPlayer(
                    station = playerState.currentStation!!,
                    trackTitle = playerState.currentTrackTitle,
                    isPlaying = playerState.isPlaying,
                    onPlayPauseClick = { playerViewModel.togglePlayPause() },
                    currentBitrate = playerState.currentBitrate,
                    favouriteStations = uiState.favouriteStations,
                    onToggleFavourite = { favouritesViewModel.toggleFavourite(playerState.currentStation!!) },
                    onDismiss = { showFullPlayer = false },
                    onSleepTimerSet = { playerViewModel.setSleepTimer(it) },
                    activeTimerMinutes = playerState.activeTimerMinutes,
                    endTimerTime = playerState.endTimerTime
                )
            }
        }
        SnowOverlay(
            enabled = announcementsViewModel.isChristmas.value && !showFullPlayer,
            snowCount = 90
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.sign_out_dialog_title)) },
            text = { Text(stringResource(R.string.sign_out_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    onSignOutClick()
                }) {
                    Text(stringResource(R.string.sign_out_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}