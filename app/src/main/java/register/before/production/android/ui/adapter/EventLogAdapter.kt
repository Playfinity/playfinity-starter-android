package register.before.production.android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_log.view.*
import register.before.production.android.R

class EventLogAdapter(private val logList: MutableList<EventLogEntry>) : RecyclerView.Adapter<EventLogAdapter.ViewHolder>() {

    //region Lifecycle

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

    fun insertRow(row: EventLogEntry) {
        logList.add(row)
        notifyDataSetChanged()
    }

    fun clear() {
        logList.clear()
        notifyDataSetChanged()
    }

    //endregion

    //region ViewHolder

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(logEntry: EventLogEntry) {
            itemView.logEntryTitleView.text = logEntry.name
            itemView.logEntryDescView.text = logEntry.params
        }
    }

    //endregion
}
