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

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Before
    fun clearDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("tasks_database")
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
}