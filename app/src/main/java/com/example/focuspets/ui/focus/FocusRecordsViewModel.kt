package com.example.focuspets.ui.focus

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focuspets.db.PetRepository
import com.example.focuspets.db.dao.ContentMinutes
import com.example.focuspets.db.entity.FocusRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 专注记录页 ViewModel：根据传入的时间区间（日/周/月/年）从本地库聚合出
 * - records：区间内的全部专注事件（含内容），用于详情列表
 * - slices：各专注内容的分钟数聚合，用于饼图
 * - totalMinutes / sessionCount：本时段汇总
 *
 * 区间边界由 Activity 用 Calendar 计算后传入，ViewModel 只负责取数与聚合。
 */
class FocusRecordsViewModel(private val repo: PetRepository) : ViewModel() {

    private val _records = MutableLiveData<List<FocusRecordEntity>>()
    val records: LiveData<List<FocusRecordEntity>> = _records

    private val _slices = MutableLiveData<List<ContentMinutes>>()
    val slices: LiveData<List<ContentMinutes>> = _slices

    private val _totalMinutes = MutableLiveData(0)
    val totalMinutes: LiveData<Int> = _totalMinutes

    private val _sessionCount = MutableLiveData(0)
    val sessionCount: LiveData<Int> = _sessionCount

    fun load(startMs: Long, endMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val recs = runCatching { repo.getRecordsBetween(startMs, endMs) }.getOrDefault(emptyList())
            val cms = runCatching { repo.getContentMinutesBetween(startMs, endMs) }.getOrDefault(emptyList())
            val total = recs.sumOf { it.durationMinutes }
            _records.postValue(recs)
            _slices.postValue(cms)
            _totalMinutes.postValue(total)
            _sessionCount.postValue(recs.size)
        }
    }
}

class FocusRecordsViewModelFactory(private val repo: PetRepository) :
    androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FocusRecordsViewModel(repo) as T
}
