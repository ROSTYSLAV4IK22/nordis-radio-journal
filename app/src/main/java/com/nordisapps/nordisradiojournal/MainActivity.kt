package com.nordisapps.nordisradiojournal

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.nordisapps.nordisradiojournal.ui.components.MiniPlayer
import kotlinx.coroutines.launch
import java.util.Locale

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager

    private val _userPhotoUrl = mutableStateOf<String?>(null)
    val userPhotoUrl: String? get() = _userPhotoUrl.value

    private val _userName = mutableStateOf<String?>(null)
    val userName: String? get() = _userName.value

    override fun attachBaseContext(newBase: Context) {
        val langCode = getSavedLanguage(newBase)
        val locale = Locale.forLanguageTag(langCode)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        credentialManager = CredentialManager.create(this)

        checkUserAuthStatus()

        setContent {
            val currentLanguage by viewModel.languageFlow.collectAsState(initial = "en")

            val context = LocalContext.current
            val locale = Locale.forLanguageTag(currentLanguage)
            val localizedContext = context.createConfigurationContext(
                Configuration().apply { setLocale(locale) }
            )
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalImageLoader provides (application as MyApp).imageLoader
            ) {
                NordisRadioJournalTheme {
                    MainApp(
                        viewModel = viewModel,
                        userPhotoUrl = userPhotoUrl,
                        userName = userName,
                        onSignInClick = { startSignIn() },
                        onSignOutClick = { signOut() },
                        onLanguageChange = { lang ->
                            viewModel.changeLanguage(lang)
                        },
                        currentLanguage = currentLanguage
                    )
                }
            }
        }
    }


    private fun checkUserAuthStatus() {
        lifecycleScope.launch {
            AuthManager.getUser(this@MainActivity).collect { user ->
                if (user.username.isNotEmpty()) {
                    _userPhotoUrl.value = user.photoUrl
                    _userName.value = user.username
                } else {
                    _userPhotoUrl.value = null
                    _userName.value = null
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
                val userName = credential.displayName
                val photo = credential.profilePictureUri?.toString()

                AuthManager.saveUser(
                    context = this@MainActivity,
                    username = userName,
                    photo = photo
                )

                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.welcome_message, userName),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: GetCredentialException) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.sign_in_error, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun signOut() {
        lifecycleScope.launch {
            try {
                AuthManager.clearUser(this@MainActivity)
                credentialManager.clearCredentialState(
                    androidx.credentials.ClearCredentialStateRequest()
                )
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

    private fun getSavedLanguage(context: Context): String {
        return try {
            val sharedPref = context.getSharedPreferences("language_settings", MODE_PRIVATE)
            sharedPref.getString("selected_language", "en") ?: "en"
        } catch (e: Exception) {
            Log.w("MainActivity", "Failed to load saved language, using default: ${e.message}")
            "en"
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
        currentLanguage: String
    ) {
        val navController = rememberNavController()
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }
        var showUserMenu by remember { mutableStateOf(false) }
        var showSignOutDialog by remember { mutableStateOf(false) }

        val uiState by viewModel.uiState.collectAsState()

        // вычисляем title по текущему маршруту (чтобы показывать "Настройки" на settings)
        val navBackStack by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStack?.destination?.route ?: "main"
        val topBarTitle = when (currentRoute) {
            "settings" -> stringResource(R.string.settings_title)
            else -> when (selectedTab) {
                0 -> stringResource(R.string.app_name)
                1 -> stringResource(R.string.title_search_stations)
                2 -> stringResource(R.string.title_favorites)
                3 -> stringResource(R.string.title_online_radio)
                else -> stringResource(R.string.app_name)
            }
        }
        val tabLabels = listOf(
            R.string.nav_home,
            R.string.nav_search,
            R.string.nav_favorites,
            R.string.nav_listen
        )

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text(topBarTitle) },
                        navigationIcon = {
                            if (currentRoute == "settings") {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { navController.navigate("settings") }) {
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
                    )
                    HorizontalDivider()
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier.background(Color.Transparent)
                ) {
                    uiState.currentStation?.let { station ->
                        MiniPlayer(
                            station = station,
                            trackTitle = uiState.currentTrackTitle,
                            isPlaying = uiState.isPlaying,
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onClose = { viewModel.closePlayer() },
                            imageLoader = (LocalContext.current.applicationContext as MyApp).imageLoader
                        )
                    }
                    NavigationBar {
                        listOf(
                            0 to (Icons.Filled.Home to Icons.Outlined.Home),
                            1 to (Icons.Filled.Search to Icons.Outlined.Search),
                            2 to (Icons.Filled.Star to Icons.Outlined.StarBorder),
                            3 to (Icons.Filled.Headphones to Icons.Outlined.Headphones)
                        ).forEach { (index, icons) ->
                            val (filledIcon, outlinedIcon) = icons
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == index) filledIcon else outlinedIcon,
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(tabLabels[index])) },
                                selected = selectedTab == index,
                                onClick = { selectedTab = index }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "main",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    "main",
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        selectedTab = selectedTab
                    )
                }
                composable(
                    "settings",
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    SettingsMenu(
                        currentLanguage = currentLanguage,
                        onLanguageChange = { lang ->
                            onLanguageChange(lang)
                            navController.popBackStack()
                        }
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
                    }
                    ) {
                        Text(stringResource(R.string.sign_out_dialog_confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSignOutDialog = false },
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
