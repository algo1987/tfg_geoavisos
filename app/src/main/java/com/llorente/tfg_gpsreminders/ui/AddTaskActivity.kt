package com.llorente.tfg_gpsreminders.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.llorente.tfg_gpsreminders.R
import com.llorente.tfg_gpsreminders.data.local.TaskEntity

class AddTaskActivity : AppCompatActivity() {

    private val taskViewModel: TaskViewModel by viewModels()

    private var isEditMode = false
    private var taskId: Int = -1
    private var taskCompleted: Boolean = false
    private var taskLatitude: Double? = null
    private var taskLongitude: Double? = null
    private var taskLocationName: String? = null
    private var taskLocationAddress: String? = null
    private var taskRadius: Float? = null
    private var taskLocationReminderEnabled: Boolean = false

    private lateinit var textViewSelectedPlace: TextView
    private lateinit var textViewSelectedLocation: TextView
    private lateinit var buttonRemoveLocation: MaterialButton

    private val selectLocationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK || result.data == null) {
                return@registerForActivityResult
            }

            taskLatitude = result.data?.getDoubleExtra("selected_latitude", 0.0)
            taskLongitude = result.data?.getDoubleExtra("selected_longitude", 0.0)
            taskLocationName = result.data?.getStringExtra("selected_place_name")
            taskLocationAddress = result.data?.getStringExtra("selected_address")

            updateLocationUI()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarAddTask)
        val textInputLayoutTitle = findViewById<TextInputLayout>(R.id.textInputLayoutTitle)
        val editTextTitle = findViewById<TextInputEditText>(R.id.editTextTitle)
        val editTextDescription = findViewById<TextInputEditText>(R.id.editTextDescription)
        val buttonSelectLocation = findViewById<MaterialButton>(R.id.buttonSelectLocation)
        buttonRemoveLocation = findViewById(R.id.buttonRemoveLocation)
        val buttonSaveTask = findViewById<MaterialButton>(R.id.buttonSaveTask)

        textViewSelectedPlace = findViewById(R.id.textViewSelectedPlace)
        textViewSelectedLocation = findViewById(R.id.textViewSelectedLocation)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        readIntentData()
        updateLocationUI()

        if (isEditMode) {
            toolbar.title = "Editar tarea"
            buttonSaveTask.text = "Guardar cambios"

            editTextTitle.setText(intent.getStringExtra("task_title").orEmpty())
            editTextDescription.setText(intent.getStringExtra("task_description").orEmpty())
        } else {
            toolbar.title = "Nueva tarea"
            buttonSaveTask.text = "Guardar tarea"
        }

        buttonSelectLocation.setOnClickListener {
            openSelectLocationScreen()
        }

        buttonRemoveLocation.setOnClickListener {
            showRemoveLocationConfirmationDialog()
        }

        buttonSaveTask.setOnClickListener {
            val title = editTextTitle.text?.toString()?.trim().orEmpty()
            val description = editTextDescription.text?.toString()?.trim().orEmpty()

            if (title.isEmpty()) {
                textInputLayoutTitle.error = "El título es obligatorio"
                return@setOnClickListener
            }

            textInputLayoutTitle.error = null

            if (isEditMode) {
                val updatedTask = TaskEntity(
                    id = taskId,
                    title = title,
                    description = if (description.isEmpty()) null else description,
                    isCompleted = taskCompleted,
                    latitude = taskLatitude,
                    longitude = taskLongitude,
                    locationName = taskLocationName,
                    locationAddress = taskLocationAddress,
                    radius = taskRadius,
                    isLocationReminderEnabled = taskLocationReminderEnabled
                )

                taskViewModel.updateTask(updatedTask)
                Toast.makeText(this, "Tarea actualizada", Toast.LENGTH_SHORT).show()
            } else {
                val newTask = TaskEntity(
                    title = title,
                    description = if (description.isEmpty()) null else description,
                    latitude = taskLatitude,
                    longitude = taskLongitude,
                    locationName = taskLocationName,
                    locationAddress = taskLocationAddress
                )

                taskViewModel.insertTask(newTask)
                Toast.makeText(this, "Tarea guardada", Toast.LENGTH_SHORT).show()
            }

            finish()
        }
    }

    private fun openSelectLocationScreen() {
        val intent = Intent(this, SelectLocationActivity::class.java).apply {
            taskLatitude?.let { putExtra("task_latitude", it) }
            taskLongitude?.let { putExtra("task_longitude", it) }
            putExtra("task_location_name", taskLocationName)
            putExtra("task_location_address", taskLocationAddress)
        }

        selectLocationLauncher.launch(intent)
    }

    private fun readIntentData() {
        taskId = intent.getIntExtra("task_id", -1)
        isEditMode = taskId != -1

        taskCompleted = intent.getBooleanExtra("task_completed", false)

        if (intent.hasExtra("task_latitude")) {
            taskLatitude = intent.getDoubleExtra("task_latitude", 0.0)
        }

        if (intent.hasExtra("task_longitude")) {
            taskLongitude = intent.getDoubleExtra("task_longitude", 0.0)
        }

        taskLocationName = intent.getStringExtra("task_location_name")
        taskLocationAddress = intent.getStringExtra("task_location_address")

        if (intent.hasExtra("task_radius")) {
            taskRadius = intent.getFloatExtra("task_radius", 0f)
        }

        taskLocationReminderEnabled =
            intent.getBooleanExtra("task_location_reminder_enabled", false)
    }

    private fun updateLocationUI() {
        val hasLocation = taskLatitude != null && taskLongitude != null

        if (!hasLocation) {
            textViewSelectedPlace.text = "Sin lugar asociado"
            textViewSelectedLocation.text = "Sin ubicación asociada"
            buttonRemoveLocation.visibility = View.GONE
            return
        }

        textViewSelectedPlace.text = buildPlaceText()
        textViewSelectedLocation.text = buildLocationText()
        buttonRemoveLocation.visibility = View.VISIBLE
    }

    private fun buildPlaceText(): String {
        return if (!taskLocationName.isNullOrBlank()) {
            taskLocationName!!
        } else {
            "Sin lugar asociado"
        }
    }

    private fun buildLocationText(): String {
        if (!taskLocationAddress.isNullOrBlank()) {
            return taskLocationAddress!!
        }

        val latitudeText = taskLatitude?.toString() ?: "-"
        val longitudeText = taskLongitude?.toString() ?: "-"

        return "Latitud: $latitudeText\nLongitud: $longitudeText"
    }

    private fun showRemoveLocationConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Quitar ubicación")
            .setMessage("¿Seguro que quieres eliminar la ubicación asociada a esta tarea?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Quitar") { _, _ ->
                clearLocation()
            }
            .show()
    }

    private fun clearLocation() {
        taskLatitude = null
        taskLongitude = null
        taskLocationName = null
        taskLocationAddress = null
        taskRadius = null
        taskLocationReminderEnabled = false

        updateLocationUI()

        Toast.makeText(this, "Ubicación eliminada", Toast.LENGTH_SHORT).show()
    }
}