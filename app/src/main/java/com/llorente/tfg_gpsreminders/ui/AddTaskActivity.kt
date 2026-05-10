package com.llorente.tfg_gpsreminders.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.llorente.tfg_gpsreminders.R
import com.llorente.tfg_gpsreminders.data.local.TaskEntity
import com.llorente.tfg_gpsreminders.geofencing.GeofenceSyncManager
import com.llorente.tfg_gpsreminders.utils.LocationUtils
import kotlinx.coroutines.launch
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService

class AddTaskActivity : AppCompatActivity() {

    private val taskViewModel: TaskViewModel by viewModels()

    private var isEditMode = false
    private var taskId: Int = -1
    private var taskCompleted: Boolean = false
    private var taskLatitude: Double? = null
    private var taskLongitude: Double? = null
    private var taskLocationName: String? = null
    private var taskLocationAddress: String? = null
    private var taskRadius: Float? = null
    private var taskLocationReminderEnabled: Boolean = false

    private var updatingReminderSwitchProgrammatically = false

    private lateinit var textViewSelectedPlace: TextView
    private lateinit var buttonEditPlace: MaterialButton
    private lateinit var textInputLayoutEditPlace: TextInputLayout
    private lateinit var editTextPlaceName: TextInputEditText
    private lateinit var buttonConfirmPlaceEdit: MaterialButton
    private lateinit var buttonCancelPlaceEdit: MaterialButton

    private lateinit var textViewSelectedLocation: TextView
    private lateinit var buttonRemoveLocation: MaterialButton
    private lateinit var reminderOptionsContainer: LinearLayout
    private lateinit var switchLocationReminder: MaterialSwitch
    private lateinit var textInputLayoutRadius: TextInputLayout
    private lateinit var editTextRadius: TextInputEditText

