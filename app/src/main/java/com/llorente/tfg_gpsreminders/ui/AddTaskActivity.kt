package com.llorente.tfg_gpsreminders.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
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
    private var taskRadius: Float? = null
    private var taskLocationReminderEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarAddTask)
        val textInputLayoutTitle = findViewById<TextInputLayout>(R.id.textInputLayoutTitle)
        val editTextTitle = findViewById<TextInputEditText>(R.id.editTextTitle)
        val editTextDescription = findViewById<TextInputEditText>(R.id.editTextDescription)
        val buttonSaveTask = findViewById<MaterialButton>(R.id.buttonSaveTask)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        readIntentData()

        if (isEditMode) {
            toolbar.title = "Editar tarea"
            buttonSaveTask.text = "Guardar cambios"

            editTextTitle.setText(intent.getStringExtra("task_title").orEmpty())
            editTextDescription.setText(intent.getStringExtra("task_description").orEmpty())
        } else {
            toolbar.title = "Nueva tarea"
            buttonSaveTask.text = "Guardar tarea"
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
                    radius = taskRadius,
                    isLocationReminderEnabled = taskLocationReminderEnabled
                )

                taskViewModel.updateTask(updatedTask)
                Toast.makeText(this, "Tarea actualizada", Toast.LENGTH_SHORT).show()
            } else {
                val newTask = TaskEntity(
                    title = title,
                    description = if (description.isEmpty()) null else description
                )

                taskViewModel.insertTask(newTask)
                Toast.makeText(this, "Tarea guardada", Toast.LENGTH_SHORT).show()
            }

            finish()
        }
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

        if (intent.hasExtra("task_radius")) {
            taskRadius = intent.getFloatExtra("task_radius", 0f)
        }

        taskLocationReminderEnabled =
            intent.getBooleanExtra("task_location_reminder_enabled", false)
    }
}