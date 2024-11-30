package com.example.arcore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Firestore instance
        firestore = FirebaseFirestore.getInstance()

        // Initialize FirebaseAuth
        val auth = FirebaseAuth.getInstance()

        setContent {
            // Remember the navigation controller
            val navController = rememberNavController()

            MyApp {
                AppNavigator(navController, auth)
            }
        }
    }
}

@Composable
fun MyApp(content: @Composable () -> Unit) {
    content()
}

@Composable
fun AppNavigator(navController: androidx.navigation.NavHostController, auth: FirebaseAuth) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController, auth) }
        composable("new_account") { NewAccountScreen(navController, auth) }
        composable("home") { HomePageScreen(navController) }
        composable("catalog") { CatalogScreen(navController) }
        composable("account") { AccountScreen(navController) }
    }
}

