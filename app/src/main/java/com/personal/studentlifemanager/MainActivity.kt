package com.personal.studentlifemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.personal.studentlifemanager.ui.screens.ExpenseAnalyticsScreen
import com.personal.studentlifemanager.ui.screens.ExpenseScreen
import com.personal.studentlifemanager.ui.screens.HomeScreen
import com.personal.studentlifemanager.ui.screens.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        setContent {
            MaterialTheme {
                val auth = remember { FirebaseAuth.getInstance() }
                val navController = rememberNavController() // "Bộ điều hướng"

                // Kiểm tra trạng thái đăng nhập
                val currentUser = auth.currentUser
                val startDestination = if (currentUser == null) "login" else "home"

                NavHost(navController = navController, startDestination = startDestination) {
                    // Màn hình Đăng nhập
                    composable("login") {
                        LoginScreen(onLoginSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true } // Đăng nhập xong thì không quay lại trang login được nữa
                            }
                        })
                    }

                    // Màn hình Home
                    composable("home") {
                        HomeScreen(
                            userName = auth.currentUser?.displayName ?: "Sinh viên",
                            onLogout = {
                                auth.signOut()
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onNavigateToExpense = {
                                navController.navigate("expense") // Lệnh nhảy sang trang chi tiêu
                            }
                        )
                    }

                    // Màn hình Module 1: Chi tiêu
                    composable("expense") {
                        ExpenseScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToAnalytics = { navController.navigate("expense_analytics") } // Mở trang so sánh
                        )
                    }

                    // Màn hình Phân tích so sánh (Trang sâu)
                    composable("expense_analytics") {
                        ExpenseAnalyticsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}