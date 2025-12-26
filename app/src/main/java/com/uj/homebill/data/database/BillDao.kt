package com.uj.homebill.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    
    // ==================== Bill Records ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillRecord(billRecord: BillRecord): Long
    
    @Query("SELECT * FROM bill_records ORDER BY timestamp DESC")
    fun getAllBillRecords(): Flow<List<BillRecord>>
    
    @Query("SELECT * FROM bill_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBillRecord(): BillRecord?
    
    @Query("SELECT * FROM bill_records WHERE year = :year ORDER BY month ASC")
    fun getBillRecordsByYear(year: Int): Flow<List<BillRecord>>
    
    @Query("SELECT * FROM bill_records WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getBillRecordByMonthYear(month: Int, year: Int): BillRecord?
    
    @Query("SELECT DISTINCT year FROM bill_records ORDER BY year DESC")
    fun getAvailableYears(): Flow<List<Int>>
    
    @Delete
    suspend fun deleteBillRecord(billRecord: BillRecord)
    
    @Query("DELETE FROM bill_records")
    suspend fun deleteAllBillRecords()
    
    // ==================== Flat Readings ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlatReadings(flatReadings: List<FlatReading>)
    
    @Query("SELECT * FROM flat_readings WHERE billRecordId = :billRecordId ORDER BY flatIndex ASC")
    suspend fun getFlatReadingsByBillId(billRecordId: Long): List<FlatReading>
    
    @Query("SELECT * FROM flat_readings WHERE billRecordId = :billRecordId ORDER BY flatIndex ASC")
    fun getFlatReadingsByBillIdFlow(billRecordId: Long): Flow<List<FlatReading>>
    
    @Query("DELETE FROM flat_readings WHERE billRecordId = :billRecordId")
    suspend fun deleteFlatReadingsByBillId(billRecordId: Long)
    
    // ==================== Latest Readings ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLatestReadings(readings: List<LatestReading>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLatestReading(reading: LatestReading)
    
    @Query("SELECT * FROM latest_readings ORDER BY flatIndex ASC")
    suspend fun getAllLatestReadings(): List<LatestReading>
    
    @Query("SELECT * FROM latest_readings ORDER BY flatIndex ASC")
    fun getAllLatestReadingsFlow(): Flow<List<LatestReading>>
    
    @Query("SELECT * FROM latest_readings WHERE flatIndex = :flatIndex")
    suspend fun getLatestReadingByFlatIndex(flatIndex: Int): LatestReading?
    
    @Query("DELETE FROM latest_readings")
    suspend fun deleteAllLatestReadings()
    
    // ==================== User Settings ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(settings: UserSettings)
    
    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getUserSettings(): UserSettings?
    
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettingsFlow(): Flow<UserSettings?>
    
    @Query("UPDATE user_settings SET userName = :name WHERE id = 1")
    suspend fun updateUserName(name: String)
    
    @Query("UPDATE user_settings SET lastSavedDate = :date, totalBillsGenerated = totalBillsGenerated + 1 WHERE id = 1")
    suspend fun updateLastSavedDate(date: Long)
    
    // ==================== Analytics Queries ====================
    
    @Query("""
        SELECT 
            year,
            SUM(totalBillAmount) as totalBillAmount,
            SUM(totalFlatUnits) as totalUnitsConsumed,
            AVG(totalBillAmount) as averageMonthlyBill,
            AVG(ratePerUnit) as averageRate,
            COUNT(*) as monthsRecorded
        FROM bill_records 
        WHERE year = :year 
        GROUP BY year
    """)
    suspend fun getYearlyAnalytics(year: Int): YearlyAnalytics?
    
    @Query("""
        SELECT 
            month,
            year,
            totalBillAmount,
            totalFlatUnits as totalUnitsConsumed,
            ratePerUnit,
            timestamp
        FROM bill_records 
        WHERE year = :year 
        ORDER BY month ASC
    """)
    fun getMonthlyBillSummaries(year: Int): Flow<List<MonthlyBillSummary>>
    
    @Query("""
        SELECT 
            year,
            SUM(totalBillAmount) as totalBillAmount,
            SUM(totalFlatUnits) as totalUnitsConsumed,
            AVG(totalBillAmount) as averageMonthlyBill,
            AVG(ratePerUnit) as averageRate,
            COUNT(*) as monthsRecorded
        FROM bill_records 
        GROUP BY year
        ORDER BY year DESC
    """)
    fun getAllYearlyAnalytics(): Flow<List<YearlyAnalytics>>
    
    // ==================== Clear All Data ====================
    
    @Transaction
    suspend fun clearAllData() {
        deleteAllBillRecords() // This will cascade delete flat_readings
        deleteAllLatestReadings()
    }
}

