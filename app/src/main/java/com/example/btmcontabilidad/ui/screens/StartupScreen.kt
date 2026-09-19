package com.example.btmcontabilidad.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.btmcontabilidad.R
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue

@Composable
fun StartupScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.am_sports_logo),
            contentDescription = "Logo de A&M Sports Control",
            modifier = Modifier.size(128.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "A&M Sports Control",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 3.dp)
    }
}
