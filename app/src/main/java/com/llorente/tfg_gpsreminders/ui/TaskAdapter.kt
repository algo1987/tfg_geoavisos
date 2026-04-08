package com.llorente.tfg_gpsreminders.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.llorente.tfg_gpsreminders.R
import com.llorente.tfg_gpsreminders.data.local.TaskEntity

class TaskAdapter(
    private var taskList: List<TaskEntity>,
    private val onTaskChecked: (TaskEntity) -> Unit,
    private val onTaskDeleted: (TaskEntity) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBoxCompleted: CheckBox = itemView.findViewById(R.id.checkBoxCompleted)
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)
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
        holder.textViewDescription.text = task.description ?: ""
        holder.checkBoxCompleted.isChecked = task.isCompleted

        holder.checkBoxCompleted.setOnCheckedChangeListener { _, _ ->
            onTaskChecked(task.copy(isCompleted = holder.checkBoxCompleted.isChecked))
        }

        holder.buttonDelete.setOnClickListener {
            onTaskDeleted(task)
        }
    }

    override fun getItemCount(): Int = taskList.size

    fun updateTasks(newTasks: List<TaskEntity>) {
        taskList = newTasks
        notifyDataSetChanged()
    }
}