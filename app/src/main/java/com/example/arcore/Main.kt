import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.arcore.ui.theme.ARCTheme

class Main : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ARCTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Main Activity") },
                elevation = 4.dp
            )
        },
        content = {
            Text("Welcome to the Main Activity!", modifier = Modifier.padding(16.dp))
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ARCTheme {
        MainScreen()
    }
}
