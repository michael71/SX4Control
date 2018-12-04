package de.blankedv.sx4control.adapter


import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.blankedv.sx4control.R
import de.blankedv.sx4control.util.LocoUtil


import org.jetbrains.anko.find

data class SXD (val sx: Int, val data: Int)


class ChannelListAdapter(private val sxList: ArrayList<SXD>,
                         private val itemClick: OnItemClickListener
) :
        RecyclerView.Adapter<ChannelListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindForecast(sxList[position])
    }

    override fun getItemCount(): Int = sxList.size

    class ViewHolder(view: View, private val itemClick: OnItemClickListener)
        : RecyclerView.ViewHolder(view) {

        private val tvChanView = view.find<TextView>(R.id.tvChan)
        private val tvDataView = view.find<TextView>(R.id.tvData)
        private val tvGraphics = view.find<TextView>(R.id.tvDataGraphics)
        //private val descriptionView = view.find<TextView>(R.id.description)
        //private val maxTemperatureView = view.find<TextView>(R.id.maxTemperature)
        //private val minTemperatureView = view.find<TextView>(R.id.minTemperature)

        fun bindForecast(sxd: SXD) {
            with(sxd) {
                tvChanView.text = sxd.sx.toString()
                tvDataView.text = "(${sxd.data.toString()})"
                tvGraphics.text = LocoUtil.SXBinaryString(sxd.data)
                itemView.setOnClickListener { itemClick(this) }
            }
        }
    }

    interface OnItemClickListener {
        operator fun invoke(forecast: SXD)
    }
}
