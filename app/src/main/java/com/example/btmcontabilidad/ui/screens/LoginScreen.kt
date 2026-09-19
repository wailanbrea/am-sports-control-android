package com.example.btmcontabilidad.ui.screens

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.btmcontabilidad.R
import com.example.btmcontabilidad.data.repository.BackendApiProvider
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val provider = remember(context) { BackendApiProvider(context) }
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.am_sports_logo),
            contentDescription = "Logo de A&M Sports Control",
            modifier = Modifier.size(112.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "A&M Sports Control",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Control contable para tus bancas",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Iniciar sesión",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
                Text(
                    text = "Accede para consultar tus bancas y movimientos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contraseña") },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            runCatching {
                                provider.login(
                                    email = email,
                                    password = password,
                                    deviceName = Build.MODEL.ifBlank { Build.DEVICE }
                                )
                            }.onSuccess {
                                password = ""
                                onLoginSuccess()
                            }.onFailure {
                                errorMessage = it.message ?: "No se pudo iniciar sesión"
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Entrar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
