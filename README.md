# UJHomeBill - Electricity Bill Calculator

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)](https://developer.android.com/jetpack/compose)

A modern Android application built with Jetpack Compose for calculating and managing electricity bills across multiple flats in a building. Perfect for apartment buildings, housing societies, and shared living spaces.

## 📱 Screenshot

![UJHomeBill App Screenshot](Screenshot_20250823_195500.png)

## ✨ Features

### 🏠 **Multi-Flat Bill Management**
- Calculate electricity bills for up to 5 flats (Kailash, Ajay, Manoj, Rakesh, Ramesh)
- Individual unit consumption tracking per flat
- Automatic rate calculation based on total bill and units

### ⚡ **Smart Calculations**
- **Auto Rate Calculation**: Automatically calculates rate per unit (₹/kWh) based on total bill and building units
- **Common Area Distribution**: Fairly distributes common area electricity costs across all flats
- **Real-time Updates**: Instant calculation updates as you enter data

### 🌐 **Bilingual Support**
- **Hindi & English**: Complete bilingual interface
- Easy language toggle with EN/हिं button
- All labels, messages, and content available in both languages

### 📊 **Comprehensive Bill Breakdown**
- Total flat units consumption
- Building total units (including common areas)
- Common area units calculation
- Rate per unit display
- Individual flat cost breakdown
- Common area cost distribution

### 🔧 **Input Validation**
- **Money Fields**: Validates up to 7 digits + 2 decimal places (₹9999999.99)
- **Units Fields**: Validates up to 6 digits integer only (999999 kWh)
- Real-time input filtering prevents invalid entries
- Error-free calculations with validated inputs

### 📄 **Export & Share Options**
- **PDF Generation**: Create professional PDF bills with complete breakdown
- **Share Functionality**: Share bill details via WhatsApp, email, SMS, etc.
- **Enhanced Share Format**: Includes flat-wise units for next month reference
- **Reset Option**: Clear all fields with confirmation dialog

## 🎨 **Modern UI Design**

### **Header Section**
- Beautiful gradient background (blue to green)
- Language toggle button with professional styling
- Action buttons: PDF Save, Share, Reset
- Proper status bar handling for modern devices

### **Input Cards**
- Clean, card-based design for all inputs
- Icon-based identification for easy understanding
- Responsive layout for different screen sizes
- Material Design 3 components

### **Smart Layout**
- Optimized for Samsung Galaxy S23+ and other modern devices
- Proper spacing and padding
- Edge-to-edge design with system bar awareness

## 📋 **How to Use**

1. **Enter Total Bill**: Input the monthly electricity bill amount
2. **Enter Building Units**: Input total building electricity consumption
3. **Add Flat Units**: Enter individual consumption for each flat
4. **Auto Calculation**: App automatically calculates:
   - Rate per unit
   - Common area consumption
   - Individual flat bills
   - Common area cost distribution
5. **Export/Share**: Use PDF or Share buttons to save/send bills

## 🔢 **Calculation Logic**

```
Rate per Unit = Total Bill ÷ Total Building Units
Common Area Units = Building Total Units - Sum of All Flat Units
Common Area Cost = Common Area Units × Rate per Unit
Common Area Cost per Flat = Common Area Cost ÷ Number of Flats
Final Flat Bill = (Flat Units × Rate per Unit) + Common Area Cost per Flat
```

## 📤 **Sample Shareable Messages**

The app generates professional, formatted messages for easy sharing. Here are examples in both languages:

### **Hindi Output Sample:**
```
🏠 UJHomeBill - बिजली बिल सारांश
=========================================

📊 यूनिट्स विवरण:
• कुल फ्लैट यूनिट्स: 1324 kWh
• बिल्डिंग कुल यूनिट्स: 1458 kWh
• कॉमन एरिया यूनिट्स: 134 kWh
• दर प्रति यूनिट: ₹8.53/kWh

💰 फ्लैट-वार बिल:
कैलाश: 485 kWh - ₹4367.07 (₹4138 + ₹229)
अजय: 148 kWh - ₹1491.48 (₹1263 + ₹229)
मनोज: 316 kWh - ₹2925.01 (₹2696 + ₹229)
राकेश: 227 kWh - ₹2165.58 (₹1937 + ₹229)
रमेश: 148 kWh - ₹1491.48 (₹1263 + ₹229)

🏢 कॉमन एरिया कॉस्ट:
• कुल: ₹1143.41
• प्रति फ्लैट: ₹228.60

💡 नोट: हर फ्लैट का अंतिम बिल = व्यक्तिगत कॉस्ट + कॉमन एरिया शेयर
```

### **English Output Sample:**
```
🏠 UJHomeBill - Electricity Bill Summary
=========================================

📊 Units Breakdown:
• Total Flat Units: 1324 kWh
• Building Total Units: 1458 kWh
• Common Area Units: 134 kWh
• Rate per Unit: ₹8.53/kWh

💰 Flat-wise Bills:
Kailash: 485 kWh - ₹4367.07 (₹4138 + ₹229)
Ajay: 148 kWh - ₹1491.48 (₹1263 + ₹229)
Manoj: 316 kWh - ₹2925.01 (₹2696 + ₹229)
Rakesh: 227 kWh - ₹2165.58 (₹1937 + ₹229)
Ramesh: 148 kWh - ₹1491.48 (₹1263 + ₹229)

🏢 Common Area Cost:
• Total: ₹1143.41
• Per Flat: ₹228.60

💡 Note: Each flat's final bill = Individual cost + Common area share
```

### **Key Features of Shared Messages:**
- **📱 WhatsApp/SMS Ready**: Formatted for messaging apps
- **📊 Complete Breakdown**: Shows all calculations transparently
- **🏠 Flat Units Included**: Helps with next month's readings
- **💰 Cost Transparency**: Individual + common area costs shown
- **🌐 Bilingual**: Available in Hindi and English
- **📋 Copy-Paste Ready**: Professional formatting maintained

## 🛡️ **Data Validation**

- **Money Fields**: `₹0.00` to `₹9999999.99`
- **Units Fields**: `0` to `999999` kWh
- **Real-time Validation**: Invalid inputs are blocked immediately
- **Error Prevention**: No crashes due to invalid data entry

## 🌟 **Key Benefits**

- **Fair Distribution**: Ensures equitable sharing of common area costs
- **Transparency**: Clear breakdown of all charges
- **Time Saving**: Instant calculations eliminate manual work
- **Professional Output**: PDF generation for record keeping
- **User Friendly**: Intuitive interface in preferred language
- **Mobile Optimized**: Perfect for on-the-go bill calculations

## 🔧 **Technical Details**

- **Platform**: Android (API 21+)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: Modern Android development practices
- **Design**: Material Design 3
- **Features**: Edge-to-edge display, system bar integration

## 📱 **Compatibility**

- Android 5.0 (API 21) and above
- Optimized for modern devices (Galaxy S23+, Pixel series, etc.)
- Responsive design for various screen sizes
- Portrait orientation support

## 🎯 **Use Cases**

- **Apartment Buildings**: Split electricity bills among residents
- **Housing Societies**: Manage common area electricity costs
- **Shared Housing**: Fair distribution among roommates
- **Property Management**: Professional bill calculation for tenants
- **Personal Use**: Track and calculate household electricity expenses

## 📞 **About**

UJHomeBill is designed to simplify electricity bill management for multi-unit buildings. With its intuitive interface, robust calculations, and professional output options, it's the perfect solution for fair and transparent bill distribution.

---

**Made with ❤️ for better bill management**
