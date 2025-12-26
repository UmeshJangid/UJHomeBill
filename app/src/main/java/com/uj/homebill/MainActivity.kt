package com.uj.homebill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import android.os.Build
import androidx.core.content.FileProvider
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.uj.homebill.ui.theme.HomeBillTheme
import com.uj.homebill.viewmodel.BillViewModel
import com.uj.homebill.viewmodel.SaveState
import com.uj.homebill.data.repository.FlatReadingData
import com.uj.homebill.screens.SettingsScreen

// Data class for individual flat with previous reading
data class Flat(
    val name: String = "",
    val nameHindi: String = "",
    val previousUnits: String = "0",
    val currentUnits: String = ""
) {
    val unitsConsumed: Int
        get() {
            val prev = previousUnits.toIntOrNull() ?: 0
            val curr = currentUnits.toIntOrNull() ?: 0
            return if (curr >= prev) curr - prev else 0
        }
}

// Data class for other common bills (water, maintenance, etc.)
data class OtherBill(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val amount: String = ""
) {
    val amountValue: Double
        get() = amount.toDoubleOrNull() ?: 0.0
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomeBillTheme {
                val navController = rememberNavController()
                val viewModel: BillViewModel = viewModel()
                
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            HomeBillScreen(
                                viewModel = viewModel,
                                navController = navController
                            )
                        }
                    }
                    composable("settings/{isHindi}") { backStackEntry ->
                        val isHindi = backStackEntry.arguments?.getString("isHindi")?.toBoolean() ?: false
                        SettingsScreen(
                            viewModel = viewModel,
                            navController = navController,
                            isHindi = isHindi
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBillScreen(
    viewModel: BillViewModel = viewModel(),
    navController: NavHostController? = null
) {
    val scrollState = rememberScrollState()
    
    // State for input values
    var monthlyBillAmount by remember { mutableStateOf("") }
    var buildingUnits by remember { mutableStateOf("") }
    var ratePerUnit by remember { mutableStateOf("0.00") }
    
    // State for other common bills (water, maintenance, etc.)
    var otherBills by remember { mutableStateOf(listOf<OtherBill>()) }
    var showAddBillDialog by remember { mutableStateOf(false) }
    var newBillName by remember { mutableStateOf("") }
    var newBillAmount by remember { mutableStateOf("") }
    
    // Language and UI state
    var isHindi by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }
    var showSaveSuccessDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Collect latest readings from database
    val latestReadings by viewModel.latestReadings.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    
    // Flat names in both languages
    val flatNamesEnglish = listOf("Kailash", "Ajay", "Manoj", "Rakesh", "Ramesh")
    val flatNamesHindi = listOf("कैलाश", "अजय", "मनोज", "राकेश", "रमेश")
    
    // State for flats with previous readings from database
    var flats by remember { mutableStateOf(
        flatNamesEnglish.mapIndexed { index, name ->
            Flat(
                name = name,
                nameHindi = flatNamesHindi[index],
                previousUnits = "0",
                currentUnits = ""
            )
        }
    )}
    
    // Load previous readings from database when available
    LaunchedEffect(latestReadings) {
        if (latestReadings.isNotEmpty()) {
            flats = flats.mapIndexed { index, flat ->
                val savedReading = latestReadings.find { it.flatIndex == index }
                flat.copy(
                    previousUnits = savedReading?.lastReading?.toString() ?: "0"
                )
            }
        }
    }
    
    // Update flat display names when language changes
    val displayFlats = flats.mapIndexed { index, flat ->
        flat.copy(
            name = if (isHindi) flatNamesHindi[index] else flatNamesEnglish[index]
        )
    }
    
    // Calculate cost per unit automatically from overall bill
    val costPerUnit = remember(monthlyBillAmount, buildingUnits) {
        val bill = monthlyBillAmount.toDoubleOrNull()
        val units = buildingUnits.toDoubleOrNull()
        if (bill != null && units != null && units > 0) {
            val rate = bill / units
            ratePerUnit = String.format("%.2f", rate)
            "₹${String.format("%.2f", rate)}/kWh"
        } else {
            ratePerUnit = "0.00"
            "₹0.00/kWh"
        }
    }
    
    // Calculate totals and common area
    val totalFlatUnits = flats.sumOf { it.unitsConsumed }
    val totalBuildingUnits = buildingUnits.toIntOrNull() ?: 0
    val commonAreaUnits = if (totalBuildingUnits > totalFlatUnits) totalBuildingUnits - totalFlatUnits else 0
    val rate = ratePerUnit.toDoubleOrNull() ?: 0.0
    val commonAreaTotalCost = commonAreaUnits * rate
    val commonAreaCostPerFlat = if (flats.isNotEmpty()) commonAreaTotalCost / flats.size else 0.0
    
    // Calculate other bills total and per flat share
    val otherBillsTotal = otherBills.sumOf { it.amountValue }
    val otherBillsPerFlat = if (flats.isNotEmpty() && otherBillsTotal > 0) otherBillsTotal / flats.size else 0.0
    
    // Total additional cost per flat (common area + other bills)
    val totalAdditionalCostPerFlat = commonAreaCostPerFlat + otherBillsPerFlat
    
    // Handle save state changes
    LaunchedEffect(saveState) {
        when (saveState) {
            is SaveState.Success -> {
                showSaveSuccessDialog = true
                viewModel.resetSaveState()
            }
            is SaveState.Error -> {
                Toast.makeText(context, (saveState as SaveState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }
    
    // Function to save and share
    fun saveAndShare() {
        // Validate inputs
        if (monthlyBillAmount.isBlank() || buildingUnits.isBlank()) {
            Toast.makeText(
                context,
                if (isHindi) "कृपया कुल बिल और यूनिट्स दर्ज करें" else "Please enter total bill and units",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        if (flats.all { it.currentUnits.isBlank() }) {
            Toast.makeText(
                context,
                if (isHindi) "कृपया कम से कम एक फ्लैट का यूनिट दर्ज करें" else "Please enter units for at least one flat",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Prepare flat reading data
        val flatReadingsData = flats.mapIndexed { index, flat ->
            val unitsConsumed = flat.unitsConsumed
            val individualCost = unitsConsumed * rate
            val totalCost = individualCost + totalAdditionalCostPerFlat
            
            FlatReadingData(
                flatName = flatNamesEnglish[index],
                flatNameHindi = flatNamesHindi[index],
                previousReading = flat.previousUnits.toIntOrNull() ?: 0,
                currentReading = flat.currentUnits.toIntOrNull() ?: 0,
                unitsConsumed = unitsConsumed,
                individualCost = individualCost,
                totalCost = totalCost
            )
        }
        
        // Save to database
        viewModel.saveBillRecord(
            totalBillAmount = monthlyBillAmount.toDoubleOrNull() ?: 0.0,
            totalBuildingUnits = totalBuildingUnits,
            ratePerUnit = rate,
            totalFlatUnits = totalFlatUnits,
            commonAreaUnits = commonAreaUnits,
            commonAreaTotalCost = commonAreaTotalCost,
            commonAreaCostPerFlat = commonAreaCostPerFlat,
            flatReadings = flatReadingsData,
            isHindi = isHindi,
            onSuccess = {
                // Share after successful save
                shareElectricityBill(context, monthlyBillAmount, buildingUnits, displayFlats, commonAreaCostPerFlat, otherBills, otherBillsPerFlat, rate, isHindi)
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
    ) {
        // Header Section with Solid Blue
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row with Language Toggle and Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Language Toggle
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { isHindi = !isHindi },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Language",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "EN" else "हिं",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Settings Button
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            IconButton(
                                onClick = { navController?.navigate("settings/$isHindi") }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = if (isHindi) "सेटिंग्स" else "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        
                        // PDF Button
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            IconButton(
                                onClick = { 
                                    if (monthlyBillAmount.isBlank() || buildingUnits.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            if (isHindi) "कृपया कुल बिल और यूनिट्स दर्ज करें" else "Please enter total bill and units",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else if (flats.all { it.currentUnits.isBlank() }) {
                                        Toast.makeText(
                                            context,
                                            if (isHindi) "कृपया कम से कम एक फ्लैट का यूनिट दर्ज करें" else "Please enter units for at least one flat",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        generatePdfBill(context, monthlyBillAmount, buildingUnits, displayFlats, commonAreaCostPerFlat, otherBills, otherBillsPerFlat, rate, isHindi)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = if (isHindi) "PDF सेव करें" else "Save PDF",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    
                        // Share Button
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            IconButton(
                                onClick = { showSaveConfirmDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = if (isHindi) "बिल शेयर करें" else "Share Bill",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        
                        // Reset Button
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            IconButton(
                                onClick = { showResetDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = if (isHindi) "फॉर्म रीसेट करें" else "Reset Form",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Centered App Title and Icon
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Electric Icon",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "UJHomeBill",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = if (isHindi) "अपनी बिजली की खपत को ट्रैक करें" else "Track your electricity consumption",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Main Content Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            
            // Input Cards for Bill Data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InputStatCard(
                    title = if (isHindi) "इस महीने का बिल" else "This Month Bill",
                    value = monthlyBillAmount,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d{0,7}(\\.\\d{0,2})?$")) || newValue.isEmpty()) {
                            monthlyBillAmount = newValue
                        }
                    },
                    icon = Icons.Default.Info,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    placeholder = "₹0.00",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                
                InputStatCard(
                    title = if (isHindi) "कुल बिल्डिंग यूनिट्स" else "Total Building Units",
                    value = buildingUnits,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d{0,6}$")) || newValue.isEmpty()) {
                            buildingUnits = newValue
                        }
                    },
                    icon = Icons.Default.Edit,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    placeholder = "0 kWh",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Rate Display Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Rate",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isHindi) "दर प्रति यूनिट" else "Rate per Unit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "₹$ratePerUnit/kWh",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isHindi) "स्वचालित गणना" else "Auto-calculated",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Flats Section with Previous and Current Reading
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Flats",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = if (isHindi) "व्यक्तिगत फ्लैट रीडिंग" else "Individual Flat Readings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Header row for columns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isHindi) "फ्लैट" else "Flat",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.width(70.dp)
                        )
                        Text(
                            text = if (isHindi) "पिछला" else "Previous",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isHindi) "वर्तमान" else "Current",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    flats.forEachIndexed { index, flat ->
                        FlatInputRow(
                            flat = flat.copy(
                                name = if (isHindi) flatNamesHindi[index] else flatNamesEnglish[index]
                            ),
                            onFlatChange = { newFlat ->
                                flats = flats.toMutableList().apply {
                                    this[index] = newFlat.copy(
                                        name = flatNamesEnglish[index],
                                        nameHindi = flatNamesHindi[index]
                                    )
                                }
                            },
                            rate = rate,
                            commonAreaCostPerFlat = totalAdditionalCostPerFlat,
                            isHindi = isHindi
                        )
                        
                        if (index < flats.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Other Common Bills Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Other Bills",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = if (isHindi) "अन्य सामान्य बिल" else "Other Common Bills",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isHindi) "पानी, मेंटेनेंस आदि (5 फ्लैट में बांटा जाएगा)" else "Water, Maintenance etc. (divided by 5 flats)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        
                        // Add Bill Button
                        FilledTonalButton(
                            onClick = { showAddBillDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "जोड़ें" else "Add",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    
                    if (otherBills.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        otherBills.forEach { bill ->
                            OtherBillRow(
                                bill = bill,
                                numberOfFlats = flats.size,
                                onRemove = {
                                    otherBills = otherBills.filter { it.id != bill.id }
                                },
                                isHindi = isHindi
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Total Other Bills Summary
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHindi) "कुल अन्य बिल:" else "Total Other Bills:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "₹${String.format("%.2f", otherBillsTotal)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHindi) "प्रति फ्लैट:" else "Per Flat:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "₹${String.format("%.2f", otherBillsPerFlat)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHindi) "कोई अन्य बिल नहीं जोड़ा गया" else "No other bills added",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Summary Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Summary",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = if (isHindi) "बिल सारांश" else "Bill Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Building Units Summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (isHindi) "यूनिट्स विवरण" else "Units Breakdown",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            SummaryRow(
                                if (isHindi) "कुल फ्लैट यूनिट्स:" else "Total Flat Units:", 
                                "$totalFlatUnits kWh"
                            )
                            SummaryRow(
                                if (isHindi) "बिल्डिंग कुल यूनिट्स:" else "Building Total Units:", 
                                "$totalBuildingUnits kWh"
                            ) 
                            SummaryRow(
                                if (isHindi) "कॉमन एरिया यूनिट्स:" else "Common Area Units:", 
                                "$commonAreaUnits kWh"
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Cost Summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (isHindi) "अतिरिक्त खर्च (प्रति फ्लैट)" else "Additional Costs (Per Flat)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            SummaryRow(
                                if (isHindi) "कॉमन एरिया बिजली:" else "Common Area Electricity:", 
                                "₹${String.format("%.2f", commonAreaCostPerFlat)}"
                            )
                            
                            if (otherBillsTotal > 0) {
                                SummaryRow(
                                    if (isHindi) "अन्य बिल शेयर:" else "Other Bills Share:", 
                                    "₹${String.format("%.2f", otherBillsPerFlat)}"
                                )
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            SummaryRow(
                                if (isHindi) "कुल अतिरिक्त प्रति फ्लैट:" else "Total Additional per Flat:", 
                                "₹${String.format("%.2f", totalAdditionalCostPerFlat)}", 
                                isHighlight = true
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = if (isHindi) 
                                    "नोट: हर फ्लैट का अंतिम बिल = बिजली खपत + कॉमन एरिया + अन्य बिल" 
                                else 
                                    "Note: Each flat's final bill = Electricity usage + Common area + Other bills",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Add Bill Dialog
    if (showAddBillDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddBillDialog = false
                newBillName = ""
                newBillAmount = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "नया बिल जोड़ें" else "Add New Bill",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = if (isHindi) 
                            "यह राशि सभी 5 फ्लैट में समान रूप से बांटी जाएगी।" 
                        else 
                            "This amount will be divided equally among all 5 flats.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = newBillName,
                        onValueChange = { newBillName = it },
                        label = { Text(if (isHindi) "बिल का नाम" else "Bill Name") },
                        placeholder = { Text(if (isHindi) "जैसे: पानी, मेंटेनेंस" else "e.g., Water, Maintenance") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = newBillAmount,
                        onValueChange = { newValue ->
                            if (newValue.matches(Regex("^\\d{0,7}(\\.\\d{0,2})?$")) || newValue.isEmpty()) {
                                newBillAmount = newValue
                            }
                        },
                        label = { Text(if (isHindi) "कुल राशि (₹)" else "Total Amount (₹)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("₹") }
                    )
                    
                    if (newBillAmount.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val perFlat = (newBillAmount.toDoubleOrNull() ?: 0.0) / flats.size
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isHindi) "प्रति फ्लैट:" else "Per Flat:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "₹${String.format("%.2f", perFlat)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBillName.isNotBlank() && newBillAmount.isNotBlank()) {
                            otherBills = otherBills + OtherBill(
                                name = newBillName.trim(),
                                amount = newBillAmount
                            )
                            showAddBillDialog = false
                            newBillName = ""
                            newBillAmount = ""
                        }
                    },
                    enabled = newBillName.isNotBlank() && newBillAmount.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isHindi) "जोड़ें" else "Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAddBillDialog = false
                        newBillName = ""
                        newBillAmount = ""
                    }
                ) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
    
    // Save Confirmation Dialog
    if (showSaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "सेव और शेयर करें" else "Save & Share",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = if (isHindi) 
                            "शेयर करने पर यह डेटा डेटाबेस में सेव हो जाएगा और अगले महीने के लिए 'पिछला यूनिट' के रूप में उपयोग होगा।" 
                        else 
                            "On sharing, this data will be saved to database and will be used as 'Previous Units' for next month.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) 
                                    "⚠️ सुनिश्चित करें कि सभी रीडिंग सही हैं, अन्यथा अगले महीने की गणना गलत हो सकती है!" 
                                else 
                                    "⚠️ Make sure all readings are correct, otherwise next month's calculation may be wrong!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveConfirmDialog = false
                        saveAndShare()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isHindi) "सेव और शेयर करें" else "Save & Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirmDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
    
    // Save Success Dialog
    if (showSaveSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSaveSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "सफलतापूर्वक सेव हुआ!" else "Saved Successfully!",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            },
            text = {
                Text(
                    text = if (isHindi) 
                        "सभी रीडिंग डेटाबेस में सेव हो गई हैं। अगले महीने में वर्तमान रीडिंग स्वचालित रूप से 'पिछला यूनिट' के रूप में दिखाई देंगी।" 
                    else 
                        "All readings have been saved to database. Next month, current readings will automatically appear as 'Previous Units'.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { showSaveSuccessDialog = false }) {
                    Text(if (isHindi) "ठीक है" else "OK")
                }
            }
        )
    }
    
    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = if (isHindi) "फॉर्म रीसेट करें" else "Reset Form",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = if (isHindi) 
                        "क्या आप वाकई सभी फील्ड्स को रीसेट करना चाहते हैं? यह केवल वर्तमान रीडिंग साफ़ करेगा, पिछली रीडिंग बनी रहेंगी।" 
                    else 
                        "Are you sure you want to reset all fields? This will only clear current readings, previous readings will remain.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        monthlyBillAmount = ""
                        buildingUnits = ""
                        ratePerUnit = "0.00"
                        flats = flats.map { it.copy(currentUnits = "") }
                        otherBills = emptyList()
                        showResetDialog = false
                        
                        Toast.makeText(
                            context,
                            if (isHindi) "फॉर्म रीसेट हो गया" else "Form reset successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Text(
                        text = if (isHindi) "रीसेट करें" else "Reset",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(
                        text = if (isHindi) "रद्द करें" else "Cancel",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

@Composable
fun InputStatCard(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    color: Color,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                    cursorColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun FlatInputRow(
    flat: Flat,
    onFlatChange: (Flat) -> Unit,
    rate: Double,
    commonAreaCostPerFlat: Double = 0.0,
    isHindi: Boolean = false
) {
    val unitsConsumed = flat.unitsConsumed
    val flatIndividualCost = unitsConsumed * rate
    val flatTotalCost = flatIndividualCost + commonAreaCostPerFlat
    val hasWarning = flat.currentUnits.isNotBlank() && 
                     (flat.currentUnits.toIntOrNull() ?: 0) < (flat.previousUnits.toIntOrNull() ?: 0)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasWarning) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Input row: Name, Previous, Current
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Flat name
                Text(
                    text = flat.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(70.dp)
                )
                
                // Previous reading (editable)
                OutlinedTextField(
                    value = flat.previousUnits,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d{0,6}$")) || newValue.isEmpty()) {
                            onFlatChange(flat.copy(previousUnits = newValue.ifEmpty { "0" }))
                        }
                    },
                    placeholder = { 
                        Text(
                            text = "0",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                )
                
                // Current reading
                OutlinedTextField(
                    value = flat.currentUnits,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d{0,6}$")) || newValue.isEmpty()) {
                            onFlatChange(flat.copy(currentUnits = newValue))
                        }
                    },
                    placeholder = { 
                        Text(
                            text = "0",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                )
            }
            
            // Warning if current < previous
            if (hasWarning) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isHindi) "वर्तमान < पिछला (मीटर बदला?)" else "Current < Previous (meter replaced?)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Cost calculation (only show if current units entered)
            if (flat.currentUnits.isNotBlank() && unitsConsumed > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "खपत: $unitsConsumed kWh" else "Consumed: $unitsConsumed kWh",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "(${flat.currentUnits} - ${flat.previousUnits})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "₹${String.format("%.2f", flatTotalCost)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "(₹${String.format("%.0f", flatIndividualCost)} + ₹${String.format("%.0f", commonAreaCostPerFlat)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isHighlight) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = value,
            style = if (isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun OtherBillRow(
    bill: OtherBill,
    numberOfFlats: Int,
    onRemove: () -> Unit,
    isHindi: Boolean
) {
    val perFlat = if (numberOfFlats > 0) bill.amountValue / numberOfFlats else 0.0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row {
                    Text(
                        text = "₹${bill.amount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = " → ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "₹${String.format("%.2f", perFlat)} ${if (isHindi) "प्रति फ्लैट" else "per flat"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = if (isHindi) "हटाएं" else "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ==================== PDF Generation ====================

fun generatePdfBill(
    context: Context,
    totalBill: String,
    totalUnits: String,
    flats: List<Flat>,
    commonAreaCostPerFlat: Double,
    otherBills: List<OtherBill>,
    otherBillsPerFlat: Double,
    rate: Double,
    isHindi: Boolean
) {
    val totalFlatUnits = flats.sumOf { it.unitsConsumed }
    val buildingUnits = totalUnits.toIntOrNull() ?: 0
    val commonAreaUnits = if (buildingUnits > totalFlatUnits) buildingUnits - totalFlatUnits else 0
    val totalAdditionalPerFlat = commonAreaCostPerFlat + otherBillsPerFlat
    
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val titlePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val headingPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val normalPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        
        val smallPaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        
        var yPosition = 60f
        
        val title = if (isHindi) "UJHomeBill - बिजली बिल रिपोर्ट" else "UJHomeBill - Electricity Bill Report"
        canvas.drawText(title, 50f, yPosition, titlePaint)
        yPosition += 40f
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        canvas.drawText(if (isHindi) "दिनांक: $currentDate" else "Date: $currentDate", 50f, yPosition, normalPaint)
        yPosition += 30f
        
        canvas.drawLine(50f, yPosition, 545f, yPosition, normalPaint)
        yPosition += 30f
        
        canvas.drawText(if (isHindi) "बिल सारांश" else "Bill Summary", 50f, yPosition, headingPaint)
        yPosition += 25f
        
        canvas.drawText(if (isHindi) "कुल मासिक बिल: ₹$totalBill" else "Total Monthly Bill: ₹$totalBill", 70f, yPosition, normalPaint)
        yPosition += 20f
        canvas.drawText(if (isHindi) "कुल यूनिट्स: $totalUnits kWh" else "Total Units: $totalUnits kWh", 70f, yPosition, normalPaint)
        yPosition += 20f
        canvas.drawText(if (isHindi) "दर प्रति यूनिट: ₹${String.format("%.2f", rate)}/kWh" else "Rate per Unit: ₹${String.format("%.2f", rate)}/kWh", 70f, yPosition, normalPaint)
        yPosition += 30f
        
        canvas.drawText(if (isHindi) "यूनिट्स विवरण" else "Units Breakdown", 50f, yPosition, headingPaint)
        yPosition += 25f
        
        canvas.drawText(if (isHindi) "कुल फ्लैट यूनिट्स: $totalFlatUnits kWh" else "Total Flat Units: $totalFlatUnits kWh", 70f, yPosition, normalPaint)
        yPosition += 20f
        canvas.drawText(if (isHindi) "बिल्डिंग कुल यूनिट्स: $buildingUnits kWh" else "Building Total Units: $buildingUnits kWh", 70f, yPosition, normalPaint)
        yPosition += 20f
        canvas.drawText(if (isHindi) "कॉमन एरिया यूनिट्स: $commonAreaUnits kWh" else "Common Area Units: $commonAreaUnits kWh", 70f, yPosition, normalPaint)
        yPosition += 30f
        
        // Other Bills Section
        if (otherBills.isNotEmpty()) {
            canvas.drawText(if (isHindi) "अन्य सामान्य बिल" else "Other Common Bills", 50f, yPosition, headingPaint)
            yPosition += 25f
            
            otherBills.forEach { bill ->
                val perFlat = bill.amountValue / flats.size
                canvas.drawText("${bill.name}: ₹${bill.amount} (₹${String.format("%.2f", perFlat)} ${if (isHindi) "प्रति फ्लैट" else "per flat"})", 70f, yPosition, normalPaint)
                yPosition += 20f
            }
            
            canvas.drawText(if (isHindi) "कुल अन्य बिल प्रति फ्लैट: ₹${String.format("%.2f", otherBillsPerFlat)}" else "Total Other Bills per Flat: ₹${String.format("%.2f", otherBillsPerFlat)}", 70f, yPosition, normalPaint)
            yPosition += 30f
        }
        
        canvas.drawText(if (isHindi) "फ्लैट-वार विवरण" else "Flat-wise Details", 50f, yPosition, headingPaint)
        yPosition += 25f
        
        flats.forEach { flat ->
            if (flat.currentUnits.isNotBlank()) {
                val unitsConsumed = flat.unitsConsumed
                val individualCost = unitsConsumed * rate
                val totalCost = individualCost + totalAdditionalPerFlat
                
                canvas.drawText("${flat.name}:", 70f, yPosition, normalPaint)
                yPosition += 15f
                canvas.drawText(if (isHindi) "  पिछला: ${flat.previousUnits} | वर्तमान: ${flat.currentUnits} | खपत: $unitsConsumed kWh" else "  Previous: ${flat.previousUnits} | Current: ${flat.currentUnits} | Consumed: $unitsConsumed kWh", 90f, yPosition, smallPaint)
                yPosition += 15f
                canvas.drawText(if (isHindi) "  व्यक्तिगत कॉस्ट: ₹${String.format("%.2f", individualCost)}" else "  Individual Cost: ₹${String.format("%.2f", individualCost)}", 90f, yPosition, smallPaint)
                yPosition += 15f
                canvas.drawText(if (isHindi) "  अतिरिक्त शेयर: ₹${String.format("%.2f", totalAdditionalPerFlat)} (कॉमन + अन्य)" else "  Additional Share: ₹${String.format("%.2f", totalAdditionalPerFlat)} (Common + Others)", 90f, yPosition, smallPaint)
                yPosition += 15f
                canvas.drawText(if (isHindi) "  कुल बिल: ₹${String.format("%.2f", totalCost)}" else "  Total Bill: ₹${String.format("%.2f", totalCost)}", 90f, yPosition, normalPaint)
                yPosition += 25f
            }
        }
        
        yPosition += 10f
        canvas.drawText(if (isHindi) "अतिरिक्त खर्च सारांश" else "Additional Costs Summary", 50f, yPosition, headingPaint)
        yPosition += 25f
        
        val commonAreaTotalCost = commonAreaUnits * rate
        canvas.drawText(if (isHindi) "कॉमन एरिया बिजली: ₹${String.format("%.2f", commonAreaTotalCost)} (₹${String.format("%.2f", commonAreaCostPerFlat)} प्रति फ्लैट)" else "Common Area Electricity: ₹${String.format("%.2f", commonAreaTotalCost)} (₹${String.format("%.2f", commonAreaCostPerFlat)} per flat)", 70f, yPosition, normalPaint)
        yPosition += 20f
        if (otherBills.isNotEmpty()) {
            val otherBillsTotal = otherBills.sumOf { it.amountValue }
            canvas.drawText(if (isHindi) "अन्य बिल: ₹${String.format("%.2f", otherBillsTotal)} (₹${String.format("%.2f", otherBillsPerFlat)} प्रति फ्लैट)" else "Other Bills: ₹${String.format("%.2f", otherBillsTotal)} (₹${String.format("%.2f", otherBillsPerFlat)} per flat)", 70f, yPosition, normalPaint)
            yPosition += 20f
        }
        canvas.drawText(if (isHindi) "कुल अतिरिक्त प्रति फ्लैट: ₹${String.format("%.2f", totalAdditionalPerFlat)}" else "Total Additional per Flat: ₹${String.format("%.2f", totalAdditionalPerFlat)}", 70f, yPosition, normalPaint)
        yPosition += 30f
        
        yPosition += 20f
        canvas.drawText(if (isHindi) "नोट: हर फ्लैट का अंतिम बिल = बिजली खपत + कॉमन एरिया + अन्य बिल" else "Note: Each flat's final bill = Electricity usage + Common area + Other bills", 50f, yPosition, smallPaint)
        
        pdfDocument.finishPage(page)
        
        val fileName = "UJHomeBill_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
        
        val pdfDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "UJHomeBill")
        } else {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "UJHomeBill")
        }
        
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }
        val file = File(pdfDir, fileName)
        
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        
        val shortPath = "UJHomeBill/$fileName"
        Toast.makeText(context, if (isHindi) "PDF सेव किया गया: $shortPath" else "PDF saved: $shortPath", Toast.LENGTH_LONG).show()
        
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, if (isHindi) "PDF खोलने में त्रुटि: ${e.message}" else "Error opening PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
        
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, if (isHindi) "PDF बनाने में त्रुटि" else "Error creating PDF", Toast.LENGTH_SHORT).show()
    }
}

// ==================== Share Function ====================

fun shareElectricityBill(
    context: Context,
    totalBill: String,
    totalUnits: String,
    flats: List<Flat>,
    commonAreaCostPerFlat: Double,
    otherBills: List<OtherBill>,
    otherBillsPerFlat: Double,
    rate: Double,
    isHindi: Boolean
) {
    val totalFlatUnits = flats.sumOf { it.unitsConsumed }
    val buildingUnits = totalUnits.toIntOrNull() ?: 0
    val commonAreaUnits = if (buildingUnits > totalFlatUnits) buildingUnits - totalFlatUnits else 0
    val totalAdditionalPerFlat = commonAreaCostPerFlat + otherBillsPerFlat
    
    val shareText = if (isHindi) {
        buildString {
            appendLine("🏠 UJHomeBill - बिजली बिल सारांश")
            appendLine("=========================================")
            appendLine()
            appendLine("📊 यूनिट्स विवरण:")
            appendLine("• कुल फ्लैट यूनिट्स: $totalFlatUnits kWh")
            appendLine("• बिल्डिंग कुल यूनिट्स: $buildingUnits kWh") 
            appendLine("• कॉमन एरिया यूनिट्स: $commonAreaUnits kWh")
            appendLine("• दर प्रति यूनिट: ₹${String.format("%.2f", rate)}/kWh")
            appendLine()
            
            // Other bills section
            if (otherBills.isNotEmpty()) {
                appendLine("📋 अन्य सामान्य बिल:")
                otherBills.forEach { bill ->
                    val perFlat = bill.amountValue / flats.size
                    appendLine("• ${bill.name}: ₹${bill.amount} (₹${String.format("%.2f", perFlat)}/फ्लैट)")
                }
                appendLine()
            }
            
            appendLine("💰 फ्लैट-वार बिल:")
            flats.forEach { flat ->
                if (flat.currentUnits.isNotBlank()) {
                    val unitsConsumed = flat.unitsConsumed
                    val individualCost = unitsConsumed * rate
                    val totalCost = individualCost + totalAdditionalPerFlat
                    appendLine("${flat.name}: ${flat.previousUnits}→${flat.currentUnits} = $unitsConsumed kWh")
                    appendLine("   ₹${String.format("%.2f", totalCost)} (₹${String.format("%.0f", individualCost)} + ₹${String.format("%.0f", totalAdditionalPerFlat)})")
                }
            }
            appendLine()
            appendLine("🏢 अतिरिक्त खर्च (प्रति फ्लैट):")
            appendLine("• कॉमन एरिया बिजली: ₹${String.format("%.2f", commonAreaCostPerFlat)}")
            if (otherBills.isNotEmpty()) {
                appendLine("• अन्य बिल: ₹${String.format("%.2f", otherBillsPerFlat)}")
            }
            appendLine("• कुल अतिरिक्त: ₹${String.format("%.2f", totalAdditionalPerFlat)}")
            appendLine()
            appendLine("💡 नोट: हर फ्लैट का अंतिम बिल = बिजली खपत + कॉमन एरिया + अन्य बिल")
        }
    } else {
        buildString {
            appendLine("🏠 UJHomeBill - Electricity Bill Summary")
            appendLine("=========================================")
            appendLine()
            appendLine("📊 Units Breakdown:")
            appendLine("• Total Flat Units: $totalFlatUnits kWh")
            appendLine("• Building Total Units: $buildingUnits kWh") 
            appendLine("• Common Area Units: $commonAreaUnits kWh")
            appendLine("• Rate per Unit: ₹${String.format("%.2f", rate)}/kWh")
            appendLine()
            
            // Other bills section
            if (otherBills.isNotEmpty()) {
                appendLine("📋 Other Common Bills:")
                otherBills.forEach { bill ->
                    val perFlat = bill.amountValue / flats.size
                    appendLine("• ${bill.name}: ₹${bill.amount} (₹${String.format("%.2f", perFlat)}/flat)")
                }
                appendLine()
            }
            
            appendLine("💰 Flat-wise Bills:")
            flats.forEach { flat ->
                if (flat.currentUnits.isNotBlank()) {
                    val unitsConsumed = flat.unitsConsumed
                    val individualCost = unitsConsumed * rate
                    val totalCost = individualCost + totalAdditionalPerFlat
                    appendLine("${flat.name}: ${flat.previousUnits}→${flat.currentUnits} = $unitsConsumed kWh")
                    appendLine("   ₹${String.format("%.2f", totalCost)} (₹${String.format("%.0f", individualCost)} + ₹${String.format("%.0f", totalAdditionalPerFlat)})")
                }
            }
            appendLine()
            appendLine("🏢 Additional Costs (Per Flat):")
            appendLine("• Common Area Electricity: ₹${String.format("%.2f", commonAreaCostPerFlat)}")
            if (otherBills.isNotEmpty()) {
                appendLine("• Other Bills: ₹${String.format("%.2f", otherBillsPerFlat)}")
            }
            appendLine("• Total Additional: ₹${String.format("%.2f", totalAdditionalPerFlat)}")
            appendLine()
            appendLine("💡 Note: Each flat's final bill = Electricity usage + Common area + Other bills")
        }
    }
    
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    
    val chooser = Intent.createChooser(intent, if (isHindi) "बिल शेयर करें" else "Share Bill")
    context.startActivity(chooser)
}
