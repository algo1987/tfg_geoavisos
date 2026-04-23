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

object TaskNotificationHelper {

    private const val CHANNEL_ID = "task_location_reminders_channel"
    private const val CHANNEL_NAME = "Recordatorios por ubicación"
    private const val CHANNEL_DESCRIPTION = "Notificaciones al entrar en una zona asociada a una tarea"

    const val ACTION_COMPLETE_TASK_FROM_NOTIFICATION =
        "com.llorente.tfg_gpsreminders.ACTION_COMPLETE_TASK_FROM_NOTIFICATION"

    const val EXTRA_TASK_ID = "extra_task_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showTaskNotification(context: Context, task: TaskEntity) {
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

        val locationText = when {
            !task.locationName.isNullOrBlank() -> task.locationName
            !task.locationAddress.isNullOrBlank() -> task.locationAddress
            else -> "zona configurada"
        }

        val title = "Tarea '${task.title}' pendiente cerca. Lugar: $locationText"
        val contentText = "Tarea '${task.title}' cerca de tu ubicación"
        val bigText = buildBigText(task)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openTaskPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Marcar como completada",
                completePendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(task.id, notification)
    }

    fun cancelTaskNotification(context: Context, taskId: Int) {
        NotificationManagerCompat.from(context).cancel(taskId)
    }

    private fun buildBigText(task: TaskEntity): String {
        val descriptionPart = if (!task.description.isNullOrBlank()) {
            "Descripción: ${task.description}"
        } else {
            "Descripción: (sin descripción)"
        }

        val locationPart = when {
            !task.locationAddress.isNullOrBlank() -> "Ubicación: ${task.locationAddress}"
            task.latitude != null && task.longitude != null ->
                "Ubicación: Lat ${task.latitude}, Lon ${task.longitude}"
            else -> "Ubicación: no disponible"
        }

        return """
            Tienes pendiente la tarea '${task.title}' cerca de donde te encuentras.
            
            $descriptionPart
            
            $locationPart
        """.trimIndent()
    }
}