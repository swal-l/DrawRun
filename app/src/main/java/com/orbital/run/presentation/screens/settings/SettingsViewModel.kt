package com.orbital.run.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    // TODO: Inject PreferencesRepository
) : ViewModel() {
    
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    private val _useMetricUnits = MutableStateFlow(true)
    val useMetricUnits: StateFlow<Boolean> = _useMetricUnits.asStateFlow()
    
    private val _stravaConnected = MutableStateFlow(false)
    val stravaConnected: StateFlow<Boolean> = _stravaConnected.asStateFlow()
    
    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
        // TODO: Save to preferences
    }
    
    fun toggleUnits() {
        _useMetricUnits.value = !_useMetricUnits.value
        // TODO: Save to preferences
    }
    
    fun openStravaSettings() {
        // TODO: Navigate to Strava OAuth
    }
    
    fun syncNow() {
        viewModelScope.launch {
            // TODO: Trigger sync
        }
    }
    
    fun exportData() {
        viewModelScope.launch {
            // TODO: Export to CSV
        }
    }
    
    fun resetApp() {
        viewModelScope.launch {
            // TODO: Show confirmation dialog
            // Clear database
        }
    }
}
