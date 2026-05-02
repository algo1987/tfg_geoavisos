package com.llorente.tfg_gpsreminders.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.llorente.tfg_gpsreminders.R
import com.llorente.tfg_gpsreminders.data.local.TaskEntity
import com.llorente.tfg_gpsreminders.ui.TaskDetailActivity
import com.llorente.tfg_gpsreminders.utils.LocationUtils

object TaskNotificationHelper {

    private const val CHANNEL_ID = "task_location_reminders_channel"

    const val ACTION_COMPLETE_TASK_FROM_NOTIFICATION =
        "com.llorente.tfg_gpsreminders.ACTION_COMPLETE_TASK_FROM_NOTIFICATION"

    const val EXTRA_TASK_ID = "extra_task_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showTaskNotification(
        context: Context,
        task: TaskEntity,
        distanceToGeofenceCenterMeters: Int? = null
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        val openTaskIntent = Intent(context, TaskDetailActivity::class.java).apply {
            putExtra("task_id", task.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openTaskPendingIntent = PendingIntent.getActivity(
            context,
            task.id,
            openTaskIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(
            context,
            TaskNotificationActionReceiver::class.java
        ).apply {
            action = ACTION_COMPLETE_TASK_FROM_NOTIFICATION
            putExtra(EXTRA_TASK_ID, task.id)
        }

        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id + 10000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val locationText = getLocationText(context, task)

        val notificationTitle = context.getString(R.string.notification_title_task_nearby)
        val shortContentText = "${task.title} · 📍 $locationText"
        val expandedContentText = buildExpandedContentText(
            taskTitle = task.title,
            locationText = locationText
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(notificationTitle)
            .setContentText(shortContentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedContentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openTaskPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                context.getString(R.string.notification_action_complete),
                completePendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(task.id, notification)
    }

    fun cancelTaskNotification(context: Context, taskId: Int) {
        NotificationManagerCompat.from(context).cancel(taskId)
    }

    private fun buildExpandedContentText(
        taskTitle: String,
        locationText: String
    ): String {
        return """
            📋 $taskTitle
            📍 $locationText
        """.trimIndent()
    }

    private fun getLocationText(context: Context, task: TaskEntity): String {
        return LocationUtils.buildLocationLabel(
            placeName = task.locationName,
            address = task.locationAddress,
            latitude = task.latitude,
            longitude = task.longitude,
            emptyText = context.getString(R.string.zone_configured)
        )
    }
}