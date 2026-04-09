package com.llorente.tfg_gpsreminders.ui

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

class TaskAdapter(
    private var taskList: List<TaskEntity>,
    private val onTaskChecked: (TaskEntity) -> Unit,
    private val onTaskDeleted: (TaskEntity) -> Unit,
    private val onTaskClicked: (TaskEntity) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBoxCompleted: CheckBox = itemView.findViewById(R.id.checkBoxCompleted)
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)
        val buttonDelete: Button = itemView.findViewById(R.id.buttonDelete)

        val textViewStatus: TextView = itemView.findViewById(R.id.textViewStatus)
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

        holder.checkBoxCompleted.setOnCheckedChangeListener(null)
        holder.checkBoxCompleted.isChecked = task.isCompleted

        if (task.isCompleted) {
            holder.textViewStatus.text = "Completada"
            holder.textViewStatus.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_green_dark)
            )

            holder.textViewTitle.paintFlags =
                holder.textViewTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

            holder.textViewTitle.alpha = 0.6f
            holder.textViewDescription.alpha = 0.6f

        } else {
            holder.textViewStatus.text = "Pendiente"
            holder.textViewStatus.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_orange_dark)
            )

            holder.textViewTitle.paintFlags =
                holder.textViewTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()

            holder.textViewTitle.alpha = 1.0f
            holder.textViewDescription.alpha = 1.0f
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
}