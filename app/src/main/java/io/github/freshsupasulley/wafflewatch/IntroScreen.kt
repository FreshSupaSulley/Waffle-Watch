package io.github.freshsupasulley.wafflewatch

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun IntroScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Waffle Watch",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "The Waffle House Index is an informal metric used to determine the severity of weather in a particular area.\n\n" +
                        "Waffle House is notorious for being open no matter the weather, so if a Waffle House is closed, you know it's serious.\n\n" +
                        "Use this app to determine how bad the weather actually is. If your local Waffle House is closed, maybe consider changing travel plans. Or use this app to find a tasty meal. Your choice.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onGetStarted) {
            Text("Get Started")
        }
    }
}
