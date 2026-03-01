package com.example.aiaccounting

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.aiaccounting.data.local.database.AppDatabase
import com.example.aiaccounting.data.local.database.DatabaseFactory
import com.example.aiaccounting.security.SecurityChecker
import com.example.aiaccounting.security.SecurityManager
import com.example.aiaccounting.ui.navigation.AppNavigation
import com.example.aiaccounting.ui.navigation.Screen
import com.example.aiaccounting.ui.theme.AIAccountingTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var securityManager: SecurityManager

    @Inject
    lateinit var databaseFactory: DatabaseFactory

    @Inject
    lateinit var securityChecker: SecurityChecker

    private val mainViewModel: MainViewModel by viewModels()

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "需要存储权限才能导出文件", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Perform security checks
        val securityResult = securityChecker.performSecurityChecks()
        if (!securityResult.isSecure) {
            // Show warning but allow to continue (you can block in production)
            val warning = securityResult.getWarningMessage()
            Toast.makeText(this, "安全警告: $warning", Toast.LENGTH_LONG).show()
        }

        setContent {
            AIAccountingTheme {
                MainApp(
                    securityManager = securityManager,
                    databaseFactory = databaseFactory,
                    onRequestStoragePermission = { requestStoragePermission() }
                )
            }
        }
    }

    private fun requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ doesn't need storage permission for app-specific directories
            return
        }
        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}

@Composable
fun MainApp(
    securityManager: SecurityManager,
    databaseFactory: DatabaseFactory,
    onRequestStoragePermission: () -> Unit
) {
    var isPinSet by remember { mutableStateOf(securityManager.isPinSet()) }
    var isDatabaseInitialized by remember { mutableStateOf(false) }
    var hasInitialSetup by remember { mutableStateOf(false) }

    // Determine start destination
    val startDestination = when {
        !isPinSet -> Screen.SetupPin.route
        !isDatabaseInitialized -> Screen.Login.route
        !hasInitialSetup -> Screen.InitialSetup.route
        else -> Screen.Home.route
    }

    val navController = rememberNavController()

    // Global PIN holder for database initialization
    var globalPin by remember { mutableStateOf<String?>(null) }

    AppNavigation(
        navController = navController,
        startDestination = startDestination,
        onSetupComplete = { pin ->
            // PIN setup completed
            isPinSet = true
            globalPin = pin
            // Initialize database with the PIN
            try {
                databaseFactory.initializeDatabase(pin)
                isDatabaseInitialized = true
            } catch (e: Exception) {
                // Handle error
            }
        },
        onLoginSuccess = { pin ->
            // Login successful, initialize database
            globalPin = pin
            try {
                databaseFactory.initializeDatabase(pin)
                isDatabaseInitialized = true
            } catch (e: Exception) {
                // Handle error
            }
        }
    )
}

/**
 * Main ViewModel for MainActivity
 */
class MainViewModel : androidx.lifecycle.ViewModel() {
    // Add any necessary state management here
}