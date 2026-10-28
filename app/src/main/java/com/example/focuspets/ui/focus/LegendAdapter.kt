package com.example.focuspets.ui.focus

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.focuspets.R
import com.example.focuspets.databinding.ItemLegendBinding

/** Pie legend: color swatch, content, minutes, and percentage. */
class LegendAdapter : RecyclerView.Adapter<LegendAdapter.VH>() {

    private var items: List<LegendItem> = emptyList()

    /** Legend tap requests content recoloring in the records screen. */
    var onItemClick: ((LegendItem) -> Unit)? = null

    data class LegendItem(
        val color: Int,
        val label: String,
        val minutes: Int,
        val percent: Int
    )

    fun submit(list: List<LegendItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLegendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }
    }

    override fun getItemCount(): Int = items.size

    class VH(private val b: ItemLegendBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: LegendItem) {
            b.dotColor.setBackgroundColor(item.color)
            b.tvLegendLabel.text = item.label
            b.tvLegendMinutes.text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_017 ,(item.minutes).toString())
            b.tvLegendPercent.text = "${item.percent}%"
        }
    }
}
