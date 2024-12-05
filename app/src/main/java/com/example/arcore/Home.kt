package com.example.arcore

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageScreen(navController: NavController) {
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar with app title and profile icon
        TopAppBar(
            title = {
                Text(
                    text = "InteriAR",
                    fontSize = 32.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            },
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Title above the plus sign
        Text(
            text = "Create New Project",
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Big plus sign button
        IconButton(
            onClick = {
                // Navigate to ARScreen for a new project
                navController.navigate("ar_screen")
            },
            modifier = Modifier
                .size(100.dp)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Navigate to ARScreen",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(100.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Button to join an existing session
        Button(
            onClick = {
                isLoading = true
                FirebaseManager.fetchLatestAnchorId(
                    onSuccess = { anchorId ->
                        isLoading = false
                        navController.navigate("ar_screen?anchorId=$anchorId")
                    },
                    onError = { error ->
                        isLoading = false
                        Toast.makeText(navController.context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Join Existing Session")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom navigation bar
        BottomAppBar(
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            IconButton(onClick = {
                // Navigate to Home Page
                navController.navigate("home") {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = {
                // Show Search Bar
                showSearchBar = !showSearchBar
            }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = { navController.navigate("account") }) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Account",
                    tint = Color.Black
                )
            }
        }

        // Search Bar
        if (showSearchBar) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        Button(
            onClick = { navController.navigate("catalog") },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Browse Catalog")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePageScreenPreview() {
    HomePageScreen(navController = rememberNavController())
}
