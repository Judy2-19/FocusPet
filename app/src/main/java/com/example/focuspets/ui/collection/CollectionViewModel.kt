package com.example.focuspets.ui.collection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.focuspets.db.PetRepository
import com.example.focuspets.db.dao.PetWithState
import com.example.focuspets.db.entity.PetEntity
import kotlinx.coroutines.launch

/** 列表项的展示模型：把「是否解锁 + 当前积分」合并进每张卡片 */
data class PetDisplay(
    val pet: PetEntity,
    val isUnlocked: Boolean,
    val availablePoints: Int
) {
    val enoughPoints: Boolean get() = availablePoints >= pet.unlockCost
    val pointsNeeded: Int get() = pet.unlockCost - availablePoints
}

class CollectionViewModel(private val repository: PetRepository) : ViewModel() {

    /** 顶部积分展示 */
    val availablePoints: LiveData<Int> = repository.getAvailablePoints()

    /** 图鉴列表（宠物状态 × 积分 两个数据源合并） */
    private val _uiState = MediatorLiveData<List<PetDisplay>>()
    val uiState: LiveData<List<PetDisplay>> = _uiState

    /** 解锁成功事件（弹 Toast 庆祝用） */
    private val _unlockEvent = MutableLiveData<PetEntity?>()
    val unlockEvent: LiveData<PetEntity?> = _unlockEvent

    init {
        var pets: List<PetWithState>? = null
        var points: Int? = null
        fun emit() {
            val p = pets ?: return
            val pt = points ?: return
            _uiState.value = p.map { PetDisplay(it.pet, it.isUnlocked, pt) }
        }
        _uiState.addSource(repository.getPetsWithState()) { pets = it; emit() }
        _uiState.addSource(repository.getAvailablePoints()) { points = it; emit() }
    }

    fun unlockPet(pet: PetEntity) {
        viewModelScope.launch {
            if (repository.unlockPet(pet)) _unlockEvent.value = pet
        }
    }

    fun consumeUnlockEvent() {
        _unlockEvent.value = null
    }
}

class CollectionViewModelFactory(private val repository: PetRepository) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CollectionViewModel(repository) as T
}
