package com.personal.studentlifemanager.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.personal.studentlifemanager.MainActivity

class PomodoroReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        // Lấy tên Task đã được truyền vào lúc lên lịch
        val taskName = inputData.getString("taskName") ?: "Tự do"
        sendNotification(taskName)
        return Result.success()
    }

    private fun sendNotification(taskName: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "pomodoro_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nhắc nhở cày cuốc",
                NotificationManager.IMPORTANCE_HIGH // Báo thức thì phải rung và kêu to
            )
            manager.createNotificationChannel(channel)
        }

        // Bấm vào thông báo sẽ mở lại App
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Đến giờ cày cuốc rồi! 🔥")
            .setContentText("Mục tiêu: $taskName. Bấm vào đây để bắt đầu ngay!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}