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

            if (isTaskListEmpty) {
                textViewEmptyState.visibility = View.VISIBLE
                recyclerViewTasks.visibility = View.GONE
            } else {
                textViewEmptyState.visibility = View.GONE
                recyclerViewTasks.visibility = View.VISIBLE
            }
        }

        fabAddTask.setOnClickListener {
            openAddTaskScreen()
        }

        // Permite crear una tarea pulsando la pantalla solo cuando la lista está vacía.
        // Cuando ya hay tareas, la creación se mantiene en el botón +.
        textViewEmptyState.setOnClickListener {
            if (isTaskListEmpty) {
                openAddTaskScreen()
            }
        }
    }

    // Se sincronizan las geovallas al abrir la app para intentar mantenerlas registradas
    // en el sistema más tiempo.
    override fun onResume() {
        super.onResume()
        GeofenceSyncManager.syncAllGeofences(this)
    }

    private fun openAddTaskScreen() {
        startActivity(Intent(this, AddTaskActivity::class.java))
    }
}