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
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_ID = "monico_one_high_priority_channel"
    private const val CHANNEL_NAME = "Monico One জরুরি নোটিফিকেশন"
    private const val CHANNEL_DESC = "কোম্পানির নোটিশ, সেলস আপডেট ও ফাইল নোটিফিকেশন"
    private const val PREFS_NAME = "monico_notification_cache"
    private const val KEY_NOTIFIED_IDS = "notified_ids"

    fun initNotificationChannel(context: Context) {
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
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks if this notification was already shown to prevent duplicate sounds.
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
     * Shows a Heads-Up Popup System Notification with Ting Sound and Lock Screen Visibility.
     */
    fun showSystemNotification(
        context: Context,
        id: String,
        title: String,
        message: String,
        type: String = "general"
    ) {
        initNotificationChannel(context)

        // Duplicate control
        if (id.isNotBlank() && isAlreadyNotified(context, id)) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open app directly to notifications
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_notifications", true)
            putExtra("notif_id", id)
            putExtra("notif_title", title)
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
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val numericId = if (id.isNotBlank()) Math.abs(id.hashCode()) else System.currentTimeMillis().toInt()
        notificationManager.notify(numericId, notification)

        if (id.isNotBlank()) {
            markAsNotified(context, id)
        }
    }
}
