package com.llorente.tfg_gpsreminders

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.llorente.tfg_gpsreminders.geofencing.GeofenceSyncManager
import com.llorente.tfg_gpsreminders.notifications.TaskNotificationHelper
import com.llorente.tfg_gpsreminders.ui.AddTaskActivity
import com.llorente.tfg_gpsreminders.ui.TaskAdapter
import com.llorente.tfg_gpsreminders.ui.TaskDetailActivity
import com.llorente.tfg_gpsreminders.ui.TaskViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val taskViewModel: TaskViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter

    private var isTaskListEmpty: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        TaskNotificationHelper.createNotificationChannel(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val recyclerViewTasks = findViewById<RecyclerView>(R.id.recyclerViewTasks)
        val fabAddTask = findViewById<FloatingActionButton>(R.id.fabAddTask)
        val textViewEmptyState = findViewById<TextView>(R.id.textViewEmptyState)

        setSupportActionBar(toolbar)

        taskAdapter = TaskAdapter(
            taskList = emptyList(),
            onTaskChecked = { updatedTask ->
                lifecycleScope.launch {
                    taskViewModel.updateTask(updatedTask)
                    GeofenceSyncManager.syncAllGeofences(this@MainActivity)
                }
            },
            onTaskDeleted = { task ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.button_delete_task))
                    .setMessage(R.string.dialog_delete_task_message)
                    .setNegativeButton(R.string.button_cancel, null)
                    .setPositiveButton(R.string.button_delete_confirm) { _, _ ->
                        lifecycleScope.launch {
                            taskViewModel.deleteTask(task)
                            GeofenceSyncManager.syncAllGeofences(this@MainActivity)
                        }
                    }
                    .show()
            },
            onTaskClicked = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java)
                intent.putExtra("task_id", task.id)
                startActivity(intent)
            }
        )

        recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }

        taskViewModel.allTasks.observe(this) { tasks ->
            isTaskListEmpty = tasks.isEmpty()

            taskAdapter.updateTasks(tasks)
            textViewEmptyState.visibility = if (isTaskListEmpty) View.VISIBLE else View.GONE
        }

        fabAddTask.setOnClickListener {
            openAddTaskScreen()
        }

        // Se va a permitir que el usuario pueda crear una tarea pulsando, tanto la pantalla como el boton +,
        // solo cuando la lista está vacía,para mejorar la experiencia inicial del usuario.
        // Cuando la lista no esté vacía, las nuevas tareas solo se podran crear desde el boton +.
        textViewEmptyState.setOnClickListener {
            if (isTaskListEmpty) {
                openAddTaskScreen()
            }
        }
    }

    // Se van a sincronizar las geovallas al abrir la app para intentar evitar que el sistema
    // las deje en segundo plano y conseguir que se mantengan activas
    override fun onResume() {
        super.onResume()
        GeofenceSyncManager.syncAllGeofences(this)
    }

    private fun openAddTaskScreen() {
        startActivity(Intent(this, AddTaskActivity::class.java))
    }
}