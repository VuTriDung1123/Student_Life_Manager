package com.personal.studentlifemanager.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object WidgetUpdater {
    @OptIn(DelicateCoroutinesApi::class)
    fun updateSafe(context: Context) {
        // 🔥 BÍ QUYẾT: Dùng GlobalScope.
        // Dù người dùng có vuốt tắt app thì luồng này vẫn sống thêm vài giây để hoàn thành việc Update Widget.
        GlobalScope.launch(Dispatchers.IO) {
            try {
                ExpenseWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}