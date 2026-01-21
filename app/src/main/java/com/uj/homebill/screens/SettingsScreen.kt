package com.uj.homebill.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.uj.homebill.viewmodel.BillViewModel
import com.uj.homebill.viewmodel.ClearDataState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions

// Clean Blue/White Theme Colors
private val BlueAccent = Color(0xFF1E88E5)
private val LightBlue = Color(0xFFE3F2FD)
private val White = Color(0xFFFFFFFF)
private val LightGray = Color(0xFFF5F5F5)
private val DarkText = Color(0xFF212121)
private val GrayText = Color(0xFF757575)
private val RedError = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BillViewModel,
    navController: NavHostController,
    isHindi: Boolean = false
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    // State
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var showYearPicker by remember { mutableStateOf(false) }
    
    // Collect data from ViewModel
    val userSettings by viewModel.userSettings.collectAsState()
    val yearlyAnalytics by viewModel.yearlyAnalytics.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()
    val selectedYearAnalytics by viewModel.selectedYearAnalytics.collectAsState()
    val monthlyBillSummaries by viewModel.monthlyBillSummaries.collectAsState()
    val clearDataState by viewModel.clearDataState.collectAsState()
    
    // Username state
    var userName by remember { mutableStateOf(userSettings?.userName ?: "") }
    
    // Update userName when userSettings changes
    LaunchedEffect(userSettings) {
        userName = userSettings?.userName ?: ""
    }
    
    // Load analytics for selected year
    LaunchedEffect(selectedYear) {
        viewModel.loadMonthlyBillSummaries(selectedYear)
    }
    
    // Handle clear data state
    LaunchedEffect(clearDataState) {
        when (clearDataState) {
            is ClearDataState.Success -> {
                Toast.makeText(
                    context,
                    if (isHindi) "सभी डेटा सफलतापूर्वक हटा दिया गया!" else "All data cleared successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                showDeleteDialog = false
                deletePassword = ""
                viewModel.resetClearDataState()
            }
            is ClearDataState.Error -> {
                Toast.makeText(
                    context,
                    (clearDataState as ClearDataState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetClearDataState()
            }
            else -> {}
        }
    }
    
    // Month names for both languages
    val monthNamesEnglish = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val monthNamesHindi = listOf("जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isHindi) "सेटिंग्स" else "Settings",
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isHindi) "वापस" else "Back",
                            tint = White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BlueAccent,
                    titleContentColor = White
                )
            )
        },
        containerColor = LightGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Section
            SettingsCard(
                title = if (isHindi) "उपयोगकर्ता प्रोफ़ाइल" else "User Profile",
                icon = Icons.Default.Person,
                iconColor = BlueAccent
            ) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { 
                        userName = it
                        viewModel.updateUserName(it)
                    },
                    label = { Text(if (isHindi) "आपका नाम" else "Your Name") },
                    placeholder = { Text(if (isHindi) "नाम दर्ज करें" else "Enter your name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlueAccent,
                        focusedLabelColor = BlueAccent,
                        cursorColor = BlueAccent
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip(
                        label = if (isHindi) "आखिरी सेव" else "Last Saved",
                        value = if (userSettings?.lastSavedDate != null && userSettings!!.lastSavedDate > 0) {
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(userSettings!!.lastSavedDate))
                        } else {
                            if (isHindi) "कभी नहीं" else "Never"
                        }
                    )
                    InfoChip(
                        label = if (isHindi) "कुल बिल" else "Total Bills",
                        value = "${userSettings?.totalBillsGenerated ?: 0}"
                    )
                }
            }
            
            // Year Analytics Section
            SettingsCard(
                title = if (isHindi) "वार्षिक एनालिटिक्स" else "Year Analytics",
                icon = Icons.Default.DateRange,
                iconColor = BlueAccent
            ) {
                if (yearlyAnalytics.isEmpty()) {
                    // No data state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = GrayText,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHindi) "कोई डेटा नहीं" else "No Data Available",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = GrayText
                        )
                        Text(
                            text = if (isHindi) 
                                "बिल शेयर करने के बाद एनालिटिक्स यहां दिखाई देगी" 
                            else 
                                "Share a bill to see analytics here",
                            fontSize = 14.sp,
                            color = GrayText.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Year Selector
                    if (availableYears.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHindi) "वर्ष चुनें:" else "Select Year:",
                                fontWeight = FontWeight.Medium,
                                color = DarkText
                            )
                            
                            ExposedDropdownMenuBox(
                                expanded = showYearPicker,
                                onExpandedChange = { showYearPicker = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedYear.toString(),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showYearPicker)
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .width(120.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BlueAccent,
                                        unfocusedBorderColor = GrayText,
                                        focusedTextColor = DarkText,
                                        unfocusedTextColor = DarkText
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = showYearPicker,
                                    onDismissRequest = { showYearPicker = false },
                                    modifier = Modifier.background(White)
                                ) {
                                    availableYears.forEach { year ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    text = year.toString(),
                                                    color = DarkText
                                                ) 
                                            },
                                            onClick = {
                                                selectedYear = year
                                                showYearPicker = false
                                            },
                                            modifier = Modifier.background(White)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Analytics for selected year
                    selectedYearAnalytics?.let { analytics ->
                        // Total Bill Highlight
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LightBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isHindi) "कुल वार्षिक बिल" else "Total Yearly Bill",
                                    fontSize = 14.sp,
                                    color = GrayText
                                )
                                Text(
                                    text = "₹${String.format("%,.0f", analytics.totalBillAmount)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueAccent
                                )
                                Text(
                                    text = "${analytics.monthsRecorded} ${if (isHindi) "महीने रिकॉर्ड" else "months recorded"}",
                                    fontSize = 12.sp,
                                    color = GrayText
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBox(
                                title = if (isHindi) "औसत मासिक" else "Avg Monthly",
                                value = "₹${String.format("%.0f", analytics.averageMonthlyBill)}",
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                title = if (isHindi) "कुल यूनिट" else "Total Units",
                                value = "${analytics.totalUnitsConsumed}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBox(
                                title = if (isHindi) "औसत दर" else "Avg Rate",
                                value = "₹${String.format("%.2f", analytics.averageRate)}/kWh",
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                title = if (isHindi) "महीने" else "Months",
                                value = "${analytics.monthsRecorded}/12",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Monthly breakdown
                        if (monthlyBillSummaries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = if (isHindi) "मासिक विवरण" else "Monthly Breakdown",
                                fontWeight = FontWeight.SemiBold,
                                color = DarkText
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = LightGray),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    monthlyBillSummaries.forEach { summary ->
                                        val monthName = if (isHindi) {
                                            monthNamesHindi.getOrElse(summary.month - 1) { "Unknown" }
                                        } else {
                                            monthNamesEnglish.getOrElse(summary.month - 1) { "Unknown" }
                                        }
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = monthName,
                                                color = DarkText
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                Text(
                                                    text = "${summary.totalUnitsConsumed} kWh",
                                                    color = GrayText,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "₹${String.format("%.0f", summary.totalBillAmount)}",
                                                    fontWeight = FontWeight.Medium,
                                                    color = BlueAccent
                                                )
                                            }
                                        }
                                        if (monthlyBillSummaries.last() != summary) {
                                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    } ?: run {
                        Text(
                            text = if (isHindi) "$selectedYear के लिए कोई डेटा नहीं" else "No data for $selectedYear",
                            color = GrayText,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // All Years Summary
            if (yearlyAnalytics.isNotEmpty()) {
                SettingsCard(
                    title = if (isHindi) "सभी वर्षों का सारांश" else "All Years Summary",
                    icon = Icons.Default.Star,
                    iconColor = BlueAccent
                ) {
                    yearlyAnalytics.forEach { analytics ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = analytics.year.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = DarkText
                                )
                                Text(
                                    text = "${analytics.monthsRecorded} ${if (isHindi) "महीने" else "months"}",
                                    fontSize = 12.sp,
                                    color = GrayText
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${String.format("%,.0f", analytics.totalBillAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = BlueAccent
                                )
                                Text(
                                    text = "${analytics.totalUnitsConsumed} kWh",
                                    fontSize = 12.sp,
                                    color = GrayText
                                )
                            }
                        }
                        if (yearlyAnalytics.last() != analytics) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
            
            // Danger Zone
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = RedError,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "खतरा क्षेत्र" else "Danger Zone",
                            fontWeight = FontWeight.Bold,
                            color = RedError
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (isHindi) 
                            "सभी सेव किए गए डेटा को स्थायी रूप से हटाएं।" 
                        else 
                            "Permanently delete all saved data.",
                        fontSize = 14.sp,
                        color = GrayText
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RedError),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isHindi) "सभी डेटा हटाएं" else "Delete All Data")
                    }
                }
            }
            
            // About App Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                onClick = { showAboutDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LightBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = BlueAccent,
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isHindi) "ऐप के बारे में" else "About App",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = DarkText
                            )
                            Text(
                                text = "UJHomeBill v2.0",
                                fontSize = 13.sp,
                                color = GrayText
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = GrayText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                deletePassword = ""
            },
            title = {
                Text(
                    text = if (isHindi) "डेटा हटाने की पुष्टि करें" else "Confirm Deletion",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isHindi) 
                            "⚠️ यह सभी बिल रिकॉर्ड और सेटिंग्स को स्थायी रूप से हटा देगा।" 
                        else 
                            "⚠️ This will permanently delete all bill records and settings."
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (isHindi) "पासवर्ड दर्ज करें:" else "Enter password:",
                        fontSize = 14.sp,
                        color = GrayText
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        placeholder = { Text(if (isHindi) "पासवर्ड" else "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = clearDataState is ClearDataState.Error,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlueAccent,
                            errorBorderColor = RedError
                        )
                    )
                    
                    if (clearDataState is ClearDataState.Error) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = (clearDataState as ClearDataState.Error).message,
                            fontSize = 12.sp,
                            color = RedError
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData(
                            password = deletePassword,
                            onSuccess = {},
                            onError = {}
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError),
                    enabled = deletePassword.isNotEmpty() && clearDataState !is ClearDataState.Clearing
                ) {
                    if (clearDataState is ClearDataState.Clearing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (isHindi) "हटाएं" else "Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        deletePassword = ""
                    }
                ) {
                    Text(
                        text = if (isHindi) "रद्द करें" else "Cancel",
                        color = BlueAccent
                    )
                }
            }
        )
    }
    
    // About App Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier
                                .size(40.dp)
                                .padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "UJHomeBill",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (isHindi) "संस्करण 3.0" else "Version 3.0",
                            fontSize = 14.sp,
                            color = GrayText
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Description
                    Text(
                        text = if (isHindi) 
                            "बिल्डिंग में कई फ्लैटों के बीच बिजली बिल की गणना और प्रबंधन के लिए एक आधुनिक Android ऐप।"
                        else 
                            "A modern Android app for calculating and managing electricity bills across multiple flats in a building.",
                        fontSize = 14.sp,
                        color = GrayText,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Features List
                    Text(
                        text = if (isHindi) "मुख्य विशेषताएं:" else "Key Features:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = DarkText
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val features = if (isHindi) listOf(
                        "🏠 5 फ्लैटों के लिए बिल प्रबंधन",
                        "💾 ऑफलाइन डेटाबेस स्टोरेज",
                        "📊 पिछला/वर्तमान यूनिट ट्रैकिंग",
                        "💧 अन्य सामान्य बिल (पानी, मेंटेनेंस)",
                        "📈 वार्षिक एनालिटिक्स और रिपोर्ट",
                        "📄 PDF जनरेशन और शेयरिंग",
                        "🌐 हिंदी और अंग्रेजी दोनों में"
                    ) else listOf(
                        "🏠 Bill management for 5 flats",
                        "💾 Offline database storage",
                        "📊 Previous/Current unit tracking",
                        "💧 Other common bills (Water, Maintenance)",
                        "📈 Yearly analytics and reports",
                        "📄 PDF generation and sharing",
                        "🌐 Available in Hindi & English"
                    )
                    
                    features.forEach { feature ->
                        Text(
                            text = feature,
                            fontSize = 13.sp,
                            color = GrayText,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HorizontalDivider(color = LightGray)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Technical Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isHindi) "प्लेटफॉर्म" else "Platform",
                                fontSize = 12.sp,
                                color = GrayText
                            )
                            Text(
                                text = "Android 7.0+",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkText
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isHindi) "तकनीक" else "Technology",
                                fontSize = 12.sp,
                                color = GrayText
                            )
                            Text(
                                text = "Kotlin + Compose",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkText
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Footer
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LightBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isHindi) "❤️ से बनाया गया" else "Made with ❤️",
                                fontSize = 13.sp,
                                color = BlueAccent,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "© 2025 UJHomeBill",
                                fontSize = 11.sp,
                                color = GrayText
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent)
                ) {
                    Text(if (isHindi) "ठीक है" else "OK")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkText
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = GrayText
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            color = DarkText
        )
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = LightGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = GrayText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                color = DarkText,
                textAlign = TextAlign.Center
            )
        }
    }
}
