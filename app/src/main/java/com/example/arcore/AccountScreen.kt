package com.example.arcore

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

data class RoomDesign(val name: String, val imageUrl: String?)

@OptIn(ExperimentalMaterial3Api::class) // Needed for TopAppBar
@Composable
fun AccountScreen(navController: NavController) {
    // Get the current user UID from FirebaseAuth
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val uid = currentUser?.uid ?: "" // Get the UID (empty string if not logged in)

    // Remember mutable state list to hold room designs
    val roomDesigns = remember { mutableStateListOf<RoomDesign>() }

    // Firebase storage reference
    val storage = FirebaseStorage.getInstance().reference

    // Fetch images associated with the current user's UID
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            fetchUserImages(storage, uid, roomDesigns)
        }
    }

    // Handle Logout
    fun logout() {
        auth.signOut()
        Log.d("AccountScreen", "User logged out")
        navController.navigate("login") {
            popUpTo("account") { inclusive = true }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Your Designs",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn {
                items(roomDesigns) { design ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                Log.d("AccountScreen", "Item clicked: ${design.name}")
                            }
                    ) {
                        Column {
                            if (design.imageUrl != null) {
                                val painter: Painter = rememberImagePainter(design.imageUrl)
                                Image(
                                    painter = painter,
                                    contentDescription = "Design Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                            Text(
                                text = design.name,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { logout() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }
}

// Fetch user images based on the UID from Firebase Storage
fun fetchUserImages(storage: StorageReference, uid: String, roomDesigns: MutableList<RoomDesign>) {
    val userImagesRef = storage.child("images/$uid")

    userImagesRef.listAll().addOnSuccessListener { result ->
        result.items.forEach { item ->
            item.downloadUrl.addOnSuccessListener { uri ->
                val designName = item.name  // Use the file name as the design name
                roomDesigns.add(RoomDesign(designName, uri.toString()))
                Log.d("Firebase", "Image URL: ${uri.toString()}")
            }.addOnFailureListener {
                Log.e("Firebase", "Failed to fetch image URL")
            }
        }
    }.addOnFailureListener {
        Log.e("Firebase", "Failed to list storage items")
    }
}

@Preview(showBackground = true)
@Composable
fun AccountScreenPreview() {
    AccountScreen(navController = rememberNavController())
}
