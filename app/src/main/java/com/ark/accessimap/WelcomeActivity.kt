package com.ark.accessimap

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ark.accessimap.ui.theme.AccessimapTheme
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Name : Screen("name")
    object AreYouReady : Screen("areyouready")
}

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccessimapTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WelcomeScreen()
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen() {
    val navController = rememberNavController()
    var username by remember { mutableStateOf("") }
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = Screen.Welcome.route) {
        composable(Screen.Welcome.route) {
            FirstWelcomeScreen { navController.navigate(Screen.Name.route) }
        }
        composable(Screen.Name.route) {
            NameScreen { name ->
                username = name

                val creds = context.getSharedPreferences("creds", MODE_PRIVATE)
                creds.edit {
                    this.putString("username", username)
                }

                navController.navigate(Screen.AreYouReady.route)
            }
        }
        composable(Screen.AreYouReady.route) {
            AreYouReadyScreen {
                val activity = (context as? ComponentActivity)
                activity?.startActivity(
                    Intent(context, MainActivity::class.java)
                )
                activity?.finish()
            }
        }
    }
}

@Composable
fun FirstWelcomeScreen(onContinue: () -> Unit) {
    val comp by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.bird_waving)
    )

    val progress by animateLottieCompositionAsState(
        comp,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        LottieAnimation(comp, progress, modifier = Modifier.size(250.dp))

        Spacer(Modifier.height(48.dp))

        Text("Hey there! I'm Accessi!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("Welcome to Accessimap!", textAlign = TextAlign.Center)

        Spacer(Modifier.weight(1f))

        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
    }
}

@Composable
fun NameScreen(onContinue: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.bird_waving))
    val progress by animateLottieCompositionAsState(comp, iterations = LottieConstants.IterateForever)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        LottieAnimation(comp, progress, modifier = Modifier.size(220.dp))

        Spacer(Modifier.height(24.dp))

        Text("What's your name?", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onContinue(name) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun AreYouReadyScreen(onContinue: () -> Unit) {
    val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.wheelchair))
    val progress by animateLottieCompositionAsState(comp, iterations = LottieConstants.IterateForever)
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        LottieAnimation(comp, progress, modifier = Modifier.size(220.dp))

        Spacer(Modifier.height(24.dp))

        Text("Are you ready to explore a world where accessibility is the 1st priority?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onContinue() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I'm ready!")
        }
    }
}