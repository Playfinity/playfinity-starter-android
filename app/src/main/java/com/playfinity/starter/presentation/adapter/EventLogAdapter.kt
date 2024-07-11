package com.playfinity.starter.presentation.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.playfinity.starter.R

class EventLogAdapter(private val logList: MutableList<EventLogEntry>) :
    RecyclerView.Adapter<EventLogAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(logList[position])
    }

    override fun getItemCount(): Int {
        return logList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun insertRow(row: EventLogEntry) {
        logList.add(row)

        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(logEntry: EventLogEntry) {
            itemView.findViewById<TextView>(R.id.logEntryTitleView).text = logEntry.name
            itemView.findViewById<TextView>(R.id.logEntryDescView).text = logEntry.params
        }
    }
}
