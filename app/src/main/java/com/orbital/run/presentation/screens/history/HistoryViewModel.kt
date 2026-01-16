package com.orbital.run.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbital.run.domain.models.Activity
import com.orbital.run.domain.repositories.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val activityRepository: ActivityRepository
) : ViewModel() {
    
    private val _selectedFilter = MutableStateFlow(ActivityFilter.ALL)
    val selectedFilter: StateFlow<ActivityFilter> = _selectedFilter.asStateFlow()
    
    val activities: StateFlow<List<Activity>> = combine(
        activityRepository.observeActivities(),
        selectedFilter
    ) { allActivities, filter ->
        allActivities.filter { filter.predicate(it) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun selectFilter(filter: ActivityFilter) {
        _selectedFilter.value = filter
    }
    
    fun deleteActivity(activity: Activity) {
        viewModelScope.launch {
            // TODO: Implement delete
            // activityRepository.deleteActivity(activity.id)
        }
    }
}
