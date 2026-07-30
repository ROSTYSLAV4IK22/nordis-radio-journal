@file:Suppress("AssignedValueIsNeverRead", "DEPRECATION")

package com.nordisapps.nordisradiojournal

import android.app.Activity
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.nordisapps.nordisradiojournal.data.AuthManager
import com.nordisapps.nordisradiojournal.data.LanguageManager
import com.nordisapps.nordisradiojournal.data.getThemeFlow
import com.nordisapps.nordisradiojournal.data.saveTheme
import com.nordisapps.nordisradiojournal.ui.MainApp
import com.nordisapps.nordisradiojournal.ui.theme.LocalImageLoader
import com.nordisapps.nordisradiojournal.ui.theme.NordisRadioJournalTheme
import com.nordisapps.nordisradiojournal.ui.theme.ThemeMode
import com.nordisapps.nordisradiojournal.viewmodel.AdminViewModel
import com.nordisapps.nordisradiojournal.viewmodel.AnnouncementsViewModel
import com.nordisapps.nordisradiojournal.viewmodel.AppViewModelFactory
import com.nordisapps.nordisradiojournal.viewmodel.AuthViewModel
import com.nordisapps.nordisradiojournal.viewmodel.FavouritesViewModel
import com.nordisapps.nordisradiojournal.viewmodel.LanguageViewModel
import com.nordisapps.nordisradiojournal.viewmodel.PlayerViewModel
import com.nordisapps.nordisradiojournal.viewmodel.RadioFactsViewModel
import com.nordisapps.nordisradiojournal.viewmodel.RecentlyPlayedViewModel
import com.nordisapps.nordisradiojournal.viewmodel.StationsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

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
    private val appViewModelFactory: AppViewModelFactory by lazy {
        AppViewModelFactory(application, (application as MyApp).sharedState)
    }
    private val viewModel: LanguageViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels {
        appViewModelFactory
    }
    private val stationsViewModel: StationsViewModel by viewModels {
        appViewModelFactory
    }
    private val favouritesViewModel: FavouritesViewModel by viewModels {
        appViewModelFactory
    }
    private val recentlyPlayedViewModel: RecentlyPlayedViewModel by viewModels {
        appViewModelFactory
    }
    private val adminViewModel: AdminViewModel by viewModels {
        appViewModelFactory
    }
    private val authViewModel: AuthViewModel by viewModels {
        appViewModelFactory
    }
    private val announcementsViewModel: AnnouncementsViewModel by viewModels {
        appViewModelFactory
    }
    private val factsViewModel: RadioFactsViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val _userPhotoUrl = mutableStateOf<String?>(null)
    val userPhotoUrl: String? get() = _userPhotoUrl.value

    private val _userName = mutableStateOf<String?>(null)
    val userName: String? get() = _userName.value

    private var initialTab by mutableIntStateOf(0)

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                signInToFirebase(
                    account.idToken!!,
                    account.displayName,
                    account.photoUrl?.toString()
                )
            } catch (e: ApiException) {
                Log.e("AUTH", "Google sign in failed", e)
                Toast.makeText(
                    this,
                    getString(R.string.sign_in_error, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val langCode = LanguageManager.getLanguage(newBase)
        val locale = Locale.forLanguageTag(langCode)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stationsViewModel
        favouritesViewModel
        recentlyPlayedViewModel.loadRecentlyPlayed()
        authViewModel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        enableEdgeToEdge()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        handleIntent(intent)

        checkUserAuthStatus()

        viewModel.changeLanguage(LanguageManager.getLanguage(this))

        setContent {
            val app = application as MyApp
            CompositionLocalProvider(
                LocalImageLoader provides app.imageLoader
            ) {
                val themeMode by getThemeFlow(this@MainActivity)
                    .collectAsState(initial = ThemeMode.SYSTEM)
                val darkTheme = when (themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
                val view = LocalView.current
                val activity = view.context as Activity
                val window = activity.window
                val controller = WindowCompat.getInsetsController(window, view)
                SideEffect {
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme
                }

                NordisRadioJournalTheme(darkTheme = darkTheme) {
                    val currentLanguage by viewModel.languageFlow.collectAsState(
                        initial = LanguageManager.getLanguage(
                            this
                        )
                    )

                    val scope = rememberCoroutineScope()

                    MainApp(
                        adminViewModel = adminViewModel,
                        stationsViewModel = stationsViewModel,
                        recentlyPlayedViewModel = recentlyPlayedViewModel,
                        favouritesViewModel = favouritesViewModel,
                        announcementsViewModel = announcementsViewModel,
                        playerViewModel = playerViewModel,
                        userPhotoUrl = userPhotoUrl,
                        userName = userName,
                        onSignInClick = { startSignIn() },
                        onSignOutClick = { signOut() },
                        onLanguageChange = { lang ->
                            LanguageManager.saveLanguage(this, lang)
                            viewModel.changeLanguage(lang)

                            scope.launch {
                                delay(250.milliseconds)
                                recreate()
                            }
                        },
                        currentLanguage = currentLanguage,
                        initialTab = initialTab,
                        currentTheme = themeMode,
                        onThemeChange = { newTheme ->
                            lifecycleScope.launch {
                                saveTheme(this@MainActivity, newTheme)
                            }
                        },
                        factsViewModel = factsViewModel
                    )
                }
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
                } else {
                    _userPhotoUrl.value = null
                    _userName.value = null
                }
            }
        }
    }

    private fun startSignIn() {
        signInLauncher.launch(googleSignInClient.signInIntent)
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

                lifecycleScope.launch {
                    AuthManager.saveUser(
                        context = this@MainActivity,
                        username = userName,
                        photo = photoUrl
                    )

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
                googleSignInClient.signOut()
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
}