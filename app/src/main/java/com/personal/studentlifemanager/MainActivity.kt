package com.personal.studentlifemanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
import com.personal.studentlifemanager.ui.screens.PomodoroTimerScreen
import com.personal.studentlifemanager.ui.screens.RecurringScreen
import com.personal.studentlifemanager.worker.ReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.personal.studentlifemanager.ui.screens.PomodoroReportScreen
import java.net.URLEncoder
import java.net.URLDecoder

class MainActivity : FragmentActivity() {

    // Lắng nghe kết quả xin quyền thông báo
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        if (notifGranted) scheduleDailyReminder()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        // 🔥 XIN QUYỀN VÀ CHẠY WORKMANAGER (Thêm vào đây)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionsToRequest = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
            }

            if (permissionsToRequest.isNotEmpty()) {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            } else {
                scheduleDailyReminder()
            }
        } else {
            scheduleDailyReminder() // Android cũ
            // Xin quyền nghe gọi cho Android cũ
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE))
            }
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
                            },
                            onNavigateToFlashcard = {
                                navController.navigate("flashcard_decks")
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


                    // 🔥 MÀN HÌNH CHÍNH POMODORO
                    composable("pomodoro") {
                        PomodoroScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToTimer = { config, taskName ->

                                val safeTaskName = taskName.ifBlank { "Tự do" }
                                val encodedTask = URLEncoder.encode(safeTaskName, "UTF-8")

                                navController.navigate("pomodoro_timer/${config.focusTime}/${config.shortBreak}/${config.sessionsCount}/${config.longBreak}?taskName=$encodedTask")
                            },

                            onNavigateToReport = {
                                navController.navigate("report")
                            }
                        )
                    }


                    // 🔥 MÀN HÌNH ĐỒNG HỒ ĐẾM NGƯỢC (Đã sửa lỗi thiếu taskName)
                    composable(
                        route = "pomodoro_timer/{focus}/{short}/{sessions}/{long}?taskName={taskName}",
                        arguments = listOf(
                            navArgument("focus") { type = NavType.IntType },
                            navArgument("short") { type = NavType.IntType },
                            navArgument("sessions") { type = NavType.IntType },
                            navArgument("long") { type = NavType.IntType },
                            navArgument("taskName") { type = NavType.StringType; defaultValue = "Tự do" }
                        )
                    ) { backStackEntry ->
                        val focus = backStackEntry.arguments?.getInt("focus") ?: 25
                        val short = backStackEntry.arguments?.getInt("short") ?: 5
                        val sessions = backStackEntry.arguments?.getInt("sessions") ?: 4
                        val long = backStackEntry.arguments?.getInt("long") ?: 15

                        // Giải mã chữ lại cho đẹp
                        val rawTaskName = backStackEntry.arguments?.getString("taskName") ?: "Tự do"
                        val decodedTask = URLDecoder.decode(rawTaskName, "UTF-8")

                        PomodoroTimerScreen(
                            config = com.personal.studentlifemanager.data.model.PomodoroConfig(focus, short, sessions, long),
                            taskName = decodedTask, // 🔥 LỖI Ở ĐÂY NÈ: Đã bổ sung truyền taskName vào!
                            onBack = { navController.popBackStack() }
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

                    //8. Màn hình báo cáo
                    composable("report") {
                        PomodoroReportScreen(onBack = { navController.popBackStack() })
                    }

                    // 🔥 9. MÀN HÌNH QUẢN LÝ DECK FLASHCARD
                    composable("flashcard_decks") {
                        com.personal.studentlifemanager.ui.screens.FlashcardDecksScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToDeck = { deckId, deckName ->
                                val encodedName = java.net.URLEncoder.encode(deckName, "UTF-8")
                                navController.navigate("flashcard_list/$deckId?deckName=$encodedName")
                            }
                        )
                    }

                    // 🔥 10. MÀN HÌNH DANH SÁCH THẺ TRONG DECK
                    composable(
                        route = "flashcard_list/{deckId}?deckName={deckName}",
                        arguments = listOf(
                            androidx.navigation.navArgument("deckId") { type = androidx.navigation.NavType.StringType },
                            androidx.navigation.navArgument("deckName") { type = androidx.navigation.NavType.StringType; defaultValue = "Bộ thẻ" }
                        )
                    ) { backStackEntry ->
                        val deckId = backStackEntry.arguments?.getString("deckId") ?: ""
                        val rawDeckName = backStackEntry.arguments?.getString("deckName") ?: "Bộ thẻ"
                        val deckName = java.net.URLDecoder.decode(rawDeckName, "UTF-8")

                        com.personal.studentlifemanager.ui.screens.FlashcardListScreen(
                            deckId = deckId,
                            deckName = deckName,
                            onBack = { navController.popBackStack() },
                            // 🔥 THÊM CÁI NÀY ĐỂ KHI BẤM NÚT NÓ BAY SANG TRANG HỌC
                            onNavigateToStudy = { id, name ->
                                val encoded = java.net.URLEncoder.encode(name, "UTF-8")
                                navController.navigate("flashcard_study/$id/$encoded")
                            }
                        )
                    }

                    // 🔥 11. MÀN HÌNH HỌC FLASHCARD (STUDY ENGINE)
                    composable(
                        route = "flashcard_study/{deckId}/{deckName}",
                        arguments = listOf(
                            androidx.navigation.navArgument("deckId") { type = androidx.navigation.NavType.StringType },
                            androidx.navigation.navArgument("deckName") { type = androidx.navigation.NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val deckId = backStackEntry.arguments?.getString("deckId") ?: ""
                        val rawDeckName = backStackEntry.arguments?.getString("deckName") ?: "Bộ thẻ"
                        val deckName = java.net.URLDecoder.decode(rawDeckName, "UTF-8")

                        // Sẽ tạo file này ở Bước 2
                        com.personal.studentlifemanager.ui.screens.FlashcardStudyScreen(
                            deckId = deckId,
                            deckName = deckName,
                            onBack = { navController.popBackStack() }
                        )
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