package com.example.arcore

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class) // Needed for TopAppBar
@Composable
fun CatalogScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val items = listOf("Chair", "Table", "Lamp", "Sofa", "Desk") // Example items
    val filteredItems = items.filter { it.contains(searchQuery.text, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catalog") },
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
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary)
                    .padding(8.dp),
                decorationBox = { innerTextField ->
                    Box(Modifier.padding(8.dp)) {
                        if (searchQuery.text.isEmpty()) {
                            Text("Search items", style = MaterialTheme.typography.bodyLarge)
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(filteredItems.size) { index ->
                    Text(
                        text = filteredItems[index],
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                Log.d("CatalogScreen", "Item clicked")
                            }
                    )
                    Divider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CatalogScreenPreview() {
    CatalogScreen(navController = rememberNavController())
}
