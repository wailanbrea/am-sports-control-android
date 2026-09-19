package com.example.btmcontabilidad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.btmcontabilidad.data.repository.BackendApiProvider
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.ui.navigation.MainAppShell
import com.example.btmcontabilidad.ui.screens.LoginScreen
import com.example.btmcontabilidad.ui.theme.BTMContabilidadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RepositoryContainer.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            BTMContabilidadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current.applicationContext
    val provider = remember(context) { BackendApiProvider(context) }
    var isAuthenticated by remember { mutableStateOf(provider.isAuthenticated()) }

    if (isAuthenticated) {
        MainAppShell(
            onLogout = {
                provider.logout()
                isAuthenticated = false
            }
        )
    } else {
        LoginScreen(onLoginSuccess = { isAuthenticated = true })
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun MainActivityPreview() {
    BTMContabilidadTheme {
        MainAppShell()
    }
}
