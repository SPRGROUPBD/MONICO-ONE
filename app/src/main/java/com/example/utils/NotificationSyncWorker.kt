package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.remote.FirebaseRtdbService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs: SharedPreferences = applicationContext.getSharedPreferences("monico_one_prefs", Context.MODE_PRIVATE)
            val empId = prefs.getString("user_emp_id", null) ?: return@withContext Result.success()
            val role = prefs.getString("user_role", "MPO") ?: "MPO"

            val service = FirebaseRtdbService(
                getCustomUrl = { prefs.getString("custom_rtdb_url", null) }
            )
            val notifications = service.fetchNotifications()

            if (notifications.isNotEmpty()) {
                NotificationHelper.checkAndNotifyNewItems(
                    context = applicationContext,
                    userRole = role,
                    userEmpId = empId,
                    items = notifications
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
