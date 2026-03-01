package com.personal.studentlifemanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.personal.studentlifemanager.ui.screens.BudgetScreen
import com.personal.studentlifemanager.ui.screens.ExpenseScreen
import com.personal.studentlifemanager.ui.screens.HomeScreen
import com.personal.studentlifemanager.ui.screens.LoginScreen
import com.personal.studentlifemanager.ui.screens.CategoryScreen
import com.personal.studentlifemanager.ui.screens.ExpenseAnalyticsScreen
import com.personal.studentlifemanager.ui.screens.PomodoroScreen
import com.personal.studentlifemanager.ui.screens.RecurringScreen
import com.personal.studentlifemanager.worker.ReminderWorker // 🔥 IMPORT ĐÚNG THƯ MỤC CỦA BẠN
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {

    // Lắng nghe kết quả xin quyền thông báo
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) scheduleDailyReminder()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        // 🔥 XIN QUYỀN VÀ CHẠY WORKMANAGER (Thêm vào đây)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                scheduleDailyReminder()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scheduleDailyReminder() // Android cũ không cần xin quyền
        }

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
                            },
                            onNavigateToPomodoro = { // 🔥 Truyền lệnh chuyển sang Pomodoro
                                navController.navigate("pomodoro")
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

                    // MÀN HÌNH POMODORO
                    composable("pomodoro") {
                        PomodoroScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToTimer = {
                                // Tạm thời để trống, lát nữa mình tạo màn hình Đếm ngược rồi gọi sau
                            }
                        )
                    }

                    // 4. Màn hình Quản lý Danh mục
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

                    // 6. Màn hình Ngân Sách
                    composable("budget") {
                        BudgetScreen(onBack = { navController.popBackStack() })
                    }

                    // 7. Màn hình Giao dịch định kỳ
                    composable("recurring") {
                        RecurringScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    // 🔥 HÀM LÊN LỊCH CHẠY NGẦM (ĐÃ NÂNG CẤP ĐỌC GIỜ TỪ BỘ NHỚ)
    private fun scheduleDailyReminder() {
        // 1. Đọc giờ, phút người dùng đã cài (Mặc định là 20:00)
        val sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val savedHour = sharedPref.getInt("reminder_hour", 20)
        val savedMinute = sharedPref.getInt("reminder_minute", 0)

        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, savedHour)
            set(Calendar.MINUTE, savedMinute)
            set(Calendar.SECOND, 0)
        }

        // Nếu lúc mở app đã qua giờ hẹn, thì dời sang ngày hôm sau
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<com.personal.studentlifemanager.worker.ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyExpenseReminder",
            ExistingPeriodicWorkPolicy.UPDATE, // Ghi đè lịch cũ
            dailyWorkRequest
        )
    }
}