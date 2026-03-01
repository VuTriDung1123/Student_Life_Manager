package com.personal.studentlifemanager

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.personal.studentlifemanager.ui.screens.BudgetScreen
import com.personal.studentlifemanager.ui.screens.ExpenseScreen
import com.personal.studentlifemanager.ui.screens.HomeScreen
import com.personal.studentlifemanager.ui.screens.LoginScreen
import com.personal.studentlifemanager.ui.screens.CategoryScreen
import com.personal.studentlifemanager.ui.screens.ExpenseAnalyticsScreen
import com.personal.studentlifemanager.ui.screens.RecurringScreen

class MainActivity : FragmentActivity() {
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

                    // 1. Màn hình Đăng nhập
                    composable("login") {
                        LoginScreen(onLoginSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        })
                    }

                    // 2. Màn hình Home
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
                                navController.navigate("expense")
                            }
                        )
                    }

                    // 3. Màn hình Chi tiêu (Trang chính)
                    composable("expense") {
                        ExpenseScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToAnalytics = { navController.navigate("expense_analytics") },
                            onNavigateToCategory = { navController.navigate("category_manage") },
                            onNavigateToBudget = { navController.navigate("budget")},
                            onNavigateToRecurring = { navController.navigate("recurring") }
                        )
                    }

                    // 4. Màn hình Quản lý Danh mục (Tránh bị văng app)
                    composable("category_manage") {
                        CategoryScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // 5. Màn hình Báo cáo tài chính (Analytics)
                    composable("expense_analytics") {
                        ExpenseAnalyticsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Màn hình Ngân Sách
                    composable("budget") {
                        BudgetScreen(onBack = { navController.popBackStack() })
                    }

                    // Màn hình Giao dịch định kỳ
                    composable("recurring") {
                        RecurringScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}