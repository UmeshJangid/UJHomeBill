package com.uj.homebill.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a monthly bill record
 */
@Entity(tableName = "bill_records")
data class BillRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val month: Int, // 1-12
    val year: Int,
    val totalBillAmount: Double,
    val totalBuildingUnits: Int,
    val ratePerUnit: Double,
    val totalFlatUnits: Int,
    val commonAreaUnits: Int,
    val commonAreaTotalCost: Double,
    val commonAreaCostPerFlat: Double,
    val isHindi: Boolean = false
)

/**
 * Represents individual flat reading for each bill record
 */
@Entity(
    tableName = "flat_readings",
    foreignKeys = [
        ForeignKey(
            entity = BillRecord::class,
            parentColumns = ["id"],
            childColumns = ["billRecordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["billRecordId"])]
)
data class FlatReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billRecordId: Long,
    val flatName: String,
    val flatNameHindi: String,
    val flatIndex: Int, // 0-4 for Kailash, Ajay, Manoj, Rakesh, Ramesh
    val previousReading: Int,
    val currentReading: Int,
    val unitsConsumed: Int,
    val individualCost: Double,
    val totalCost: Double // individual + common area share
)

/**
 * Stores the latest meter readings for each flat (used as previous reading next month)
 */
@Entity(tableName = "latest_readings")
data class LatestReading(
    @PrimaryKey
    val flatIndex: Int, // 0-4
    val flatName: String,
    val flatNameHindi: String,
    val lastReading: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * User settings and preferences
 */
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1, // Single row table
    val userName: String = "",
    val lastSavedDate: Long = 0,
    val totalBillsGenerated: Int = 0
)

/**
 * Yearly analytics summary
 */
data class YearlyAnalytics(
    val year: Int,
    val totalBillAmount: Double,
    val totalUnitsConsumed: Int,
    val averageMonthlyBill: Double,
    val averageRate: Double,
    val monthsRecorded: Int
)

/**
 * Monthly bill summary for display
 */
data class MonthlyBillSummary(
    val month: Int,
    val year: Int,
    val totalBillAmount: Double,
    val totalUnitsConsumed: Int,
    val ratePerUnit: Double,
    val timestamp: Long
)

