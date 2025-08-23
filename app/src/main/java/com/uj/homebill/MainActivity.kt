package com.uj.homebill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import android.os.Build
import androidx.core.content.FileProvider
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.uj.homebill.ui.theme.HomeBillTheme

// Data class for individual flat
data class Flat(
    val name: String = "",
    val units: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomeBillTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeBillScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBillScreen() {
    val scrollState = rememberScrollState()
    
    // State for input values
    var monthlyBillAmount by remember { mutableStateOf("") }
    var unitsUsed by remember { mutableStateOf("") }
    var ratePerUnit by remember { mutableStateOf("0.00") }
    
    // Language and UI state
    var isHindi by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // State for flats with string resources
    val flatNames = if (isHindi) {
        listOf("कैलाश", "अजय", "मनोज", "राकेश", "रमेश")
    } else {
        listOf(
            stringResource(R.string.flat_kailash),
            stringResource(R.string.flat_ajay), 
            stringResource(R.string.flat_manoj),
            stringResource(R.string.flat_rakesh),
            stringResource(R.string.flat_ramesh)
        )
    }
    
    var flats by remember { mutableStateOf(listOf(
        Flat(flatNames[0], ""),
        Flat(flatNames[1], ""),
        Flat(flatNames[2], ""),
        Flat(flatNames[3], ""),
        Flat(flatNames[4], "")
    )) }
    
    // Update flat names when language changes
    flats = flats.mapIndexed { index, flat ->
        flat.copy(name = flatNames[index])
    }
    
    // Calculate cost per unit automatically from overall bill
    val costPerUnit = remember(monthlyBillAmount, unitsUsed) {
        val bill = monthlyBillAmount.toDoubleOrNull()
        val units = unitsUsed.toDoubleOrNull()
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
    val totalFlatUnits = flats.sumOf { it.units.toIntOrNull() ?: 0 }
    val totalBuildingUnits = unitsUsed.toIntOrNull() ?: 0
    val commonAreaUnits = if (totalBuildingUnits > totalFlatUnits) totalBuildingUnits - totalFlatUnits else 0
    val rate = ratePerUnit.toDoubleOrNull() ?: 0.0
    val commonAreaTotalCost = commonAreaUnits * rate
    val commonAreaCostPerFlat = if (flats.isNotEmpty()) commonAreaTotalCost / flats.size else 0.0
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Header Section with Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Toggle
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { isHindi = !isHindi }
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Language",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHindi) "EN" else "हिं",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // App Title and Icon
                Column(
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
                        text = if (isHindi) "अपनी बिजली की खपत को ट्रैक करें" else stringResource(R.string.track_consumption),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
                
                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { 
                            // Validate inputs before generating PDF
                            if (monthlyBillAmount.isBlank() || unitsUsed.isBlank()) {
                                Toast.makeText(
                                    context,
                                    if (isHindi) "कृपया कुल बिल और यूनिट्स दर्ज करें" else "Please enter total bill and units",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (flats.all { it.units.isBlank() }) {
                                Toast.makeText(
                                    context,
                                    if (isHindi) "कृपया कम से कम एक फ्लैट का यूनिट दर्ज करें" else "Please enter units for at least one flat",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                generatePdfBill(context, monthlyBillAmount, unitsUsed, flats, commonAreaCostPerFlat, isHindi)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = if (isHindi) "PDF सेव करें" else "Save PDF",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { 
                            // Validate inputs before sharing
                            if (monthlyBillAmount.isBlank() || unitsUsed.isBlank()) {
                                Toast.makeText(
                                    context,
                                    if (isHindi) "कृपया कुल बिल और यूनिट्स दर्ज करें" else "Please enter total bill and units",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (flats.all { it.units.isBlank() }) {
                                Toast.makeText(
                                    context,
                                    if (isHindi) "कृपया कम से कम एक फ्लैट का यूनिट दर्ज करें" else "Please enter units for at least one flat",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                shareElectricityBill(context, monthlyBillAmount, unitsUsed, flats, commonAreaCostPerFlat, isHindi)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = if (isHindi) "बिल शेयर करें" else stringResource(R.string.share_bill),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
                    title = if (isHindi) "इस महीने का बिल" else stringResource(R.string.this_month_bill),
                    value = monthlyBillAmount,
                    onValueChange = { newValue ->
                        // Allow only numbers and single decimal point
                        if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
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
                    title = if (isHindi) "कुल बिल्डिंग यूनिट्स" else stringResource(R.string.total_building_units),
                    value = unitsUsed,
                    onValueChange = { newValue ->
                        // Allow only numbers
                        if (newValue.matches(Regex("^\\d*$"))) {
                            unitsUsed = newValue
                        }
                    },
                    icon = Icons.Default.Edit,
                    color = MaterialTheme.colorScheme.secondaryContainer,
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
                            text = if (isHindi) "दर प्रति यूनिट" else stringResource(R.string.rate_per_unit),
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
                            text = if (isHindi) "स्वचालित गणना" else stringResource(R.string.auto_calculated),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Flats Section
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
                            text = if (isHindi) "व्यक्तिगत फ्लैट रीडिंग" else stringResource(R.string.individual_flat_readings),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    flats.forEachIndexed { index, flat ->
                        FlatInputRow(
                            flat = flat,
                            onFlatChange = { newFlat ->
                                flats = flats.toMutableList().apply {
                                    this[index] = newFlat
                                }
                            },
                            rate = rate,
                            commonAreaCostPerFlat = commonAreaCostPerFlat,
                            isHindi = isHindi
                        )
                        
                        if (index < flats.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Summary Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
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
                            text = if (isHindi) "बिल सारांश" else stringResource(R.string.bill_summary),
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
                                text = if (isHindi) "यूनिट्स विवरण" else stringResource(R.string.units_breakdown),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            SummaryRow(
                                if (isHindi) "कुल फ्लैट यूनिट्स:" else "${stringResource(R.string.total_flat_units)}:", 
                                "$totalFlatUnits kWh"
                            )
                            SummaryRow(
                                if (isHindi) "बिल्डिंग कुल यूनिट्स:" else "${stringResource(R.string.building_total_units)}:", 
                                "$totalBuildingUnits kWh"
                            ) 
                            SummaryRow(
                                if (isHindi) "कॉमन एरिया यूनिट्स:" else "${stringResource(R.string.common_area_units)}:", 
                                "$commonAreaUnits kWh"
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Cost Summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (isHindi) "कॉमन एरिया कॉस्ट" else stringResource(R.string.common_area_cost),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            SummaryRow(
                                if (isHindi) "कुल कॉमन एरिया कॉस्ट:" else "${stringResource(R.string.total_common_area_cost)}:", 
                                "₹${String.format("%.2f", commonAreaTotalCost)}"
                            )
                            SummaryRow(
                                if (isHindi) "कॉस्ट प्रति फ्लैट:" else "${stringResource(R.string.cost_per_flat)}:", 
                                "₹${String.format("%.2f", commonAreaCostPerFlat)}", 
                                isHighlight = true
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = if (isHindi) "नोट: हर फ्लैट का अंतिम बिल = व्यक्तिगत कॉस्ट + कॉमन एरिया शेयर" else stringResource(R.string.note_final_bill),
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
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
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
    val flatIndividualCost = (flat.units.toIntOrNull() ?: 0) * rate
    val flatTotalCost = flatIndividualCost + commonAreaCostPerFlat
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // First row: Name and Input field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = flat.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(80.dp)
                )
                
                OutlinedTextField(
                    value = flat.units,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d*$"))) {
                            onFlatChange(flat.copy(units = newValue))
                        }
                    },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Second row: Cost calculation
            if (flat.units.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "कुल बिल:" else "${stringResource(R.string.total_bill)}:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
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



fun generatePdfBill(
    context: Context,
    totalBill: String,
    totalUnits: String,
    flats: List<Flat>,
    commonAreaCostPerFlat: Double,
    isHindi: Boolean
) {
    val rate = if (totalBill.isNotBlank() && totalUnits.isNotBlank()) {
        val bill = totalBill.toDoubleOrNull() ?: 0.0
        val units = totalUnits.toDoubleOrNull() ?: 0.0
        if (units > 0) bill / units else 0.0
    } else 0.0
    
    val totalFlatUnits = flats.sumOf { it.units.toIntOrNull() ?: 0 }
    val buildingUnits = totalUnits.toIntOrNull() ?: 0
    val commonAreaUnits = if (buildingUnits > totalFlatUnits) buildingUnits - totalFlatUnits else 0
    
    try {
        // Create a new PDF document
        val pdfDocument = PdfDocument()
        
        // Create a page description
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        
        // Start a page
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        // Define paint objects for different text styles
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
        
        // Start drawing on the PDF
        var yPosition = 60f
        
        // Title
        val title = if (isHindi) "UJHomeBill - बिजली बिल रिपोर्ट" else "UJHomeBill - Electricity Bill Report"
        canvas.drawText(title, 50f, yPosition, titlePaint)
        yPosition += 40f
        
        // Date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        canvas.drawText(if (isHindi) "दिनांक: $currentDate" else "Date: $currentDate", 50f, yPosition, normalPaint)
        yPosition += 30f
        
        // Line separator
        canvas.drawLine(50f, yPosition, 545f, yPosition, normalPaint)
        yPosition += 30f
        
        // Bill Summary Section
        canvas.drawText(if (isHindi) "बिल सारांश" else "Bill Summary", 50f, yPosition, headingPaint)
        yPosition += 25f
        
        canvas.drawText(if (isHindi) "कुल मासिक बिल: ₹$totalBill" else "Total Monthly Bill: ₹$totalBill", 70f, yPosition, normalPaint)
        yPosition += 20f
        canvas.drawText(if (isHindi) "कुल यूनिट्स: $totalUnits kWh" else "Total Units: $totalUnits kWh", 70f, yPosition, normalPaint)
        yPosition += 20f
        canvas.drawText(if (isHindi) "दर प्रति यूनिट: ₹${String.format("%.2f", rate)}/kWh" else "Rate per Unit: ₹${String.format("%.2f", rate)}/kWh", 70f, yPosition, normalPaint)
        yPosition += 30f
        
        // Units Breakdown Section
        canvas.drawText(if (isHindi) "यूनिट्स विवरण" else "Units Breakdown", 50f, yPosition, headingPaint)
        yPosition += 25f
        
        canvas.drawText(if (isHindi) "कुल फ्लैट यूनिट्स: $totalFlatUnits kWh" else "Total Flat Units: $totalFlatUnits kWh", 70f, yPosition, normalPaint)
        yPosition += 20f
        canvas.drawText(if (isHindi) "बिल्डिंग कुल यूनिट्स: $buildingUnits kWh" else "Building Total Units: $buildingUnits kWh", 70f, yPosition, normalPaint)
        yPosition += 20f
        canvas.drawText(if (isHindi) "कॉमन एरिया यूनिट्स: $commonAreaUnits kWh" else "Common Area Units: $commonAreaUnits kWh", 70f, yPosition, normalPaint)
        yPosition += 30f
        
        // Flat-wise Details Section
        canvas.drawText(if (isHindi) "फ्लैट-वार विवरण" else "Flat-wise Details", 50f, yPosition, headingPaint)
        yPosition += 25f
        
        flats.forEach { flat ->
            if (flat.units.isNotBlank()) {
                val individualCost = (flat.units.toIntOrNull() ?: 0) * rate
                val totalCost = individualCost + commonAreaCostPerFlat
                
                canvas.drawText("${flat.name}:", 70f, yPosition, normalPaint)
                yPosition += 15f
                canvas.drawText(if (isHindi) "  यूनिट्स: ${flat.units} kWh" else "  Units: ${flat.units} kWh", 90f, yPosition, smallPaint)
                yPosition += 15f
                canvas.drawText(if (isHindi) "  व्यक्तिगत कॉस्ट: ₹${String.format("%.2f", individualCost)}" else "  Individual Cost: ₹${String.format("%.2f", individualCost)}", 90f, yPosition, smallPaint)
                yPosition += 15f
                canvas.drawText(if (isHindi) "  कॉमन एरिया शेयर: ₹${String.format("%.2f", commonAreaCostPerFlat)}" else "  Common Area Share: ₹${String.format("%.2f", commonAreaCostPerFlat)}", 90f, yPosition, smallPaint)
                yPosition += 15f
                canvas.drawText(if (isHindi) "  कुल बिल: ₹${String.format("%.2f", totalCost)}" else "  Total Bill: ₹${String.format("%.2f", totalCost)}", 90f, yPosition, normalPaint)
                yPosition += 25f
            }
        }
        
        // Common Area Cost Section
        yPosition += 10f
        canvas.drawText(if (isHindi) "कॉमन एरिया कॉस्ट" else "Common Area Cost", 50f, yPosition, headingPaint)
        yPosition += 25f
        
        val commonAreaTotalCost = commonAreaUnits * rate
        canvas.drawText(if (isHindi) "कुल कॉमन एरिया कॉस्ट: ₹${String.format("%.2f", commonAreaTotalCost)}" else "Total Common Area Cost: ₹${String.format("%.2f", commonAreaTotalCost)}", 70f, yPosition, normalPaint)
        yPosition += 20f
        canvas.drawText(if (isHindi) "कॉस्ट प्रति फ्लैट: ₹${String.format("%.2f", commonAreaCostPerFlat)}" else "Cost per Flat: ₹${String.format("%.2f", commonAreaCostPerFlat)}", 70f, yPosition, normalPaint)
        yPosition += 30f
        
        // Footer note
        yPosition += 20f
        canvas.drawText(if (isHindi) "नोट: हर फ्लैट का अंतिम बिल = व्यक्तिगत कॉस्ट + कॉमन एरिया शेयर" else "Note: Each flat's final bill = Individual cost + Common area share", 50f, yPosition, smallPaint)
        
        // Finish the page
        pdfDocument.finishPage(page)
        
        // Save the PDF
        val fileName = "UJHomeBill_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
        
        // Use app-specific directory which doesn't require permission
        val pdfDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, use app-specific external storage
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "UJHomeBill")
        } else {
            // For older versions, use external storage
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "UJHomeBill")
        }
        
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }
        val file = File(pdfDir, fileName)
        
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        
        // Show success message with shorter path
        val shortPath = "UJHomeBill/$fileName"
        Toast.makeText(context, if (isHindi) "PDF सेव किया गया: $shortPath" else "PDF saved: $shortPath", Toast.LENGTH_LONG).show()
        
        // Open the PDF using FileProvider for security
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

fun shareElectricityBill(
    context: Context,
    totalBill: String,
    totalUnits: String,
    flats: List<Flat>,
    commonAreaCostPerFlat: Double,
    isHindi: Boolean
) {
    val rate = if (totalBill.isNotBlank() && totalUnits.isNotBlank()) {
        val bill = totalBill.toDoubleOrNull() ?: 0.0
        val units = totalUnits.toDoubleOrNull() ?: 0.0
        if (units > 0) bill / units else 0.0
    } else 0.0
    
    val totalFlatUnits = flats.sumOf { it.units.toIntOrNull() ?: 0 }
    val buildingUnits = totalUnits.toIntOrNull() ?: 0
    val commonAreaUnits = if (buildingUnits > totalFlatUnits) buildingUnits - totalFlatUnits else 0
    
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
            appendLine("💰 फ्लैट-वार बिल:")
            flats.forEach { flat ->
                if (flat.units.isNotBlank()) {
                    val individualCost = (flat.units.toIntOrNull() ?: 0) * rate
                    val totalCost = individualCost + commonAreaCostPerFlat
                    appendLine("${flat.name}: ₹${String.format("%.2f", totalCost)} (₹${String.format("%.0f", individualCost)} + ₹${String.format("%.0f", commonAreaCostPerFlat)})")
                }
            }
            appendLine()
            appendLine("🏢 कॉमन एरिया कॉस्ट:")
            appendLine("• कुल: ₹${String.format("%.2f", commonAreaUnits * rate)}")
            appendLine("• प्रति फ्लैट: ₹${String.format("%.2f", commonAreaCostPerFlat)}")
            appendLine()
            appendLine("💡 नोट: हर फ्लैट का अंतिम बिल = व्यक्तिगत कॉस्ट + कॉमन एरिया शेयर")
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
            appendLine("💰 Flat-wise Bills:")
            flats.forEach { flat ->
                if (flat.units.isNotBlank()) {
                    val individualCost = (flat.units.toIntOrNull() ?: 0) * rate
                    val totalCost = individualCost + commonAreaCostPerFlat
                    appendLine("${flat.name}: ₹${String.format("%.2f", totalCost)} (₹${String.format("%.0f", individualCost)} + ₹${String.format("%.0f", commonAreaCostPerFlat)})")
                }
            }
            appendLine()
            appendLine("🏢 Common Area Cost:")
            appendLine("• Total: ₹${String.format("%.2f", commonAreaUnits * rate)}")
            appendLine("• Per Flat: ₹${String.format("%.2f", commonAreaCostPerFlat)}")
            appendLine()
            appendLine("💡 Note: Each flat's final bill = Individual cost + Common area share")
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

@Preview(showBackground = true)
@Composable
fun HomeBillScreenPreview() {
    HomeBillTheme {
        HomeBillScreen()
    }
}