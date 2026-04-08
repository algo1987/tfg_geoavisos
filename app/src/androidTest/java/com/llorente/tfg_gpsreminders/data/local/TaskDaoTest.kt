package com.llorente.tfg_gpsreminders.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertTask_savesTaskCorrectly() = runBlocking {
        val task = TaskEntity(
            title = "Comprar pan",
            description = "Ir a la panadería"
        )

        taskDao.insertTask(task)
        val tasks = taskDao.getAllTasksList()

        assertEquals(1, tasks.size)
        assertEquals("Comprar pan", tasks[0].title)
        assertEquals("Ir a la panadería", tasks[0].description)
        assertFalse(tasks[0].isCompleted)
    }

    @Test
    fun updateTask_changesCompletedStatus() = runBlocking {
        val task = TaskEntity(
            title = "Hacer recado",
            description = "Pasar por la farmacia"
        )

        taskDao.insertTask(task)
        val insertedTask = taskDao.getAllTasksList().first()

        val updatedTask = insertedTask.copy(isCompleted = true)
        taskDao.updateTask(updatedTask)

        val tasks = taskDao.getAllTasksList()

        assertEquals(1, tasks.size)
        assertTrue(tasks[0].isCompleted)
    }

    @Test
    fun deleteTask_removesTaskCorrectly() = runBlocking {
        val task = TaskEntity(
            title = "Llamar al banco",
            description = "Consultar recibo"
        )

        taskDao.insertTask(task)
        val insertedTask = taskDao.getAllTasksList().first()

        taskDao.deleteTask(insertedTask)
        val tasks = taskDao.getAllTasksList()

        assertTrue(tasks.isEmpty())
    }

    // Test para comprobar orden
    @Test
    fun getAllTasks_returnsTasksOrderedByIdDesc() = runBlocking {
        val task1 = TaskEntity(title = "Tarea 1")
        val task2 = TaskEntity(title = "Tarea 2")

        taskDao.insertTask(task1)
        taskDao.insertTask(task2)

        val tasks = taskDao.getAllTasksList()

        assertEquals(2, tasks.size)
        assertEquals("Tarea 2", tasks[0].title) // la más reciente primero
        assertEquals("Tarea 1", tasks[1].title)
    }


    // Test de campo opcional
    @Test
    fun insertTask_withNullDescription_worksCorrectly() = runBlocking {
        val task = TaskEntity(title = "Sin descripción")

        taskDao.insertTask(task)
        val tasks = taskDao.getAllTasksList()

        assertEquals(1, tasks.size)
        assertEquals(null, tasks[0].description)
    }

    // Test de lista vacía
    @Test
    fun getAllTasks_whenEmpty_returnsEmptyList() = runBlocking {
        val tasks = taskDao.getAllTasksList()
        assertTrue(tasks.isEmpty())
    }
}