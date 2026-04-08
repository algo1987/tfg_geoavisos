package com.llorente.tfg_gpsreminders.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.llorente.tfg_gpsreminders.R
import com.llorente.tfg_gpsreminders.data.local.TaskEntity

class AddTaskActivity : AppCompatActivity() {

    private val taskViewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarAddTask)
        val textInputLayoutTitle = findViewById<TextInputLayout>(R.id.textInputLayoutTitle)
        val editTextTitle = findViewById<TextInputEditText>(R.id.editTextTitle)
        val editTextDescription = findViewById<TextInputEditText>(R.id.editTextDescription)
        val buttonSaveTask = findViewById<MaterialButton>(R.id.buttonSaveTask)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        buttonSaveTask.setOnClickListener {
            val title = editTextTitle.text?.toString()?.trim().orEmpty()
            val description = editTextDescription.text?.toString()?.trim().orEmpty()

            if (title.isEmpty()) {
                textInputLayoutTitle.error = "El título es obligatorio"
            } else {
                textInputLayoutTitle.error = null

                val task = TaskEntity(
                    title = title,
                    description = if (description.isEmpty()) null else description
                )

                taskViewModel.insertTask(task)
                Toast.makeText(this, "Tarea guardada", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}