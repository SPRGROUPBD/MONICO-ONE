package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.MainActivity
import com.example.R
import com.example.data.model.NotificationItem
import java.util.concurrent.TimeUnit

object NotificationHelper {

    const val CHANNEL_ID = "monico_one_high_priority_channel"
    private const val CHANNEL_NAME = "Monico One জরুরি নোটিফিকেশন"
    private const val CHANNEL_DESC = "কোম্পানির নোটিশ, সেলস আপডেট, সার্কুলার ও জরুরি বার্তা"
    private const val PREFS_NAME = "monico_notification_cache"
    private const val KEY_NOTIFIED_IDS = "notified_ids"
    private const val WORK_NAME = "monico_notification_sync_worker"

    fun initNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 200, 250)
                    setSound(soundUri, audioAttributes)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if this notification was already shown to prevent duplicate sounds and alerts.
     */
    fun isAlreadyNotified(context: Context, notificationId: String): Boolean {
        if (notificationId.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notifiedSet = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()) ?: emptySet()
        return notifiedSet.contains(notificationId)
    }

    /**
     * Marks a notification as having played sound and shown on system tray.
     */
    fun markAsNotified(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notifiedSet = (prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()) ?: emptySet()).toMutableSet()
        notifiedSet.add(notificationId)
        prefs.edit().putStringSet(KEY_NOTIFIED_IDS, notifiedSet).apply()
    }

    /**
     * Shows a Heads-Up Popup System Notification with Ting Sound, Vibration, and Lock Screen Visibility.
     */
    fun showSystemNotification(
        context: Context,
        id: String,
        title: String,
        message: String,
        type: String = "general"
    ) {
        initNotificationChannel(context)

        // Duplicate control: do not play sound or spam if already alerted
        if (id.isNotBlank() && isAlreadyNotified(context, id)) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open app directly to notifications dialog / notification viewer
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_notifications", true)
            putExtra("notif_id", id)
            putExtra("notif_title", title)
            putExtra("notif_message", message)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.monico_one_icon_1791604475434)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX) // Popup / Heads-Up notification
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Lock screen visibility
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 200, 250))
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val numericId = if (id.isNotBlank()) Math.abs(id.hashCode()) else System.currentTimeMillis().toInt()
        notificationManager.notify(numericId, notification)

        if (id.isNotBlank()) {
            markAsNotified(context, id)
        }
    }

    /**
     * Checks a list of fetched notifications against current user role and triggers system notifications
     * for newly arrived unnotified items.
     */
    fun checkAndNotifyNewItems(
        context: Context,
        userRole: String,
        userEmpId: String,
        items: List<NotificationItem>
    ) {
        val cleanRole = userRole.uppercase().trim()
        val isSuperAdmin = cleanRole == "SUPER_ADMIN" || cleanRole == "ADMIN"

        items.forEach { notif ->
            val matchesRole = isSuperAdmin ||
                    notif.targetRole.equals("ALL", ignoreCase = true) ||
                    notif.targetRole.contains(cleanRole, ignoreCase = true) ||
                    notif.targetRole.contains("PALL", ignoreCase = true)

            if (matchesRole && !isAlreadyNotified(context, notif.id)) {
                // If it wasn't read yet by this user, show popup notification with sound
                val isReadByMe = notif.readBy[userEmpId.uppercase()] == true
                if (!isReadByMe) {
                    showSystemNotification(
                        context = context,
                        id = notif.id,
                        title = notif.title,
                        message = notif.message,
                        type = notif.type
                    )
                }
            }
        }
    }

    /**
     * Schedules periodic background sync using WorkManager so notifications arrive even when app is closed.
     */
    fun scheduleBackgroundSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<NotificationSyncWorker>(
            15, TimeUnit.MINUTES // Minimum interval allowed by Android WorkManager
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
