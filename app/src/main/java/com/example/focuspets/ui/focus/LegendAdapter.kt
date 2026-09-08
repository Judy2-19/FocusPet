package com.example.focuspets.ui.focus

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.focuspets.R
import com.example.focuspets.databinding.ItemLegendBinding

/** 饼图图例：颜色块 + 内容 + 分钟数 + 占比 */
class LegendAdapter : RecyclerView.Adapter<LegendAdapter.VH>() {

    private var items: List<LegendItem> = emptyList()

    /** 点击某条图例 → 修改该内容的饼图颜色（在记录页中设置） */
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
            b.tvLegendMinutes.text = "${item.minutes} 分"
            b.tvLegendPercent.text = "${item.percent}%"
        }
    }
}
