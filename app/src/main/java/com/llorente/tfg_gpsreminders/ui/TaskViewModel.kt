package com.llorente.tfg_gpsreminders.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.llorente.tfg_gpsreminders.data.TaskRepository
import com.llorente.tfg_gpsreminders.data.local.AppDatabase
import com.llorente.tfg_gpsreminders.data.local.TaskEntity

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    val allTasks: LiveData<List<TaskEntity>>

    init {
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)
        allTasks = repository.allTasks
    }

    suspend fun insertTask(task: TaskEntity) {
        repository.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        repository.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        repository.deleteTask(task)
    }

    suspend fun getTaskById(taskId: Int): TaskEntity? {
        return repository.getTaskById(taskId)
    }

    suspend fun getAllTasksList(): List<TaskEntity> {
        return repository.getAllTasksList()
    }
}