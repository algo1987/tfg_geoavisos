package com.llorente.tfg_gpsreminders.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.llorente.tfg_gpsreminders.R
import com.llorente.tfg_gpsreminders.data.local.TaskEntity
import com.llorente.tfg_gpsreminders.utils.LocationUtils

class TaskAdapter(
    private var taskList: List<TaskEntity>,
    private val onTaskChecked: (TaskEntity) -> Unit,
    private val onTaskDeleted: (TaskEntity) -> Unit,
    private val onTaskClicked: (TaskEntity) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBoxCompleted: CheckBox = itemView.findViewById(R.id.checkBoxCompleted)
        val textViewStatus: TextView = itemView.findViewById(R.id.textViewStatus)
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)
        val textViewLocation: TextView = itemView.findViewById(R.id.textViewLocation)
        val buttonDelete: Button = itemView.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)

        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        holder.textViewTitle.text = task.title

        if (task.description.isNullOrBlank()) {
            holder.textViewDescription.visibility = View.GONE
        } else {
            holder.textViewDescription.visibility = View.VISIBLE
            holder.textViewDescription.text = task.description
        }

        holder.textViewLocation.text = "📍 ${getLocationText(task)}"

        holder.checkBoxCompleted.setOnCheckedChangeListener(null)
        holder.checkBoxCompleted.isChecked = task.isCompleted

        if (task.isCompleted) {
            applyCompletedStyle(holder)
        } else {
            applyPendingStyle(holder)
        }

        holder.checkBoxCompleted.setOnCheckedChangeListener { _, isChecked ->
            onTaskChecked(task.copy(isCompleted = isChecked))

            Toast.makeText(
                holder.itemView.context,
                if (isChecked) "Tarea completada" else "Tarea pendiente",
                Toast.LENGTH_SHORT
            ).show()
        }

        holder.buttonDelete.setOnClickListener {
            onTaskDeleted(task)
        }

        holder.itemView.setOnClickListener {
            onTaskClicked(task)
        }
    }

    override fun getItemCount(): Int = taskList.size

    fun updateTasks(newTasks: List<TaskEntity>) {
        taskList = newTasks
        notifyDataSetChanged()
    }

    private fun applyCompletedStyle(holder: TaskViewHolder) {
        holder.textViewStatus.text = "Completada"
        holder.textViewStatus.setBackgroundResource(R.drawable.bg_status_completed)

        holder.textViewTitle.paintFlags =
            holder.textViewTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

        holder.textViewTitle.alpha = 0.6f
        holder.textViewDescription.alpha = 0.6f
        holder.textViewLocation.alpha = 0.6f
    }

    private fun applyPendingStyle(holder: TaskViewHolder) {
        holder.textViewStatus.text = "Pendiente"
        holder.textViewStatus.setBackgroundResource(R.drawable.bg_status_pending)

        holder.textViewTitle.paintFlags =
            holder.textViewTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

        holder.textViewTitle.alpha = 1.0f
        holder.textViewDescription.alpha = 1.0f
        holder.textViewLocation.alpha = 1.0f
    }

    private fun getLocationText(task: TaskEntity): String {
        return LocationUtils.buildLocationLabel(
            placeName = task.locationName,
            address = task.locationAddress,
            latitude = task.latitude,
            longitude = task.longitude,
            emptyText = "No seleccionado"
        )
    }
}