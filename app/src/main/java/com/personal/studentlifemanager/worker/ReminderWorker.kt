package com.personal.studentlifemanager.worker // Nhớ sửa lại package name nếu cần

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.personal.studentlifemanager.MainActivity

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "expense_reminder_channel"

        // 1. Tạo Channel (Bắt buộc cho Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nhắc nhở chi tiêu",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Kênh gửi thông báo nhắc nhở ghi chép chi tiêu hằng ngày"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Tạo hành động khi bấm vào thông báo (Mở app lên)
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Build cái thông báo
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_agenda) // Icon tạm của hệ thống, bạn có thể thay bằng icon app
            .setContentTitle("Hôm nay bạn đã tiêu gì chưa?")
            .setContentText("Dành 1 phút ghi chép để kiểm soát tài chính tốt hơn nhé!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Bấm vào là tự mất thông báo
            .build()

        // 4. Bắn thông báo!
        notificationManager.notify(1001, notification)
    }
}