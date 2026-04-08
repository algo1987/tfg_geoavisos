package com.llorente.tfg_gpsreminders.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.llorente.tfg_gpsreminders.MainActivity
import com.llorente.tfg_gpsreminders.R
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.containsString

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Test
    fun addTask_displaysTaskInList() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.fabAddTask)).perform(click())

        onView(withId(R.id.editTextTitle)).perform(typeText("Comprar leche"))
        onView(withId(R.id.editTextDescription)).perform(typeText("Ir al supermercado"), closeSoftKeyboard())

        onView(withId(R.id.buttonSaveTask)).perform(click())

        onView(withId(R.id.textViewTitle)).check(matches(withText(containsString("Comprar leche"))))
    }
}