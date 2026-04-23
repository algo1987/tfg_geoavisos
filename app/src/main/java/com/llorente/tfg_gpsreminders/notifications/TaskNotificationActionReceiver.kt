package com.llorente.tfg_gpsreminders.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.llorente.tfg_gpsreminders.data.local.AppDatabase
import com.llorente.tfg_gpsreminders.geofencing.GeofenceSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TaskNotificationHelper.ACTION_COMPLETE_TASK_FROM_NOTIFICATION) {
            return
        }

        val taskId = intent.getIntExtra(TaskNotificationHelper.EXTRA_TASK_ID, -1)
        if (taskId == -1) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val taskDao = AppDatabase.getDatabase(appContext).taskDao()
                val task = taskDao.getTaskById(taskId) ?: return@launch

                if (!task.isCompleted) {
                    val updatedTask = task.copy(
                        isCompleted = true,
                        isLocationReminderEnabled = false
                    )

                    taskDao.updateTask(updatedTask)
                    GeofenceSyncManager.syncAllGeofences(appContext)
                }

                TaskNotificationHelper.cancelTaskNotification(appContext, taskId)

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        appContext,
                        "Tarea marcada como completada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}