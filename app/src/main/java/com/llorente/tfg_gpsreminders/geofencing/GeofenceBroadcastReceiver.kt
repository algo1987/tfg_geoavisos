package com.llorente.tfg_gpsreminders.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
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
        private const val PREFS_NAME = "geofence_receiver_state"
        private const val KEY_PREFIX_INSIDE = "inside_task_"
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
                val triggeringLocation = geofencingEvent.triggeringLocation

                val transitionText = when (transition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
                    Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
                    Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELL"
                    else -> "UNKNOWN($transition)"
                }

                Log.d(TAG, "Transition type: $transitionText")
                Log.d(TAG, "Triggering location: $triggeringLocation")

                val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()
                Log.d(TAG, "Geovallas activadas: ${triggeringGeofences.size}")

                val taskDao = AppDatabase.getDatabase(context.applicationContext).taskDao()
                val prefs = getPrefs(context.applicationContext)

                for (geofence in triggeringGeofences) {
                    val taskId = geofence.requestId.toIntOrNull() ?: continue
                    Log.d(TAG, "Procesando geovalla de taskId=$taskId")

                    val task = taskDao.getTaskById(taskId) ?: continue

                    when (transition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> {
                            val alreadyInside = isTaskMarkedAsInside(prefs, taskId)

                            if (alreadyInside) {
                                Log.d(TAG, "ENTER ignorado para taskId=$taskId porque ya estaba marcada como dentro")
                                continue
                            }

                            if (!task.isCompleted && task.isLocationReminderEnabled) {
                                val distanceToGeofenceCenter = calculateDistanceToTaskLocation(
                                    triggeringLocation = triggeringLocation,
                                    taskLatitude = task.latitude,
                                    taskLongitude = task.longitude
                                )

                                Log.d(
                                    TAG,
                                    "Mostrando notificación para taskId=${task.id}. Distancia al centro=$distanceToGeofenceCenter metros"
                                )

                                TaskNotificationHelper.showTaskNotification(
                                    context = context.applicationContext,
                                    task = task,
                                    distanceToGeofenceCenterMeters = distanceToGeofenceCenter
                                )

                                markTaskAsInside(prefs, taskId)
                            } else {
                                Log.d(TAG, "La tarea ya no es válida para notificación")
                                clearTaskInsideState(prefs, taskId)
                            }
                        }

                        Geofence.GEOFENCE_TRANSITION_EXIT -> {
                            Log.d(TAG, "Salida detectada para taskId=${task.id}. Se limpia estado interno.")
                            clearTaskInsideState(prefs, taskId)
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

    private fun calculateDistanceToTaskLocation(
        triggeringLocation: Location?,
        taskLatitude: Double?,
        taskLongitude: Double?
    ): Int? {
        if (triggeringLocation == null || taskLatitude == null || taskLongitude == null) {
            return null
        }

        val results = FloatArray(1)

        Location.distanceBetween(
            triggeringLocation.latitude,
            triggeringLocation.longitude,
            taskLatitude,
            taskLongitude,
            results
        )

        return results[0].toInt()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun isTaskMarkedAsInside(prefs: SharedPreferences, taskId: Int): Boolean {
        return prefs.getBoolean(KEY_PREFIX_INSIDE + taskId, false)
    }

    private fun markTaskAsInside(prefs: SharedPreferences, taskId: Int) {
        prefs.edit()
            .putBoolean(KEY_PREFIX_INSIDE + taskId, true)
            .apply()
    }

    private fun clearTaskInsideState(prefs: SharedPreferences, taskId: Int) {
        prefs.edit()
            .remove(KEY_PREFIX_INSIDE + taskId)
            .apply()
    }
}