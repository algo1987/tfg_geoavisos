package com.llorente.tfg_gpsreminders.data

import androidx.lifecycle.LiveData
import com.llorente.tfg_gpsreminders.data.local.TaskDao
import com.llorente.tfg_gpsreminders.data.local.TaskEntity

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: LiveData<List<TaskEntity>> = taskDao.getAllTasks()

    suspend fun insertTask(task: TaskEntity) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun getTaskById(taskId: Int): TaskEntity? {
        return taskDao.getTaskById(taskId)
    }
}