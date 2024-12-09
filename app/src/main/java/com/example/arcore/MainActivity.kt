package com.example.arcore

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Log out user on start
        FirebaseAuth.getInstance().signOut()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        firestore = FirebaseFirestore.getInstance()

        // Initialize FirebaseAuth
        val auth = FirebaseAuth.getInstance()

        // Retrieve ARCore API Key from AndroidManifest.xml
        val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val arcoreApiKey = applicationInfo.metaData.getString("ARCORE_API_KEY")

        // Initialize ARCore with the API key
        initializeARCore(arcoreApiKey)

        setContent {
            val navController = rememberNavController()
            MyApp {
                AppNavigator(navController, auth)
            }
        }
    }

    private fun initializeARCore(apiKey: String?) {
        // Initialize ARCore with the provided API key
        if (apiKey != null) {
            println("ARCore API Key: $apiKey")
            // Your ARCore initialization code goes here
        } else {
            println("Failed to retrieve ARCore API Key")
        }
    }
}

@Composable
fun MyApp(content: @Composable () -> Unit) {
    content()
}

@Composable
fun AppNavigator(navController: NavHostController, auth: FirebaseAuth) {
    // Check if the user is logged out
    val currentUser = auth.currentUser
    val startDestination = if (currentUser == null) "login" else "home"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") { LoginScreen(navController, auth) }
        composable("new_account") { NewAccountScreen(navController, auth) }
        composable("home") { HomePageScreen(navController) }
        composable("catalog") { CatalogScreen(navController) }
        composable("account") { AccountScreen(navController) }

        // Navigate to ARScreen Activity with optional anchorId parameter
        composable("ar_screen?anchorId={anchorId}") { backStackEntry ->
            val anchorId = backStackEntry.arguments?.getString("anchorId")

            // Navigate to ARScreen activity and pass the anchorId
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                val intent = Intent(context, ARScreen::class.java).apply {
                    putExtra("anchorId", anchorId)
                }
                context.startActivity(intent)
            }
        }
    }
}
