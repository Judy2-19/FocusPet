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
        holder.points.text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_018 ,(item.totalPoints).toString())

        // Leaderboard avatars use cat/dog artwork and emoji for other pets.
        bindPetAvatar(holder, item)

        val isMe = item.uid == myUid
        holder.itemView.setBackgroundColor(if (isMe) 0x1AFFB300.toInt() else 0x00000000)
        holder.itemView.contentDescription = buildString {
            append(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_126 ,(item.rank).toString(), (item.displayName).toString(), (item.totalPoints).toString()))
            append(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_022 ,(PetCatalog.emoji(item.petId)).toString()))
            if (isMe) append(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_023))
        }
    }

    private fun bindPetAvatar(holder: VH, item: LeaderboardEntry) {
        val path = item.petAvatar
        if (!path.isNullOrEmpty()) {
            // Decode artwork on a background thread to avoid list jank.
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
                    if (iv.tag != path) return@post   // The row has been recycled; discard this result.
                    if (bmp != null) {
                        iv.setImageBitmap(bmp)
                    } else {
                        // Fall back to emoji if artwork loading fails.
                        iv.visibility = View.GONE
                        tv.visibility = View.VISIBLE
                        tv.text = PetCatalog.emoji(fallbackId)
                    }
                }
            }
        } else {
            // Use emoji directly for pets without artwork.
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
