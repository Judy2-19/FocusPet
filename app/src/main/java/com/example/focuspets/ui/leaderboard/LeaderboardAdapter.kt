package com.example.focuspets.ui.leaderboard

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.focuspets.R
import com.example.focuspets.cloud.CloudSyncManager
import com.example.focuspets.cloud.model.LeaderboardEntry
import com.example.focuspets.model.PetCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.VH>() {

    private val items = mutableListOf<LeaderboardEntry>()
    private val myUid = CloudSyncManager.currentUid()
    private val loadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun submit(list: List<LeaderboardEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.leaderboard_item, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.rank.text = "#${item.rank}"
        holder.name.text = item.displayName
        holder.points.text = "${item.totalPoints} 分"

        // 宠物头像：猫/狗用 assets 立绘，其余用 emoji 兜底
        bindPetAvatar(holder, item)

        val isMe = item.uid == myUid
        holder.itemView.setBackgroundColor(if (isMe) 0x1AFFB300.toInt() else 0x00000000)
        holder.itemView.contentDescription = buildString {
            append("第 ${item.rank} 名，${item.displayName}，${item.totalPoints} 分")
            append("，宠物 ${PetCatalog.emoji(item.petId)}")
            if (isMe) append("，这是你")
        }
    }

    private fun bindPetAvatar(holder: VH, item: LeaderboardEntry) {
        val path = item.petAvatar
        if (!path.isNullOrEmpty()) {
            // 立绘：后台线程解码，避免卡列表
            holder.ivPet.visibility = View.VISIBLE
            holder.tvPet.visibility = View.GONE
            holder.ivPet.tag = path
            val iv = holder.ivPet
            val tv = holder.tvPet
            val fallbackId = item.petId
            loadScope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    try { iv.context.assets.open(path).use { BitmapFactory.decodeStream(it) } }
                    catch (_: Exception) { null }
                }
                iv.post {
                    if (iv.tag != path) return@post   // 行已被复用，放弃
                    if (bmp != null) {
                        iv.setImageBitmap(bmp)
                    } else {
                        // 立绘加载失败 → 退回 emoji
                        iv.visibility = View.GONE
                        tv.visibility = View.VISIBLE
                        tv.text = PetCatalog.emoji(fallbackId)
                    }
                }
            }
        } else {
            // 无立绘宠物：直接 emoji
            holder.ivPet.visibility = View.GONE
            holder.tvPet.visibility = View.VISIBLE
            holder.tvPet.text = PetCatalog.emoji(item.petId)
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val rank: TextView = v.findViewById(R.id.tv_rank)
        val name: TextView = v.findViewById(R.id.tv_name)
        val points: TextView = v.findViewById(R.id.tv_points)
        val ivPet: ImageView = v.findViewById(R.id.iv_pet)
        val tvPet: TextView = v.findViewById(R.id.tv_pet)
    }
}
