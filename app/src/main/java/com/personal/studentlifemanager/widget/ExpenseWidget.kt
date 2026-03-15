package com.personal.studentlifemanager.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider // 🔥 ĐÃ THÊM IMPORT NÀY
import com.personal.studentlifemanager.MainActivity
import java.text.NumberFormat
import java.util.Locale

class ExpenseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = context.getSharedPreferences("ExpenseWidgetPrefs", Context.MODE_PRIVATE)
            val balance = prefs.getLong("balance", 0L)
            val income = prefs.getLong("income", 0L)
            val expense = prefs.getLong("expense", 0L)
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFFE3F2FD)) // Màu nền xanh nhạt
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hàng chứa Tiêu đề và Nút Reload
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Số dư hiện tại",
                        // 🔥 ĐÃ SỬA GỌN GÀNG LẠI THÀNH ColorProvider()
                        style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 14.sp),
                        modifier = GlanceModifier.defaultWeight()
                    )

                    Image(
                        provider = ImageProvider(android.R.drawable.ic_popup_sync),
                        contentDescription = "Làm mới",
                        modifier = GlanceModifier
                            .size(24.dp)
                            .clickable(actionRunCallback<RefreshWidgetAction>())
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Số tiền (Bấm vào đây để mở app)
                Text(
                    text = formatter.format(balance),
                    modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
                    style = TextStyle(
                        // 🔥 ĐÃ SỬA GỌN GÀNG LẠI THÀNH ColorProvider()
                        color = ColorProvider(Color(0xFF1565C0)),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(16.dp))

                // Hàng chứa Thu - Chi
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.Start) {
                        Text("Thu nhập", style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.Gray)))
                        Text(formatter.format(income), style = TextStyle(fontSize = 14.sp, color = ColorProvider(Color(0xFF2E7D32)), fontWeight = FontWeight.Bold))
                    }
                    Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                        Text("Chi tiêu", style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.Gray)))
                        Text(formatter.format(expense), style = TextStyle(fontSize = 14.sp, color = ColorProvider(Color(0xFFC62828)), fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            ExpenseWidget().update(context, glanceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseWidget()
}