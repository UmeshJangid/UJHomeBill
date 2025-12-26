package com.uj.homebill.data.repository

import com.uj.homebill.data.database.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class BillRepository(private val billDao: BillDao) {
    
    // ==================== Latest Readings (Previous Month) ====================
    
    suspend fun getLatestReadings(): List<LatestReading> {
        return billDao.getAllLatestReadings()
    }
    
    fun getLatestReadingsFlow(): Flow<List<LatestReading>> {
        return billDao.getAllLatestReadingsFlow()
    }
    
    suspend fun getLatestReadingForFlat(flatIndex: Int): LatestReading? {
        return billDao.getLatestReadingByFlatIndex(flatIndex)
    }
    
    // ==================== Save Bill ====================
    
    suspend fun saveBillRecord(
        totalBillAmount: Double,
        totalBuildingUnits: Int,
        ratePerUnit: Double,
        totalFlatUnits: Int,
        commonAreaUnits: Int,
        commonAreaTotalCost: Double,
        commonAreaCostPerFlat: Double,
        flatReadings: List<FlatReadingData>,
        isHindi: Boolean
    ): Long {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        val year = calendar.get(Calendar.YEAR)
        
        // Create bill record
        val billRecord = BillRecord(
            month = month,
            year = year,
            totalBillAmount = totalBillAmount,
            totalBuildingUnits = totalBuildingUnits,
            ratePerUnit = ratePerUnit,
            totalFlatUnits = totalFlatUnits,
            commonAreaUnits = commonAreaUnits,
            commonAreaTotalCost = commonAreaTotalCost,
            commonAreaCostPerFlat = commonAreaCostPerFlat,
            isHindi = isHindi
        )
        
        val billRecordId = billDao.insertBillRecord(billRecord)
        
        // Create flat readings
        val flatReadingEntities = flatReadings.mapIndexed { index, data ->
            FlatReading(
                billRecordId = billRecordId,
                flatName = data.flatName,
                flatNameHindi = data.flatNameHindi,
                flatIndex = index,
                previousReading = data.previousReading,
                currentReading = data.currentReading,
                unitsConsumed = data.unitsConsumed,
                individualCost = data.individualCost,
                totalCost = data.totalCost
            )
        }
        billDao.insertFlatReadings(flatReadingEntities)
        
        // Update latest readings for next month
        val latestReadings = flatReadings.mapIndexed { index, data ->
            LatestReading(
                flatIndex = index,
                flatName = data.flatName,
                flatNameHindi = data.flatNameHindi,
                lastReading = data.currentReading
            )
        }
        billDao.insertLatestReadings(latestReadings)
        
        // Update user settings
        ensureUserSettingsExist()
        billDao.updateLastSavedDate(System.currentTimeMillis())
        
        return billRecordId
    }
    
    // ==================== Bill Records ====================
    
    fun getAllBillRecords(): Flow<List<BillRecord>> {
        return billDao.getAllBillRecords()
    }
    
    suspend fun getLatestBillRecord(): BillRecord? {
        return billDao.getLatestBillRecord()
    }
    
    suspend fun getFlatReadingsForBill(billRecordId: Long): List<FlatReading> {
        return billDao.getFlatReadingsByBillId(billRecordId)
    }
    
    // ==================== Analytics ====================
    
    fun getAvailableYears(): Flow<List<Int>> {
        return billDao.getAvailableYears()
    }
    
    suspend fun getYearlyAnalytics(year: Int): YearlyAnalytics? {
        return billDao.getYearlyAnalytics(year)
    }
    
    fun getAllYearlyAnalytics(): Flow<List<YearlyAnalytics>> {
        return billDao.getAllYearlyAnalytics()
    }
    
    fun getMonthlyBillSummaries(year: Int): Flow<List<MonthlyBillSummary>> {
        return billDao.getMonthlyBillSummaries(year)
    }
    
    // ==================== User Settings ====================
    
    suspend fun getUserSettings(): UserSettings? {
        return billDao.getUserSettings()
    }
    
    fun getUserSettingsFlow(): Flow<UserSettings?> {
        return billDao.getUserSettingsFlow()
    }
    
    suspend fun updateUserName(name: String) {
        ensureUserSettingsExist()
        billDao.updateUserName(name)
    }
    
    private suspend fun ensureUserSettingsExist() {
        if (billDao.getUserSettings() == null) {
            billDao.insertUserSettings(UserSettings())
        }
    }
    
    // ==================== Clear Data ====================
    
    suspend fun clearAllData() {
        billDao.clearAllData()
    }
}

/**
 * Data class to hold flat reading data before saving to database
 */
data class FlatReadingData(
    val flatName: String,
    val flatNameHindi: String,
    val previousReading: Int,
    val currentReading: Int,
    val unitsConsumed: Int,
    val individualCost: Double,
    val totalCost: Double
)

