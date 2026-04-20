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
                    .setTitle("Eliminar tarea")
                    .setMessage("¿Seguro que quieres eliminar esta tarea?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Eliminar") { _, _ ->
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
            taskAdapter.updateTasks(tasks)
            textViewEmptyState.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
        }

        fabAddTask.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
    }

//    override fun onResume() {
//        super.onResume()
//        GeofenceSyncManager.syncAllGeofences(this)
//    }
}