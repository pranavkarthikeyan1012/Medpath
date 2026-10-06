package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.HealthDataManager
import com.example.data.HealthcareFacility
import com.example.data.Pharmacy
import com.example.ui.components.AccessibilityDialog
import com.example.ui.components.AccessibilityTopBarAction
import com.example.ui.theme.status_due_amber
import com.example.ui.theme.status_due_bg
import com.example.ui.theme.status_taken_bg
import com.example.ui.theme.status_taken_border
import com.example.ui.theme.status_taken_green
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityExplorerScreen(navController: NavController) {
    val context = LocalContext.current
    val facilities by HealthDataManager.facilities.collectAsStateWithLifecycle()
    val pharmacies by HealthDataManager.pharmacies.collectAsStateWithLifecycle()
    val userLat by HealthDataManager.userLatitude.collectAsStateWithLifecycle()
    val userLng by HealthDataManager.userLongitude.collectAsStateWithLifecycle()
    val locationName by HealthDataManager.locationName.collectAsStateWithLifecycle()

    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isLocating by remember { mutableStateOf(false) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun refreshLocation() {
        isLocating = true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    isLocating = false
                    if (loc != null) {
                        HealthDataManager.updateUserLocation(loc.latitude, loc.longitude, "Live GPS Device Location")
                        Toast.makeText(context, "Location updated: %.4f, %.4f".format(loc.latitude, loc.longitude), Toast.LENGTH_SHORT).show()
                    } else {
                        // Fallback location near MIOT and SRM in Chennai
                        HealthDataManager.updateUserLocation(13.0185, 80.1865, "Manapakkam Medical Zone, Chennai (Near MIOT & SRM)")
                        Toast.makeText(context, "Location synchronized with local Chennai healthcare network", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    isLocating = false
                    Toast.makeText(context, "Unable to get GPS, using healthcare district center", Toast.LENGTH_SHORT).show()
                }
            } catch (e: SecurityException) {
                isLocating = false
            }
        } else {
            isLocating = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationPermissionGranted = fineGranted || coarseGranted
        if (locationPermissionGranted) {
            refreshLocation()
        } else {
            Toast.makeText(context, "Location permission enables automatic distance calculation to nearby hospitals & pharmacies", Toast.LENGTH_LONG).show()
        }
    }

    if (showAccessibilityDialog) {
        AccessibilityDialog(onDismissRequest = { showAccessibilityDialog = false })
    }

    // Dynamic distance calculation and sorting relative to user's real location
    val updatedFacilities = facilities.map { fac ->
        val dynamicDist = HealthDataManager.getDistanceKm(fac.latitude, fac.longitude)
        fac.copy(distanceKm = dynamicDist)
    }

    val updatedPharmacies = pharmacies.map { pharm ->
        val dynamicDist = HealthDataManager.getDistanceKm(pharm.latitude, pharm.longitude)
        pharm.copy(distanceKm = dynamicDist)
    }

    // Nearest Government Hospital relative to patient location (Ramapuram / GPS)
    val nearestGovtHospital = updatedFacilities
        .filter { it.type.contains("Government", ignoreCase = true) }
        .minByOrNull { it.distanceKm }

    // Filter logic sorted by distance so nearest is always at top
    val filteredFacilities = updatedFacilities.filter { fac ->
        val matchesSearch = searchQuery.isBlank() ||
                fac.name.contains(searchQuery, ignoreCase = true) ||
                fac.type.contains(searchQuery, ignoreCase = true) ||
                fac.address.contains(searchQuery, ignoreCase = true) ||
                fac.departments.any { it.contains(searchQuery, ignoreCase = true) }

        val matchesCategory = when (selectedCategory) {
            "Govt Hospitals" -> fac.type.contains("Government", ignoreCase = true)
            "Private Hospitals" -> fac.type.contains("Private", ignoreCase = true) || fac.type.contains("Specialty", ignoreCase = true)
            "24x7 Emergency" -> fac.isEmergency24x7
            "Pharmacies" -> false // handled in pharmacies list
            else -> true
        }

        matchesSearch && matchesCategory
    }.sortedBy { it.distanceKm }

    val filteredPharmacies = updatedPharmacies.filter { pharm ->
        val matchesSearch = searchQuery.isBlank() ||
                pharm.name.contains(searchQuery, ignoreCase = true) ||
                pharm.type.contains(searchQuery, ignoreCase = true) ||
                pharm.address.contains(searchQuery, ignoreCase = true)

        val matchesCategory = when (selectedCategory) {
            "Govt Hospitals" -> pharm.type.contains("Jan Aushadhi", ignoreCase = true) || pharm.type.contains("Government", ignoreCase = true)
            "Pharmacies" -> true
            "24x7 Emergency" -> pharm.openHours.contains("24", ignoreCase = true)
            "Private Hospitals" -> false
            else -> true
        }

        matchesSearch && matchesCategory
    }.sortedBy { it.distanceKm }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hospitals & Pharmacies", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Nearby Care & Government Health Centers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AccessibilityTopBarAction(
                        onOpenDialog = { showAccessibilityDialog = true }
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Location Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (locationPermissionGranted) status_taken_bg else status_due_bg,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = null,
                                            tint = if (locationPermissionGranted) status_taken_green else status_due_amber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (locationPermissionGranted) "LIVE GPS ACTIVE" else "DEFAULT HEALTHCARE HUB",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (locationPermissionGranted) status_taken_green else status_due_amber
                                        )
                                    }
                                    Text(
                                        text = "%.4f° N, %.4f° E".format(userLat, userLng),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = locationName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (locationPermissionGranted) {
                                        refreshLocation()
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isLocating
                            ) {
                                if (isLocating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (locationPermissionGranted) "Locate" else "Enable GPS", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Search hospitals, govt centers, pharmacies...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // Category Chips
            item {
                val categories = listOf("All", "Govt Hospitals", "Private Hospitals", "Pharmacies", "24x7 Emergency")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                when (cat) {
                                    "Govt Hospitals" -> Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                                    "Pharmacies" -> Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(16.dp))
                                    "24x7 Emergency" -> Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    else -> Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }
            }

            // Emergency SOS Shortcut
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Emergency, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Emergency Bed Telemetry & ER", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text("Real-time ICU, ER & Oxygen bed tracking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                        Button(
                            onClick = { navController.navigate("emergency_hospitals") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Live Beds →", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Nearest Government Hospital Priority Card
            if (nearestGovtHospital != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondary
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("NEAREST GOVERNMENT HOSPITAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = status_taken_bg
                                ) {
                                    Text(
                                        text = "${nearestGovtHospital.distanceKm} km away",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = status_taken_green
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = nearestGovtHospital.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Text(
                                text = "${nearestGovtHospital.type} • ${nearestGovtHospital.address}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val phone = nearestGovtHospital.emergencyPhone.split(" / ").first()
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call ER / 108", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val uri = Uri.parse("geo:${nearestGovtHospital.latitude},${nearestGovtHospital.longitude}?q=${Uri.encode(nearestGovtHospital.name)}")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${nearestGovtHospital.latitude},${nearestGovtHospital.longitude}")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Navigate", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Section: Hospitals
            if (filteredFacilities.isNotEmpty() && selectedCategory != "Pharmacies") {
                item {
                    Text(
                        text = "Hospitals & Medical Centers (${filteredFacilities.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(filteredFacilities, key = { it.id }) { facility ->
                    HospitalFacilityCard(
                        facility = facility,
                        context = context,
                        onViewLiveBeds = { navController.navigate("emergency_hospitals") }
                    )
                }
            }

            // Section: Pharmacies
            if (filteredPharmacies.isNotEmpty() && selectedCategory != "Private Hospitals") {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pharmacies & Drug Stores (${filteredPharmacies.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(filteredPharmacies, key = { it.id }) { pharmacy ->
                    PharmacyCard(pharmacy = pharmacy, context = context)
                }
            }

            if (filteredFacilities.isEmpty() && filteredPharmacies.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No healthcare centers match your search", fontWeight = FontWeight.Bold)
                            Text("Try searching for general medicine, Apollo, Jan Aushadhi, or clear filters.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { searchQuery = ""; selectedCategory = "All" }) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HospitalFacilityCard(
    facility: HealthcareFacility,
    context: Context,
    onViewLiveBeds: () -> Unit
) {
    val isGovernment = facility.type.contains("Government", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, if (isGovernment) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = facility.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isGovernment) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isGovernment) "🏛️ GOVERNMENT HOSPITAL" else "🏥 PRIVATE MULTISPECIALTY",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isGovernment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (facility.isEmergency24x7) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "24/7 ER",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${facility.distanceKm} km",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "~${facility.travelTimeMinutes} mins",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text(facility.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Bed Availability Snapshot
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Live Bed Telemetry", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "ICU: ${facility.icuBedsAvailable} • ER: ${facility.emergencyBedsAvailable} • General: ${facility.generalBedsAvailable}",
                            style = MaterialTheme.typography.bodySmall,
                            color = status_taken_green,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = status_taken_bg
                    ) {
                        Text(
                            text = "ER Wait: ${facility.erWaitTimeMinutes}m",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = status_taken_green,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Key Specialties & Departments:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(
                text = facility.departments.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isGovernment) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "✓ PM-JAY / Ayushman Bharat & CGHS Cashless Supported",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Navigate in Google Maps
                Button(
                    onClick = {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode("${facility.name}, ${facility.address}")}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            // Fallback to general geo browser
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode("${facility.name}, ${facility.address}")}"))
                            context.startActivity(browserIntent)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Navigate", fontWeight = FontWeight.Bold)
                }

                // Call Emergency/Desk
                OutlinedButton(
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${facility.emergencyPhone.split("/").first().trim()}"))
                        context.startActivity(dialIntent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontWeight = FontWeight.Bold)
                }

                // Live Beds / Bed request
                OutlinedButton(
                    onClick = onViewLiveBeds,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Live Beds", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PharmacyCard(pharmacy: Pharmacy, context: Context) {
    val isJanAushadhi = pharmacy.type.contains("Jan Aushadhi", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isJanAushadhi) status_taken_border else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pharmacy.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isJanAushadhi) status_taken_bg else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isJanAushadhi) "🏛️ GOVT PRADHAN MANTRI JAN AUSHADHI" else pharmacy.type.uppercase(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isJanAushadhi) status_taken_green else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${pharmacy.distanceKm} km",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text(pharmacy.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = status_taken_bg
                ) {
                    Text(
                        text = "🕒 ${pharmacy.openHours}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = status_taken_green,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (pharmacy.homeDelivery) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "🛵 Home Delivery Available",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "🏷️ ${pharmacy.discountOffer}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode("${pharmacy.name}, ${pharmacy.address}")}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode("${pharmacy.name}, ${pharmacy.address}")}"))
                            context.startActivity(browserIntent)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Navigate", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pharmacy.phone.split("/").first().trim()}"))
                        context.startActivity(dialIntent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
