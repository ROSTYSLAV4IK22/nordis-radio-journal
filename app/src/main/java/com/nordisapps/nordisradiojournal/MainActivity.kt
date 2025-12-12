@file:Suppress("AssignedValueIsNeverRead")

package com.nordisapps.nordisradiojournal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nordisapps.nordisradiojournal.ui.theme.NordisRadioJournalTheme
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.nordisapps.nordisradiojournal.tools.AdminPanelScreen
import com.nordisapps.nordisradiojournal.tools.EditStationScreen
import com.nordisapps.nordisradiojournal.ui.components.FullPlayer
import com.nordisapps.nordisradiojournal.ui.components.MiniPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        }
    }
    private val viewModel: MainViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager

    private val _userPhotoUrl = mutableStateOf<String?>(null)
    val userPhotoUrl: String? get() = _userPhotoUrl.value

    private val _userName = mutableStateOf<String?>(null)
    val userName: String? get() = _userName.value

    private var initialTab by mutableIntStateOf(0)

    override fun attachBaseContext(newBase: Context) {
        val langCode = LanguageManager.getLanguage(newBase)
        val locale = Locale.forLanguageTag(langCode)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        enableEdgeToEdge()
        credentialManager = CredentialManager.create(this)

        handleIntent(intent)

        checkUserAuthStatus()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            viewModel.onUserChanged()
        }

        viewModel.changeLanguage(LanguageManager.getLanguage(this))

        setContent {
            NordisRadioJournalTheme {
                val currentLanguage by viewModel.languageFlow.collectAsState(
                    initial = LanguageManager.getLanguage(
                        this
                    )
                )

                val scope = rememberCoroutineScope()

                MainApp(
                    viewModel = viewModel,
                    userPhotoUrl = userPhotoUrl,
                    userName = userName,
                    onSignInClick = { startSignIn() },
                    onSignOutClick = { signOut() },
                    onLanguageChange = { lang ->
                        LanguageManager.saveLanguage(this, lang)
                        viewModel.changeLanguage(lang)

                        scope.launch {
                            delay(250)
                            recreate()
                        }
                    },
                    currentLanguage = currentLanguage,
                    initialTab = initialTab
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun checkUserAuthStatus() {
        lifecycleScope.launch {
            AuthManager.getUser(this@MainActivity).collect { user ->
                if (user.username.isNotEmpty()) {
                    _userPhotoUrl.value = user.photoUrl
                    _userName.value = user.username
                    viewModel.onUserChanged()
                } else {
                    _userPhotoUrl.value = null
                    _userName.value = null
                    viewModel.onUserChanged()
                }
            }
        }
    }

    private fun startSignIn() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("700543803497-12e55ldbu0tf2vfoc9t1u9ipqk3e02d5.apps.googleusercontent.com")
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity,
                )
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)

                val idToken = credential.idToken
                signInToFirebase(
                    idToken,
                    credential.displayName,
                    credential.profilePictureUri?.toString()
                )

            } catch (e: GetCredentialException) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.sign_in_error, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun signInToFirebase(idToken: String, userName: String?, photoUrl: String?) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

        FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
            .addOnSuccessListener { authResult ->
                val user = authResult.user

                Log.d("AUTH", "✅ Firebase SignIn SUCCESS!")
                Log.d("AUTH", "User ID: ${user?.uid}")
                Log.d("AUTH", "Email: ${user?.email}")
                Log.d("AUTH", "Display Name: ${user?.displayName}")

                // Сохраняем локально
                lifecycleScope.launch {
                    AuthManager.saveUser(
                        context = this@MainActivity,
                        username = userName,
                        photo = photoUrl
                    )
                    viewModel.onUserChanged()

                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.welcome_message, userName),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { error ->
                Log.e("AUTH", "❌ Firebase SignIn FAILED: ${error.message}")
                Toast.makeText(
                    this@MainActivity,
                    "Firebase Auth Error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun signOut() {
        lifecycleScope.launch {
            try {
                AuthManager.clearUser(this@MainActivity)
                credentialManager.clearCredentialState(
                    androidx.credentials.ClearCredentialStateRequest()
                )
                viewModel.onUserChanged()
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.signed_out_message),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.w("MainActivity", "Error signing out: ${e.message}", e)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.sign_out_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.getStringExtra("shortcut_action")) {
            "open_search" -> initialTab = 1
            "open_favorites" -> initialTab = 2
        }
    }

    @Composable
    fun MainApp(
        viewModel: MainViewModel,
        userPhotoUrl: String?,
        userName: String?,
        onSignInClick: () -> Unit,
        onSignOutClick: () -> Unit,
        onLanguageChange: (String) -> Unit,
        currentLanguage: String,
        initialTab: Int
    ) {
        val context = LocalContext.current
        val navController = rememberNavController()
        var showUserMenu by remember { mutableStateOf(false) }
        var showSignOutDialog by remember { mutableStateOf(false) }
        var showFullPlayer by remember { mutableStateOf(false) }
        var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab) }

        val uiState by viewModel.uiState.collectAsState()


Box(modifier = Modifier.fillMaxSize()) {

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        val route =
                            navController.currentBackStackEntryAsState().value?.destination?.route
                        Text(
                            when {
                                route == "settings" -> stringResource(R.string.settings_title)
                                route == "admin_panel" -> "Админ панель"
                                route?.startsWith("edit_station_screen") == true -> {
                                    val stationId = navController.currentBackStackEntry?.arguments?.getString("stationId")
                                    if (stationId == null) "Новая станция" else "Редактирование"
                                }
                                else -> stringResource(R.string.app_name)
                            }
                        )
                    },
                    navigationIcon = {
                        val route =
                            navController.currentBackStackEntryAsState().value?.destination?.route
                        when {
                            route == "settings" || route == "admin_panel" || route?.startsWith("edit_station_screen") == true -> {
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
                        // Показываем actions только если НЕ на экране редактирования
                        if (route?.startsWith("edit_station_screen") != true) {
                            IconButton(onClick = {
                                navController.navigate("settings") {
                                    launchSingleTop = true
                                }
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Настройки")
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
                                        if (uiState.isUserAdmin) {
                                            DropdownMenuItem(
                                                text = { Text(context.getString(R.string.admin_button)) },
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
                                            text = { Text(context.getString(R.string.sign_out)) },
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
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .background(Color.Transparent)
        ) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { it })
                },
                exitTransition = {
                    slideOutHorizontally(targetOffsetX = { -it })
                },
                popEnterTransition = {
                    slideInHorizontally(initialOffsetX = { -it })
                },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { it })
                }
            ) {
                composable("home") {
                    MainScreen(
                        viewModel = viewModel,
                        selectedTab = selectedTab
                    )
                }
                composable("settings") {
                    SettingsMenu(
                        currentLanguage = currentLanguage,
                        onLanguageChange = { langCode ->
                            onLanguageChange(langCode)
                        }
                    )
                }
                composable("admin_panel") {
                    AdminPanelScreen(
                        uiState = uiState,
                        imageLoader = (context.applicationContext as MyApp).imageLoader,
                        onDeleteStationClicked = { station ->
                            viewModel.deleteStation(
                                station = station,
                                onSuccess = {
                                    Toast.makeText(
                                        context, "Станция \"${station.name}\" удалена",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onFailure = { error ->
                                    Toast.makeText(
                                        context,
                                        "Ошибка удаления: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        },
                        onAddStationClicked = { navController.navigate("edit_station_screen") },
                        onEditStationClicked = { station ->
                            navController.navigate("edit_station_screen?stationId=${station.id}")
                        }
                    )
                }
                composable(
                    route = "edit_station_screen?stationId={stationId}",
                    arguments = listOf(navArgument("stationId") {
                        type = NavType.StringType
                        nullable = true
                    })
                ) {
                        backStackEntry ->
                    val stationId = backStackEntry.arguments?.getString("stationId")
                    EditStationScreen(
                        stationId = stationId,
                        uiState = uiState,
                        onSaveStation = { stationToSave ->
                            viewModel.saveStation(
                                station = stationToSave,
                                onSuccess = {
                                    // Если все сохранилось успешно, возвращаемся на экран админ-панели
                                    navController.popBackStack()
                                    // И показываем приятное сообщение
                                    Toast.makeText(context, "Станция сохранена!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { error ->
                                    // Если произошла ошибка, показываем ее, чтобы понять, в чем дело
                                    Toast.makeText(context, "Ошибка сохранения: ${error.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    )
                }
            }
            if (navController.currentBackStackEntryAsState().value?.destination?.route == "home") {
                uiState.currentStation?.let { station ->
                    MiniPlayer(
                        station = station,
                        trackTitle = uiState.currentTrackTitle,
                        isPlaying = uiState.isPlaying,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onClose = { viewModel.closePlayer() },
                        onExpandClick = { showFullPlayer = true },
                        imageLoader = (context.applicationContext as MyApp).imageLoader,
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
        if (uiState.currentStation != null) {
            FullPlayer(
                station = uiState.currentStation!!,
                trackTitle = uiState.currentTrackTitle,
                isPlaying = uiState.isPlaying,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                currentBitrate = uiState.currentBitrate,
                favouriteStations = uiState.favouriteStations,
                onToggleFavourite = { viewModel.toggleFavourite(uiState.currentStation!!) },
                imageLoader = (context.applicationContext as MyApp).imageLoader,
                onDismiss = { showFullPlayer = false } // Передаем действие для закрытия
            )
        }
    }
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
}