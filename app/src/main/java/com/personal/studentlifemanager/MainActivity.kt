package com.personal.studentlifemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.personal.studentlifemanager.ui.screens.HomeScreen
import com.personal.studentlifemanager.ui.screens.LoginScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        //splashScreen.setKeepOnScreenCondition { true }
        setContent {
            MaterialTheme {
                val auth = remember { FirebaseAuth.getInstance() }
                // Trạng thái đăng nhập
                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
                var userName by remember { mutableStateOf(auth.currentUser?.displayName ?: "Sinh viên") }

                if (!isLoggedIn) {
                    LoginScreen(onLoginSuccess = { name ->
                        userName = name
                        isLoggedIn = true
                    })
                } else {
                    HomeScreen(
                        userName = userName,
                        onLogout = {
                            auth.signOut()
                            isLoggedIn = false
                        }
                    )
                }
            }
        }
    }
}