package com.llorente.tfg_gpsreminders.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.llorente.tfg_gpsreminders.data.local.AppDatabase
import com.llorente.tfg_gpsreminders.notifications.TaskNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GEOFENCE_RECEIVER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() invocado")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geofencingEvent = GeofencingEvent.fromIntent(intent)

                if (geofencingEvent == null) {
                    Log.d(TAG, "GeofencingEvent es null")
                    return@launch
                }

                if (geofencingEvent.hasError()) {
                    Log.d(TAG, "GeofencingEvent con error: ${geofencingEvent.errorCode}")
                    return@launch
                }

                val transition = geofencingEvent.geofenceTransition

                val transitionText = when (transition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
                    Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
                    Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELL"
                    else -> "UNKNOWN($transition)"
                }

                Log.d(TAG, "Transition type: $transitionText")

                val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()
                Log.d(TAG, "Geovallas activadas: ${triggeringGeofences.size}")

                val taskDao = AppDatabase.getDatabase(context.applicationContext).taskDao()

                for (geofence in triggeringGeofences) {
                    val taskId = geofence.requestId.toIntOrNull() ?: continue
                    Log.d(TAG, "Procesando geovalla de taskId=$taskId")

                    val task = taskDao.getTaskById(taskId) ?: continue

                    when (transition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> {
                            if (!task.isCompleted && task.isLocationReminderEnabled) {
                                Log.d(TAG, "Mostrando notificación para taskId=${task.id}")
                                TaskNotificationHelper.showTaskNotification(context.applicationContext, task)
                            } else {
                                Log.d(TAG, "La tarea ya no es válida para notificación")
                            }
                        }

                        Geofence.GEOFENCE_TRANSITION_EXIT -> {
                            Log.d(TAG, "Salida detectada para taskId=${task.id}")
                        }

                        else -> {
                            Log.d(TAG, "Transición ignorada para taskId=${task.id}")
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}