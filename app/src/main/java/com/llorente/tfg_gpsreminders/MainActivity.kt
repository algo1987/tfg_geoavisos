package com.llorente.tfg_gpsreminders

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.llorente.tfg_gpsreminders.ui.AddTaskActivity
import com.llorente.tfg_gpsreminders.ui.TaskAdapter
import com.llorente.tfg_gpsreminders.ui.TaskViewModel

class MainActivity : AppCompatActivity() {

    private val taskViewModel: TaskViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val recyclerViewTasks = findViewById<RecyclerView>(R.id.recyclerViewTasks)
        val fabAddTask = findViewById<FloatingActionButton>(R.id.fabAddTask)
        val textViewEmptyState = findViewById<TextView>(R.id.textViewEmptyState)

        setSupportActionBar(toolbar)

        taskAdapter = TaskAdapter(
            taskList = emptyList(),
            onTaskChecked = { updatedTask ->
                taskViewModel.updateTask(updatedTask)
            },
            onTaskDeleted = { task ->
                taskViewModel.deleteTask(task)
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
}