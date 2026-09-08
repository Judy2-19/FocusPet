package com.example.focuspets.ui.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.focuspets.db.PetRepository
import com.example.focuspets.db.dao.PetWithState
import com.example.focuspets.db.entity.PetEntity
import com.example.focuspets.model.PetCareState
import com.example.focuspets.model.PetMood

/**
 * 主界面 UI 状态：
 * - selectedPet：当前选中宠物（默认第一只已解锁的，点击/滑动切换）
 * - nextLockedPet：图鉴中解锁积分最低的未解锁宠物（进度条目标）
 * - mood：宠物心情（SICK / NORMAL / GLOW）
 */
data class HomeUiState(
    val selectedPet: PetEntity,
    val unlockedCount: Int,
    val availablePoints: Int,
    val nextLockedPet: PetEntity?,
    val progressPercent: Int,
    val remainingPoints: Int,
    val mood: PetMood
)

class HomeViewModel(private val repository: PetRepository) : ViewModel() {

    /** 当前选中的宠物下标（基于「已解锁列表」，0 = 默认第一只） */
    private val selectedIndex = MutableLiveData(0)

    /** 跨进程重启后待恢复的首页宠物 id（由 SettingsManager 持久化；-1 表示无需恢复） */
    private var pendingPetId = -1

    /** 指定下次 emit 时恢复到某个宠物（用于「退出后再进入保持同一只宠物」） */
    fun selectPetById(id: Int) {
        pendingPetId = id
    }

    /** 宠物心情（专注成功/失败后由 Fragment 调 refreshMood 刷新） */
    private val mood = MutableLiveData(PetMood.NORMAL)

    /** 最新已解锁数量，切换宠物取模用 */
    private var unlockedCount = 1

    /**
     * 四个数据源合并成一个 UiState：
     * 图鉴解锁状态 + 可用积分（Room LiveData，数据库一变自动刷新）
     * + 用户选择下标 + 宠物心情
     */
    val uiState: LiveData<HomeUiState> = MediatorLiveData<HomeUiState>().apply {
        var pets: List<PetWithState>? = null
        var points: Int? = null
        fun emit() {
            val p = pets ?: return
            val pt = points ?: return
            val unlocked = p.filter { it.isUnlocked }.map { it.pet }
            if (unlocked.isEmpty()) return
            unlockedCount = unlocked.size
            // 跨进程重启后恢复上次首页选中的宠物（SettingsManager 持久化）
            if (pendingPetId >= 0) {
                var idx = unlocked.indexOfFirst { it.id == pendingPetId }
                if (idx < 0) idx = 0
                pendingPetId = -1
                if (idx != selectedIndex.value ?: 0) {
                    selectedIndex.value = idx
                    return          // 下标变化会重新触发 emit，用新下标渲染
                }
            }
            val index = (selectedIndex.value ?: 0).coerceIn(0, unlocked.size - 1)

            // 进度条目标：解锁积分最低的未解锁宠物（列表已按 unlock_cost 升序）
            val nextLocked = p.firstOrNull { !it.isUnlocked }?.pet

            value = HomeUiState(
                selectedPet = unlocked[index],
                unlockedCount = unlocked.size,
                availablePoints = pt,
                nextLockedPet = nextLocked,
                progressPercent = if (nextLocked == null) 100
                else (pt * 100 / nextLocked.unlockCost).coerceAtMost(100),
                remainingPoints = if (nextLocked == null) 0
                else (nextLocked.unlockCost - pt).coerceAtLeast(0),
                mood = mood.value ?: PetMood.NORMAL
            )
        }
        addSource(repository.getPetsWithState()) { pets = it; emit() }
        addSource(repository.getAvailablePoints()) { points = it; emit() }
        addSource(selectedIndex) { emit() }
        addSource(mood) { emit() }
    }

    /** 点击宠物 / 左滑：切换到下一只已解锁宠物（循环） */
    fun selectNext() {
        val count = unlockedCount.coerceAtLeast(1)
        selectedIndex.value = ((selectedIndex.value ?: 0) + 1).mod(count)
    }

    /** 右滑：切换到上一只 */
    fun selectPrev() {
        val count = unlockedCount.coerceAtLeast(1)
        selectedIndex.value = (((selectedIndex.value ?: 0) - 1) + count).mod(count)
    }

    /** 专注完成/失败/回到页面时刷新心情 */
    fun refreshMood(context: Context) {
        mood.value = PetCareState.getMood(context)
    }
}

class HomeViewModelFactory(private val repository: PetRepository) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(repository) as T
}
