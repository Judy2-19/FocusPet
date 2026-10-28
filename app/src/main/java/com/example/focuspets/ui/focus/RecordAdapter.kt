package com.example.focuspets.ui.focus

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.focuspets.R
import com.example.focuspets.databinding.ItemFocusRecordBinding

/** Record details: left color bar, content, completion time, and duration. */
class RecordAdapter : RecyclerView.Adapter<RecordAdapter.VH>() {

    private var items: List<RecordItem> = emptyList()

    data class RecordItem(
        val color: Int,
        val content: String,
        val timeText: String,
        val minutes: Int
    )

    fun submit(list: List<RecordItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFocusRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class VH(private val b: ItemFocusRecordBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: RecordItem) {
            b.barColor.setBackgroundColor(item.color)
            b.tvRecordContent.text = item.content
            b.tvRecordTime.text = item.timeText
            b.tvRecordMinutes.text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_017 ,(item.minutes).toString())
        }
    }
}
