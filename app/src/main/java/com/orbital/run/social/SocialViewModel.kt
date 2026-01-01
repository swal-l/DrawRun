package com.orbital.run.social

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SocialViewModel(application: Application) : AndroidViewModel(application) {
    
    // Direct access for simplicity in this feature
    private val repository = SocialRepository

    val feed: StateFlow<List<SocialActivity>> = repository.feed

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshFeed()
        }
    }

    fun onDraw(activityId: String) {
        // Optimistic update handled in Repo or here? 
        // Repo handles it in memory for Mock.
        repository.toggleDraw(activityId)
    }
}
