package com.example.arcore

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import android.util.Log
import com.example.arcore.R

@OptIn(ExperimentalMaterial3Api::class) // Needed for TopAppBar
@Composable
fun CatalogScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

    // Map each item to a drawable resource
    val items = mapOf(
        "Chair" to R.drawable.chair,
        "Table" to R.drawable.table,
        "Lamp" to R.drawable.lamp,
        "Sofa" to R.drawable.sofa,
        "Desk" to R.drawable.desk
    )

    val filteredItems = items.filterKeys { it.contains(searchQuery.text, ignoreCase = true) }

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
                items(filteredItems.toList().size) { index ->
                    val item = filteredItems.toList()[index]
                    val itemName = item.first
                    val itemImageRes = item.second

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                Log.d("CatalogScreen", "Item clicked: $itemName")
                            }
                    ) {
                        Image(
                            painter = painterResource(id = itemImageRes),
                            contentDescription = itemName,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = itemName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                    Divider(color = Color.Gray, thickness = 1.dp)
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