    private val selectLocationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK || result.data == null) {
                return@registerForActivityResult
            }

            taskLatitude = result.data?.getDoubleExtra("selected_latitude", 0.0)
            taskLongitude = result.data?.getDoubleExtra("selected_longitude", 0.0)
            taskLocationName = LocationUtils.normalizePlaceName(
                result.data?.getStringExtra("selected_place_name")
            )
            taskLocationAddress = result.data?.getStringExtra("selected_address")

            if (taskRadius == null) {
                taskRadius = 150f
            }

            updateLocationUI()
            hidePlaceEditMode()
            renderReminderSection()

            if (!taskLocationReminderEnabled) {
                requestPermissionsForLocationReminder()
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION]
                ?: hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

            val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.POST_NOTIFICATIONS]
                    ?: hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                true
            }

            if (!fineGranted) {
                disableReminderWithMessage(getString(R.string.permission_location_required))
                return@registerForActivityResult
            }

            if (!notificationGranted) {
                disableReminderWithMessage(getString(R.string.permission_notification_required))
                return@registerForActivityResult
            }

            requestBackgroundLocationIfNeeded()
        }

    private val backgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !granted) {
                disableReminderWithMessage(getString(R.string.permission_background_required))
                return@registerForActivityResult
            }

            finalizeReminderActivation()
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarAddTask)
        val textInputLayoutTitle = findViewById<TextInputLayout>(R.id.textInputLayoutTitle)
        val editTextTitle = findViewById<TextInputEditText>(R.id.editTextTitle)
        val editTextDescription = findViewById<TextInputEditText>(R.id.editTextDescription)
        val buttonSelectLocation = findViewById<MaterialButton>(R.id.buttonSelectLocation)
        buttonRemoveLocation = findViewById(R.id.buttonRemoveLocation)
        val buttonSaveTask = findViewById<MaterialButton>(R.id.buttonSaveTask)

        textViewSelectedPlace = findViewById(R.id.textViewSelectedPlace)
        buttonEditPlace = findViewById(R.id.buttonEditPlace)
        textInputLayoutEditPlace = findViewById(R.id.textInputLayoutEditPlace)
        editTextPlaceName = findViewById(R.id.editTextPlaceName)
        buttonConfirmPlaceEdit = findViewById(R.id.buttonConfirmPlaceEdit)
        buttonCancelPlaceEdit = findViewById(R.id.buttonCancelPlaceEdit)

        textViewSelectedLocation = findViewById(R.id.textViewSelectedLocation)
        reminderOptionsContainer = findViewById(R.id.reminderOptionsContainer)
        switchLocationReminder = findViewById(R.id.switchLocationReminder)
        textInputLayoutRadius = findViewById(R.id.textInputLayoutRadius)
        editTextRadius = findViewById(R.id.editTextRadius)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        readIntentData()
        updateLocationUI()
        hidePlaceEditMode()
        renderReminderSection()

        if (isEditMode) {
            toolbar.title = getString(R.string.title_edit_task)
            buttonSaveTask.text = getString(R.string.button_save_changes)

            editTextTitle.setText(intent.getStringExtra("task_title").orEmpty())
            editTextDescription.setText(intent.getStringExtra("task_description").orEmpty())
        } else {
            toolbar.title = getString(R.string.title_add_task)
            buttonSaveTask.text = getString(R.string.button_save_task)
        }

        buttonSelectLocation.setOnClickListener {
            openSelectLocationScreen()
        }

        buttonRemoveLocation.setOnClickListener {
            showRemoveLocationConfirmationDialog()
        }

        buttonEditPlace.setOnClickListener {
            showPlaceEditMode()
        }

        buttonConfirmPlaceEdit.setOnClickListener {
            confirmPlaceEdit()
        }

        buttonCancelPlaceEdit.setOnClickListener {
            cancelPlaceEdit()
        }

        switchLocationReminder.setOnCheckedChangeListener { _, isChecked ->
            if (updatingReminderSwitchProgrammatically) {
                return@setOnCheckedChangeListener
            }

            if (!hasLocationSelected()) {
                updateReminderSwitch(false)
                Toast.makeText(
                    this,
                    getString(R.string.message_location_required),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                requestPermissionsForLocationReminder()
            } else {
                taskLocationReminderEnabled = false
            }
        }

        buttonSaveTask.setOnClickListener {
            val title = editTextTitle.text?.toString()?.trim().orEmpty()
            val description = editTextDescription.text?.toString()?.trim().orEmpty()

            if (title.isEmpty()) {
                textInputLayoutTitle.error = getString(R.string.error_title_required)
                return@setOnClickListener
            }

            textInputLayoutTitle.error = null

            if (textInputLayoutEditPlace.visibility == View.VISIBLE) {
                confirmPlaceEdit()
            }

            val radiusValue = getValidatedRadiusOrNull()
            if (switchLocationReminder.isChecked && radiusValue == null) {
                return@setOnClickListener
            }

            val taskToSave = if (isEditMode) {
                TaskEntity(
                    id = taskId,
                    title = title,
                    description = if (description.isEmpty()) null else description,
                    isCompleted = taskCompleted,
                    latitude = taskLatitude,
                    longitude = taskLongitude,
                    locationName = taskLocationName,
                    locationAddress = taskLocationAddress,
                    radius = if (taskLocationReminderEnabled) radiusValue else null,
                    isLocationReminderEnabled = taskLocationReminderEnabled
                )
            } else {
                TaskEntity(
                    title = title,
                    description = if (description.isEmpty()) null else description,
                    isCompleted = false,
                    latitude = taskLatitude,
                    longitude = taskLongitude,
                    locationName = taskLocationName,
                    locationAddress = taskLocationAddress,
                    radius = if (taskLocationReminderEnabled) radiusValue else null,
                    isLocationReminderEnabled = taskLocationReminderEnabled
                )
            }

            lifecycleScope.launch {
                if (isEditMode) {
                    taskViewModel.updateTask(taskToSave)
                    Toast.makeText(this@AddTaskActivity, R.string.toast_task_updated, Toast.LENGTH_SHORT).show()
                } else {
                    taskViewModel.insertTask(taskToSave)
                    Toast.makeText(this@AddTaskActivity, R.string.toast_task_saved, Toast.LENGTH_SHORT).show()
                }

                GeofenceSyncManager.syncAllGeofences(this@AddTaskActivity)
                finish()
            }
        }
    }

    private fun openSelectLocationScreen() {
        val intent = Intent(this, SelectLocationActivity::class.java).apply {
            taskLatitude?.let { putExtra("task_latitude", it) }
            taskLongitude?.let { putExtra("task_longitude", it) }
            putExtra("task_location_name", taskLocationName)
            putExtra("task_location_address", taskLocationAddress)

            val radius = editTextRadius.text?.toString()?.trim()?.toFloatOrNull() ?: (taskRadius ?: 150f)
            putExtra("task_radius", radius)
        }

        selectLocationLauncher.launch(intent)
    }

    private fun readIntentData() {
        taskId = intent.getIntExtra("task_id", -1)
        isEditMode = taskId != -1

        taskCompleted = intent.getBooleanExtra("task_completed", false)

        if (intent.hasExtra("task_latitude")) {
            taskLatitude = intent.getDoubleExtra("task_latitude", 0.0)
        }

        if (intent.hasExtra("task_longitude")) {
            taskLongitude = intent.getDoubleExtra("task_longitude", 0.0)
        }

        taskLocationName = LocationUtils.normalizePlaceName(
            intent.getStringExtra("task_location_name")
        )
        taskLocationAddress = intent.getStringExtra("task_location_address")

        if (intent.hasExtra("task_radius")) {
            taskRadius = intent.getFloatExtra("task_radius", 150f)
        }

        taskLocationReminderEnabled =
            intent.getBooleanExtra("task_location_reminder_enabled", false)
    }

    private fun updateLocationUI() {
        val hasLocation = hasLocationSelected()

        if (!hasLocation) {
            textViewSelectedPlace.text = getString(R.string.no_place_selected)
            textViewSelectedLocation.text = getString(R.string.no_location_selected)
            buttonEditPlace.visibility = View.GONE
            buttonRemoveLocation.visibility = View.GONE
            textInputLayoutEditPlace.visibility = View.GONE
            buttonConfirmPlaceEdit.visibility = View.GONE
            buttonCancelPlaceEdit.visibility = View.GONE
            reminderOptionsContainer.visibility = View.GONE
            taskLocationReminderEnabled = false
            updateReminderSwitch(false)
            return
        }

        textViewSelectedPlace.text = buildPlaceText()
        textViewSelectedLocation.text = buildLocationText()
        buttonEditPlace.visibility = View.VISIBLE
        buttonRemoveLocation.visibility = View.VISIBLE
        reminderOptionsContainer.visibility = View.VISIBLE
    }

    private fun renderReminderSection() {
        if (!hasLocationSelected()) {
            reminderOptionsContainer.visibility = View.GONE
            return
        }

        reminderOptionsContainer.visibility = View.VISIBLE

        if (taskRadius == null) {
            taskRadius = 150f
        }

        editTextRadius.setText(
            if (taskRadius != null) {
                taskRadius!!.toInt().toString()
            } else {
                "150"
            }
        )

        updateReminderSwitch(taskLocationReminderEnabled)
    }

    private fun buildPlaceText(): String {
        return LocationUtils.buildLocationLabel(
            placeName = taskLocationName,
            address = null,
            latitude = taskLatitude,
            longitude = taskLongitude,
            emptyText = getString(R.string.no_place_selected)
        )
    }

    private fun buildLocationText(): String {
        if (!taskLocationAddress.isNullOrBlank()) {
            return taskLocationAddress!!
        }

        return LocationUtils.formatCoordinates(taskLatitude, taskLongitude)
    }

    private fun showPlaceEditMode() {
        if (!hasLocationSelected()) {
            return
        }

        textInputLayoutEditPlace.visibility = View.VISIBLE
        buttonConfirmPlaceEdit.visibility = View.VISIBLE
        buttonCancelPlaceEdit.visibility = View.VISIBLE

        textViewSelectedPlace.visibility = View.GONE
        buttonEditPlace.visibility = View.GONE

        editTextPlaceName.setText(taskLocationName.orEmpty())
        editTextPlaceName.requestFocus()
        editTextPlaceName.setSelection(editTextPlaceName.text?.length ?: 0)

        editTextPlaceName.post {
            val imm = getSystemService<InputMethodManager>()
            imm?.showSoftInput(editTextPlaceName, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hidePlaceEditMode() {
        val imm = getSystemService<InputMethodManager>()
        imm?.hideSoftInputFromWindow(editTextPlaceName.windowToken, 0)

        textInputLayoutEditPlace.visibility = View.GONE
        buttonConfirmPlaceEdit.visibility = View.GONE
        buttonCancelPlaceEdit.visibility = View.GONE

        textViewSelectedPlace.visibility = View.VISIBLE
        buttonEditPlace.visibility = if (hasLocationSelected()) View.VISIBLE else View.GONE
    }

    private fun confirmPlaceEdit() {
        val newValue = editTextPlaceName.text?.toString()?.trim().orEmpty()
        taskLocationName = LocationUtils.normalizePlaceName(
            if (newValue.isEmpty()) null else newValue
        )
        textViewSelectedPlace.text = buildPlaceText()
        hidePlaceEditMode()
    }

    private fun cancelPlaceEdit() {
        editTextPlaceName.setText(taskLocationName.orEmpty())
        hidePlaceEditMode()
    }

    private fun showRemoveLocationConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_remove_location_title)
            .setMessage(R.string.dialog_remove_location_message)
            .setNegativeButton(R.string.button_cancel, null)
            .setPositiveButton(R.string.button_remove) { _, _ ->
                clearLocation()
            }
            .show()
    }

    private fun clearLocation() {
        taskLatitude = null
        taskLongitude = null
        taskLocationName = null
        taskLocationAddress = null
        taskRadius = null
        taskLocationReminderEnabled = false

        updateLocationUI()
        hidePlaceEditMode()
        renderReminderSection()

        Toast.makeText(this, R.string.toast_location_removed, Toast.LENGTH_SHORT).show()
    }

    private fun getValidatedRadiusOrNull(): Float? {
        if (!taskLocationReminderEnabled) {
            return null
        }

        val rawRadius = editTextRadius.text?.toString()?.trim().orEmpty()
        val radius = rawRadius.toFloatOrNull()

        if (radius == null || radius <= 0f) {
            textInputLayoutRadius.error = getString(R.string.error_invalid_radius)
            return null
        }

        textInputLayoutRadius.error = null
        taskRadius = radius
        return radius
    }

    private fun requestPermissionsForLocationReminder() {
        val missingPermissions = mutableListOf<String>()

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (missingPermissions.isNotEmpty()) {
            showLocationPermissionsExplanation(missingPermissions)
            return
        }

        requestBackgroundLocationIfNeeded()
    }

    private fun showLocationPermissionsExplanation(missingPermissions: List<String>) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_location_permissions_title)
            .setMessage(R.string.dialog_location_permissions_message)
            .setNegativeButton(R.string.button_cancel) { _, _ ->
                disableReminderWithMessage(getString(R.string.location_reminder_not_enabled))
            }
            .setPositiveButton(R.string.button_continue) { _, _ ->
                permissionLauncher.launch(missingPermissions.toTypedArray())
            }
            .show()
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        ) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_background_permission_title)
                .setMessage(R.string.dialog_background_permission_message)
                .setNegativeButton(R.string.button_cancel) { _, _ ->
                    disableReminderWithMessage(getString(R.string.location_reminder_not_enabled))
                }
                .setPositiveButton(R.string.button_continue) { _, _ ->
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
                .show()
            return
        }

        finalizeReminderActivation()
    }

    private fun finalizeReminderActivation() {
        taskLocationReminderEnabled = true
        if (taskRadius == null) {
            taskRadius = 150f
        }

        if (editTextRadius.text.isNullOrBlank()) {
            editTextRadius.setText(taskRadius!!.toInt().toString())
        }

        renderReminderSection()
        updateReminderSwitch(true)
        Toast.makeText(this, R.string.toast_location_reminder_enabled, Toast.LENGTH_SHORT).show()
    }

    private fun disableReminderWithMessage(message: String) {
        taskLocationReminderEnabled = false
        updateReminderSwitch(false)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateReminderSwitch(enabled: Boolean) {
        updatingReminderSwitchProgrammatically = true
        switchLocationReminder.isChecked = enabled
        updatingReminderSwitchProgrammatically = false
    }

    private fun hasLocationSelected(): Boolean {
        return taskLatitude != null && taskLongitude != null
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}