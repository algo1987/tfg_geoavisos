package com.llorente.tfg_gpsreminders.ui

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.llorente.tfg_gpsreminders.MainActivity
import com.llorente.tfg_gpsreminders.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import com.llorente.tfg_gpsreminders.data.local.AppDatabase
import com.llorente.tfg_gpsreminders.data.local.TaskEntity
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Before
    fun clearAllTables() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getDatabase(context)
        database.clearAllTables()
    }

    private fun insertTaskDirectly(task: TaskEntity) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getDatabase(context)
        database.taskDao().insertTask(task)
    }

    @Test
    fun addTask_displaysTaskInList() {
        val uniqueTitle = "Comprar leche ${System.currentTimeMillis()}"

        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.fabAddTask)).perform(click())
        onView(withId(R.id.editTextTitle)).perform(replaceText(uniqueTitle), closeSoftKeyboard())
        onView(withId(R.id.editTextDescription)).perform(replaceText("Ir al supermercado"), closeSoftKeyboard())
        onView(withId(R.id.buttonSaveTask)).perform(click())

        onView(withText(uniqueTitle)).check(matches(isDisplayed()))
    }

    @Test
    fun editTask_updatesTaskInList() {
        val originalTitle = "Tarea original ${System.currentTimeMillis()}"
        val updatedTitle = "Tarea editada ${System.currentTimeMillis()}"

        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.fabAddTask)).perform(click())
        onView(withId(R.id.editTextTitle)).perform(replaceText(originalTitle), closeSoftKeyboard())
        onView(withId(R.id.editTextDescription)).perform(replaceText("Descripción original"), closeSoftKeyboard())
        onView(withId(R.id.buttonSaveTask)).perform(click())

        onView(withText(originalTitle)).perform(click())

        onView(withId(R.id.buttonEditTask)).perform(click())

        onView(withId(R.id.editTextTitle)).perform(replaceText(updatedTitle), closeSoftKeyboard())
        onView(withId(R.id.editTextDescription)).perform(replaceText("Descripción editada"), closeSoftKeyboard())

        onView(withId(R.id.buttonSaveTask)).perform(click())

        onView(withId(R.id.textViewTaskTitle)).check(matches(withText(updatedTitle)))

        pressBack()

        onView(withText(updatedTitle)).check(matches(isDisplayed()))
    }

    @Test
    fun deleteTask_removesTaskFromListAfterConfirmation() {
        val titleToDelete = "Tarea borrar ${System.currentTimeMillis()}"

        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.fabAddTask)).perform(click())
        onView(withId(R.id.editTextTitle)).perform(replaceText(titleToDelete), closeSoftKeyboard())
        onView(withId(R.id.editTextDescription)).perform(replaceText("Descripción borrar"), closeSoftKeyboard())
        onView(withId(R.id.buttonSaveTask)).perform(click())

        onView(withText(titleToDelete)).check(matches(isDisplayed()))

        onView(withText(titleToDelete)).perform(click())

        onView(withId(R.id.buttonDeleteTask)).perform(click())

        onView(withText("Eliminar")).perform(click())

        onView(withText(titleToDelete)).check(doesNotExist())
    }

    @Test
    fun openTaskDetail_showsPlaceAndLocation() {
        val title = "Tarea con ubicacion ${System.currentTimeMillis()}"

        insertTaskDirectly(
            TaskEntity(
                title = title,
                description = "Descripcion con ubicacion",
                latitude = 40.4015,
                longitude = -3.7026,
                locationName = "Alcampo",
                locationAddress = "P.º de la Esperanza, 51, Arganzuela, 28005 Madrid, Spain"
            )
        )

        ActivityScenario.launch(MainActivity::class.java)

        onView(withText(title)).perform(click())

        onView(withId(R.id.textViewTaskTitle)).check(matches(withText(title)))
        onView(withId(R.id.textViewTaskPlace)).check(matches(withText("Alcampo")))
        onView(withId(R.id.textViewTaskLocation)).check(
            matches(withText("P.º de la Esperanza, 51, Arganzuela, 28005 Madrid, Spain"))
        )
    }

    @Test
    fun editTask_keepsLocationData() {
        val title = "Tarea editar ubicacion ${System.currentTimeMillis()}"

        insertTaskDirectly(
            TaskEntity(
                title = title,
                description = "Descripcion inicial",
                latitude = 40.4015,
                longitude = -3.7026,
                locationName = "Alcampo",
                locationAddress = "P.º de la Esperanza, 51, Arganzuela, 28005 Madrid, Spain"
            )
        )

        ActivityScenario.launch(MainActivity::class.java)

        onView(withText(title)).perform(click())

        onView(withId(R.id.textViewTaskPlace)).check(matches(withText("Alcampo")))
        onView(withId(R.id.textViewTaskLocation)).check(
            matches(withText("P.º de la Esperanza, 51, Arganzuela, 28005 Madrid, Spain"))
        )

        onView(withId(R.id.buttonEditTask)).perform(click())

        onView(withId(R.id.textViewSelectedPlace)).check(matches(withText("Alcampo")))
        onView(withId(R.id.textViewSelectedLocation)).check(
            matches(withText("P.º de la Esperanza, 51, Arganzuela, 28005 Madrid, Spain"))
        )

        onView(withId(R.id.buttonSaveTask)).perform(click())

        onView(withId(R.id.textViewTaskPlace)).check(matches(withText("Alcampo")))
        onView(withId(R.id.textViewTaskLocation)).check(
            matches(withText("P.º de la Esperanza, 51, Arganzuela, 28005 Madrid, Spain"))
        )
    }

    @Test
    fun removeLocation_confirmDialog_cancel_keepsLocation() {
        val title = "Tarea cancelar quitar ubicacion ${System.currentTimeMillis()}"

        insertTaskDirectly(
            TaskEntity(
                title = title,
                description = "Descripcion",
                latitude = 40.4015,
                longitude = -3.7026,
                locationName = "Alcampo",
                locationAddress = "P.º de la Esperanza, 51, Arganzuela, 28005 Madrid, Spain"
            )
        )

        ActivityScenario.launch(MainActivity::class.java)

        onView(withText(title)).perform(click())
        onView(withId(R.id.buttonEditTask)).perform(click())

        onView(withId(R.id.textViewSelectedPlace)).check(matches(withText("Alcampo")))
        onView(withId(R.id.textViewSelectedLocation)).check(
            matches(withText("P.º de la Esperanza, 51, Arganzuela, 28005 Madrid, Spain"))
        )

        onView(withId(R.id.buttonRemoveLocation)).perform(click())

        onView(withText("Quitar ubicación")).check(matches(isDisplayed()))
        onView(withText("Cancelar")).perform(click())

        onView(withId(R.id.textViewSelectedPlace)).check(matches(withText("Alcampo")))
        onView(withId(R.id.textViewSelectedLocation)).check(
            matches(withText("P.º de la Esperanza, 51, Arganzuela, 28005 Madrid, Spain"))
        )
        onView(withId(R.id.buttonRemoveLocation)).check(matches(isDisplayed()))
    }

    @Test
    fun removeLocation_confirmDialog_accept_clearsLocation() {
        val title = "Tarea confirmar quitar ubicacion ${System.currentTimeMillis()}"

        insertTaskDirectly(
            TaskEntity(
                title = title,
                description = "Descripcion",
                latitude = 40.4015,
                longitude = -3.7026,
                locationName = "Alcampo",
                locationAddress = "P.º de la Esperanza, 51, Arganzuela, 28005 Madrid, Spain"
            )
        )

        ActivityScenario.launch(MainActivity::class.java)

        onView(withText(title)).perform(click())
        onView(withId(R.id.buttonEditTask)).perform(click())

        onView(withId(R.id.buttonRemoveLocation)).perform(click())

        onView(withText("Quitar")).perform(click())

        onView(withId(R.id.textViewSelectedPlace))
            .check(matches(withText("Sin lugar asociado")))

        onView(withId(R.id.textViewSelectedLocation))
            .check(matches(withText("Sin ubicación asociada")))

        onView(withId(R.id.buttonRemoveLocation))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun taskWithoutLocation_showsDefaultTexts() {
        val title = "Tarea sin ubicacion ${System.currentTimeMillis()}"

        insertTaskDirectly(
            TaskEntity(
                title = title,
                description = "Descripcion sin ubicacion"
            )
        )

        ActivityScenario.launch(MainActivity::class.java)

        onView(withText(title)).perform(click())

        onView(withId(R.id.textViewTaskPlace))
            .check(matches(withText("Sin lugar asociado")))

        onView(withId(R.id.textViewTaskLocation))
            .check(matches(withText("Sin ubicación asociada")))
    }

    @Test
    fun taskWithLocation_showsMapInDetail() {
        val title = "Tarea con mapa ${System.currentTimeMillis()}"

        insertTaskDirectly(
            TaskEntity(
                title = title,
                description = "Descripcion con ubicacion",
                latitude = 40.4015,
                longitude = -3.7026,
                locationName = "Alcampo",
                locationAddress = "P.º de la Esperanza, 51, Arganzuela, 28005 Madrid, Spain"
            )
        )

        ActivityScenario.launch(MainActivity::class.java)

        onView(withText(title)).perform(click())

        onView(withId(R.id.textViewTaskPlace))
            .check(matches(withText("Alcampo")))

        onView(withId(R.id.textViewTaskLocation))
            .check(matches(withText("P.º de la Esperanza, 51, Arganzuela, 28005 Madrid, Spain")))

        onView(withId(R.id.textLabelMap))
            .check(matches(isDisplayed()))

        onView(withId(R.id.mapTaskDetailContainer))
            .check(matches(isDisplayed()))
    }

    @Test
    fun taskWithoutLocation_hidesMapInDetail() {
        val title = "Tarea sin mapa ${System.currentTimeMillis()}"

        insertTaskDirectly(
            TaskEntity(
                title = title,
                description = "Descripcion sin ubicacion"
            )
        )

        ActivityScenario.launch(MainActivity::class.java)

        onView(withText(title)).perform(click())

        onView(withId(R.id.textViewTaskPlace))
            .check(matches(withText("Sin lugar asociado")))

        onView(withId(R.id.textViewTaskLocation))
            .check(matches(withText("Sin ubicación asociada")))

        onView(withId(R.id.textLabelMap))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(withId(R.id.mapTaskDetailContainer))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }
}