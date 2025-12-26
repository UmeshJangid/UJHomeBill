package com.uj.homebill.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uj.homebill.data.database.AppDatabase
import com.uj.homebill.data.database.LatestReading
import com.uj.homebill.data.database.UserSettings
import com.uj.homebill.data.database.YearlyAnalytics
import com.uj.homebill.data.database.MonthlyBillSummary
import com.uj.homebill.data.repository.BillRepository
import com.uj.homebill.data.repository.FlatReadingData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BillViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: BillRepository
    
    // State flows
    private val _latestReadings = MutableStateFlow<List<LatestReading>>(emptyList())
    val latestReadings: StateFlow<List<LatestReading>> = _latestReadings.asStateFlow()
    
    private val _userSettings = MutableStateFlow<UserSettings?>(null)
    val userSettings: StateFlow<UserSettings?> = _userSettings.asStateFlow()
    
    private val _availableYears = MutableStateFlow<List<Int>>(emptyList())
    val availableYears: StateFlow<List<Int>> = _availableYears.asStateFlow()
    
    private val _yearlyAnalytics = MutableStateFlow<List<YearlyAnalytics>>(emptyList())
    val yearlyAnalytics: StateFlow<List<YearlyAnalytics>> = _yearlyAnalytics.asStateFlow()
    
    private val _selectedYearAnalytics = MutableStateFlow<YearlyAnalytics?>(null)
    val selectedYearAnalytics: StateFlow<YearlyAnalytics?> = _selectedYearAnalytics.asStateFlow()
    
    private val _monthlyBillSummaries = MutableStateFlow<List<MonthlyBillSummary>>(emptyList())
    val monthlyBillSummaries: StateFlow<List<MonthlyBillSummary>> = _monthlyBillSummaries.asStateFlow()
    
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()
    
    private val _clearDataState = MutableStateFlow<ClearDataState>(ClearDataState.Idle)
    val clearDataState: StateFlow<ClearDataState> = _clearDataState.asStateFlow()
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = BillRepository(database.billDao())
        
        // Load initial data
        loadLatestReadings()
        loadUserSettings()
        loadYearlyAnalytics()
    }
    
    private fun loadLatestReadings() {
        viewModelScope.launch {
            repository.getLatestReadingsFlow().collect { readings ->
                _latestReadings.value = readings
            }
        }
    }
    
    private fun loadUserSettings() {
        viewModelScope.launch {
            repository.getUserSettingsFlow().collect { settings ->
                _userSettings.value = settings
            }
        }
    }
    
    private fun loadYearlyAnalytics() {
        viewModelScope.launch {
            repository.getAllYearlyAnalytics().collect { analytics ->
                _yearlyAnalytics.value = analytics
            }
        }
        
        viewModelScope.launch {
            repository.getAvailableYears().collect { years ->
                _availableYears.value = years
            }
        }
    }
    
    fun loadMonthlyBillSummaries(year: Int) {
        viewModelScope.launch {
            repository.getMonthlyBillSummaries(year).collect { summaries ->
                _monthlyBillSummaries.value = summaries
            }
        }
        
        viewModelScope.launch {
            _selectedYearAnalytics.value = repository.getYearlyAnalytics(year)
        }
    }
    
    /**
     * Get the previous reading for a specific flat
     */
    fun getPreviousReadingForFlat(flatIndex: Int): Int {
        return _latestReadings.value.find { it.flatIndex == flatIndex }?.lastReading ?: 0
    }
    
    /**
     * Save bill record to database
     */
    fun saveBillRecord(
        totalBillAmount: Double,
        totalBuildingUnits: Int,
        ratePerUnit: Double,
        totalFlatUnits: Int,
        commonAreaUnits: Int,
        commonAreaTotalCost: Double,
        commonAreaCostPerFlat: Double,
        flatReadings: List<FlatReadingData>,
        isHindi: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                // Validate data
                if (totalBillAmount <= 0) {
                    throw IllegalArgumentException("Total bill amount must be greater than 0")
                }
                if (totalBuildingUnits <= 0) {
                    throw IllegalArgumentException("Total building units must be greater than 0")
                }
                if (flatReadings.isEmpty() || flatReadings.all { it.currentReading == 0 }) {
                    throw IllegalArgumentException("At least one flat must have readings")
                }
                
                // Check for invalid readings (current < previous)
                val invalidFlats = flatReadings.filter { it.currentReading < it.previousReading && it.currentReading > 0 }
                if (invalidFlats.isNotEmpty()) {
                    // Allow override - just log warning
                    // User has chosen to override
                }
                
                repository.saveBillRecord(
                    totalBillAmount = totalBillAmount,
                    totalBuildingUnits = totalBuildingUnits,
                    ratePerUnit = ratePerUnit,
                    totalFlatUnits = totalFlatUnits,
                    commonAreaUnits = commonAreaUnits,
                    commonAreaTotalCost = commonAreaTotalCost,
                    commonAreaCostPerFlat = commonAreaCostPerFlat,
                    flatReadings = flatReadings,
                    isHindi = isHindi
                )
                
                _saveState.value = SaveState.Success
                onSuccess()
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Unknown error")
                onError(e.message ?: "Unknown error")
            }
        }
    }
    
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
    
    /**
     * Update user name
     */
    fun updateUserName(name: String) {
        viewModelScope.launch {
            repository.updateUserName(name)
        }
    }
    
    /**
     * Clear all data with password verification
     */
    fun clearAllData(password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (password == "1234") {
                _clearDataState.value = ClearDataState.Clearing
                try {
                    repository.clearAllData()
                    _clearDataState.value = ClearDataState.Success
                    onSuccess()
                } catch (e: Exception) {
                    _clearDataState.value = ClearDataState.Error(e.message ?: "Unknown error")
                    onError(e.message ?: "Unknown error")
                }
            } else {
                _clearDataState.value = ClearDataState.Error("Incorrect password")
                onError("Incorrect password")
            }
        }
    }
    
    fun resetClearDataState() {
        _clearDataState.value = ClearDataState.Idle
    }
}

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}

sealed class ClearDataState {
    object Idle : ClearDataState()
    object Clearing : ClearDataState()
    object Success : ClearDataState()
    data class Error(val message: String) : ClearDataState()
}

