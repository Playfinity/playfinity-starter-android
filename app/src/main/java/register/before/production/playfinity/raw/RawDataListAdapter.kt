package register.before.production.playfinity.raw

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.playfinity.sdk.bluetooth.BluetoothDataRaw
import kotlinx.android.synthetic.main.sensor_raw_data_list_view.view.*
import java.util.*

data class RawDataListItem(
        val id: Int,
        val ax: String,
        val ay: String,
        val az: String,
        val gx: String,
        val gy: String,
        val gz: String,
        val baro: String,
        val sensortime: String) {
    constructor(id: Int, bluetoothDataRaw: BluetoothDataRaw) : this(
            id,
            valueFormat.format(formatLocale, bluetoothDataRaw.accx ?: 0.0),
            valueFormat.format(formatLocale, bluetoothDataRaw.accy ?: 0.0),
            valueFormat.format(formatLocale, bluetoothDataRaw.accz ?: 0.0),
            valueFormat.format(formatLocale, bluetoothDataRaw.gyrox ?: 0.0),
            valueFormat.format(formatLocale, bluetoothDataRaw.gyroy ?: 0.0),
            valueFormat.format(formatLocale, bluetoothDataRaw.gyroz ?: 0.0),
            (bluetoothDataRaw.baroRaw ?: 0L).toString(),
            (bluetoothDataRaw.sensorTime ?: 0L).toString()
    )

    constructor(id: Int) : this(
            id,
            "accx",
            "accy",
            "accz",
            "gyrox",
            "gyroy",
            "gyroz",
            "baro",
            "time"
    )

    companion object {
        private const val valueFormat = "%.1f"
        private val formatLocale = Locale.US
    }
}

class RawDataViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(item: RawDataListItem) {
        itemView.ax.text = item.ax
        itemView.ay.text = item.ay
        itemView.az.text = item.az
        itemView.gx.text = item.gx
        itemView.gy.text = item.gy
        itemView.gz.text = item.gz
        itemView.baro.text = item.baro
        itemView.time.text = item.sensortime
    }
}

class RawDataListAdapter : RecyclerView.Adapter<RawDataViewHolder>() {

    private val items = mutableListOf<RawDataListItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RawDataViewHolder {
        return RawDataViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.sensor_raw_data_list_view, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(viewHolder: RawDataViewHolder, position: Int) {
        viewHolder.bind(items[position])
    }

    fun getItems(): List<RawDataListItem> = items

    fun submit(rawDataListItems: List<RawDataListItem>) {
        val positionStart = items.size + 1
        items.addAll(rawDataListItems)
        notifyItemRangeInserted(positionStart, rawDataListItems.size)
        // scroll to end of table
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }
}