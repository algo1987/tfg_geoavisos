package com.llorente.tfg_gpsreminders.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.llorente.tfg_gpsreminders.R
import com.llorente.tfg_gpsreminders.data.local.TaskEntity
import kotlinx.coroutines.launch
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TaskDetailActivity : AppCompatActivity() {

    private val taskViewModel: TaskViewModel by viewModels()

    private var currentTask: TaskEntity? = null
    private var taskId: Int = -1

    private lateinit var textViewTaskTitle: TextView
    private lateinit var textViewTaskDescription: TextView
    private lateinit var textViewTaskStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarTaskDetail)
        textViewTaskTitle = findViewById(R.id.textViewTaskTitle)
        textViewTaskDescription = findViewById(R.id.textViewTaskDescription)
        textViewTaskStatus = findViewById(R.id.textViewTaskStatus)
        val buttonEditTask = findViewById<MaterialButton>(R.id.buttonEditTask)
        val buttonDeleteTask = findViewById<MaterialButton>(R.id.buttonDeleteTask)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        taskId = intent.getIntExtra("task_id", -1)

        if (taskId == -1) {
            Toast.makeText(this, "No se pudo cargar la tarea", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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
                    task.radius?.let { putExtra("task_radius", it) }

                    putExtra("task_location_reminder_enabled", task.isLocationReminderEnabled)
                }

                startActivity(intent)
            }
        }

        buttonDeleteTask.setOnClickListener {
            currentTask?.let { task ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Eliminar tarea")
                    .setMessage("¿Seguro que quieres eliminar esta tarea?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Eliminar") { _, _ ->
                        taskViewModel.deleteTask(task)
                        Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                        finish()
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

    private fun loadTask() {
        lifecycleScope.launch {
            val task = taskViewModel.getTaskById(taskId)

            if (task == null) {
                Toast.makeText(this@TaskDetailActivity, "La tarea no existe", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                currentTask = task
                renderTask(task)
            }
        }
    }

    private fun renderTask(task: TaskEntity) {
        textViewTaskTitle.text = task.title
        textViewTaskDescription.text = task.description ?: "Sin descripción"
        textViewTaskStatus.text = if (task.isCompleted) "Completada" else "Pendiente"
    }
}