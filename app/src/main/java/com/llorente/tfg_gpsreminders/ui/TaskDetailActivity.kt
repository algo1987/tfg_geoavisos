package com.llorente.tfg_gpsreminders.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.llorente.tfg_gpsreminders.R
import com.llorente.tfg_gpsreminders.data.local.TaskEntity
import com.llorente.tfg_gpsreminders.geofencing.GeofenceSyncManager
import com.llorente.tfg_gpsreminders.utils.LocationUtils
import kotlinx.coroutines.launch

class TaskDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private val taskViewModel: TaskViewModel by viewModels()

    private var currentTask: TaskEntity? = null
    private var taskId: Int = -1
    private var googleMap: GoogleMap? = null

    private lateinit var textViewTaskTitle: TextView
    private lateinit var textViewTaskDescription: TextView
    private lateinit var textViewTaskStatus: TextView
    private lateinit var textViewTaskPlace: TextView
    private lateinit var textViewTaskLocation: TextView
    private lateinit var textViewReminderStatus: TextView
    private lateinit var textViewReminderRadius: TextView
    private lateinit var textLabelMap: TextView
    private lateinit var mapTaskDetailContainer: View

    private var circle: Circle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarTaskDetail)
        textViewTaskTitle = findViewById(R.id.textViewTaskTitle)
        textViewTaskDescription = findViewById(R.id.textViewTaskDescription)
        textViewTaskStatus = findViewById(R.id.textViewTaskStatus)
        textViewTaskPlace = findViewById(R.id.textViewTaskPlace)
        textViewTaskLocation = findViewById(R.id.textViewTaskLocation)
        textViewReminderStatus = findViewById(R.id.textViewReminderStatus)
        textViewReminderRadius = findViewById(R.id.textViewReminderRadius)
        textLabelMap = findViewById(R.id.textLabelMap)
        mapTaskDetailContainer = findViewById(R.id.mapTaskDetailContainer)

        val buttonEditTask = findViewById<MaterialButton>(R.id.buttonEditTask)
        val buttonDeleteTask = findViewById<MaterialButton>(R.id.buttonDeleteTask)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        taskId = intent.getIntExtra("task_id", -1)

        if (taskId == -1) {
            Toast.makeText(this, R.string.toast_task_load_error, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapTaskDetailContainer) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadTask()

        buttonEditTask.setOnClickListener {
            currentTask?.let { task ->
                val intent = Intent(this, AddTaskActivity::class.java).apply {
                    putExtra("task_id", task.id)
                    putExtra("task_title", task.title)
                    putExtra("task_description", task.description)
                    putExtra("task_completed", task.isCompleted)

                    task.latitude?.let { putExtra("task_latitude", it) }
                    task.longitude?.let { putExtra("task_longitude", it) }
                    putExtra("task_location_name", task.locationName)
                    putExtra("task_location_address", task.locationAddress)
                    task.radius?.let { putExtra("task_radius", it) }

                    putExtra("task_location_reminder_enabled", task.isLocationReminderEnabled)
                }

                startActivity(intent)
            }
        }

        buttonDeleteTask.setOnClickListener {
            currentTask?.let { task ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_delete_task_title)
                    .setMessage(R.string.dialog_delete_task_message)
                    .setNegativeButton(R.string.button_cancel, null)
                    .setPositiveButton(R.string.button_delete_confirm) { _, _ ->
                        lifecycleScope.launch {
                            taskViewModel.deleteTask(task)
                            GeofenceSyncManager.syncAllGeofences(this@TaskDetailActivity)
                            Toast.makeText(this@TaskDetailActivity, R.string.toast_task_deleted, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (taskId != -1) {
            loadTask()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isZoomGesturesEnabled = true
        googleMap?.uiSettings?.isScrollGesturesEnabled = true
        googleMap?.uiSettings?.isRotateGesturesEnabled = true
        googleMap?.uiSettings?.isTiltGesturesEnabled = true
        googleMap?.uiSettings?.isMapToolbarEnabled = true

        currentTask?.let { renderMapIfNeeded(it) }
    }

    private fun loadTask() {
        lifecycleScope.launch {
            val task = taskViewModel.getTaskById(taskId)

            if (task == null) {
                Toast.makeText(
                    this@TaskDetailActivity,
                    getString(R.string.toast_task_not_found),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                currentTask = task
                renderTask(task)
            }
        }
    }

    private fun renderTask(task: TaskEntity) {
        textViewTaskTitle.text = task.title
        textViewTaskDescription.text = task.description ?: getString(R.string.no_description)
        textViewTaskStatus.text = if (task.isCompleted) getString(R.string.status_completed) else getString(R.string.status_pending)

        textViewTaskPlace.text = LocationUtils.buildLocationLabel(
            placeName = task.locationName,
            address = null,
            latitude = task.latitude,
            longitude = task.longitude,
            emptyText = getString(R.string.no_place_selected)
        )

        textViewTaskLocation.text = when {
            !task.locationAddress.isNullOrBlank() -> task.locationAddress
            task.latitude != null && task.longitude != null ->
                LocationUtils.formatCoordinates(task.latitude, task.longitude)
            else -> getString(R.string.no_location_selected)
        }

        if (task.isLocationReminderEnabled) {
            textViewReminderStatus.text = getString(R.string.status_enabled)
            textViewReminderRadius.text = "${(task.radius ?: 150f).toInt()} metros"
        } else {
            textViewReminderStatus.text = getString(R.string.status_disabled)
            textViewReminderRadius.text = getString(R.string.not_configured)
        }

        renderMapIfNeeded(task)
    }

    private fun renderMapIfNeeded(task: TaskEntity) {
        val hasCoordinates = task.latitude != null && task.longitude != null

        if (!hasCoordinates) {
            textLabelMap.visibility = View.GONE
            mapTaskDetailContainer.visibility = View.GONE
            return
        }

        textLabelMap.visibility = View.VISIBLE
        mapTaskDetailContainer.visibility = View.VISIBLE

        val latLng = LatLng(task.latitude!!, task.longitude!!)

        val radius = task.radius ?: 150f

        googleMap?.clear()
        googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(LocationUtils.buildLocationLabel(task.locationName, null, task.latitude, task.longitude, "Ubicación"))
        )

        circle = googleMap?.addCircle(
            CircleOptions()
                .center(latLng)
                .radius(radius.toDouble())
                .strokeWidth(2f)
                .strokeColor(Color.parseColor("#4285F4"))
                .fillColor(Color.parseColor("#334285F4"))
        )

        val zoom = if (radius > 300) 14f else 16f
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }
}