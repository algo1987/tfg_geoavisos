package com.llorente.tfg_gpsreminders.geofencing

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.llorente.tfg_gpsreminders.data.local.AppDatabase
import com.llorente.tfg_gpsreminders.data.local.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GeofenceSyncManager {

    private const val TAG = "GEOFENCE_SYNC"
    private const val DEFAULT_RADIUS = 150f
    private const val NOTIFICATION_RESPONSIVENESS_MS = 10000

    fun syncAllGeofences(context: Context) {
        val appContext = context.applicationContext

        Log.d(TAG, "---- syncAllGeofences() ----")

        if (!hasForegroundLocationPermission(appContext)) {
            Log.d(TAG, "No hay permiso ACCESS_FINE_LOCATION. Se cancela el registro.")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission(appContext)) {
            Log.d(TAG, "No hay permiso ACCESS_BACKGROUND_LOCATION. Se cancela el registro.")
            return
        }

        val geofencingClient = LocationServices.getGeofencingClient(appContext)
        val pendingIntent = getGeofencePendingIntent(appContext)

        geofencingClient.removeGeofences(pendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Geovallas previas eliminadas correctamente.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al eliminar geovallas previas: ${e.message}", e)
            }
            .addOnCompleteListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val taskDao = AppDatabase.getDatabase(appContext).taskDao()
                    val allTasks = taskDao.getAllTasksList()

                    Log.d(TAG, "Total tareas en BD: ${allTasks.size}")

                    allTasks.forEach { task ->
                        Log.d(
                            TAG,
                            "Tarea id=${task.id}, title=${task.title}, enabled=${task.isLocationReminderEnabled}, " +
                                    "completed=${task.isCompleted}, lat=${task.latitude}, lon=${task.longitude}, radius=${task.radius}"
                        )
                    }

                    val tasksWithActiveReminder = allTasks.filter { task ->
                        task.isLocationReminderEnabled &&
                                !task.isCompleted &&
                                task.latitude != null &&
                                task.longitude != null
                    }

                    Log.d(TAG, "Tareas válidas para geovalla: ${tasksWithActiveReminder.size}")

                    if (tasksWithActiveReminder.isEmpty()) {
                        Log.d(TAG, "No hay tareas válidas. No se registran geovallas.")
                        return@launch
                    }

                    val geofenceList = tasksWithActiveReminder.map { task ->
                        val geofence = buildGeofence(task)
                        Log.d(
                            TAG,
                            "Preparando geovalla taskId=${task.id}, lat=${task.latitude}, lon=${task.longitude}, radius=${task.radius ?: DEFAULT_RADIUS}"
                        )
                        geofence
                    }

                    val geofencingRequest = GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                        .addGeofences(geofenceList)
                        .build()

                    try {
                        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                            .addOnSuccessListener {
                                Log.d(TAG, "Geovallas registradas correctamente. Total=${geofenceList.size}")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error al registrar geovallas: ${e.message}", e)
                            }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException al registrar geovallas: ${e.message}", e)
                    }
                }
            }
    }

    private fun buildGeofence(task: TaskEntity): Geofence {
        return Geofence.Builder()
            .setRequestId(task.id.toString())
            .setCircularRegion(
                task.latitude!!,
                task.longitude!!,
                task.radius ?: DEFAULT_RADIUS
            )
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setNotificationResponsiveness(NOTIFICATION_RESPONSIVENESS_MS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()
    }

    private fun getGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)

        return PendingIntent.getBroadcast(
            context,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun hasForegroundLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}