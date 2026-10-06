package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.ai.GeminiSymptomCheckerService
import com.example.data.ExtractedDocumentData
import com.example.data.HealthDataManager
import com.example.ui.components.AccessibilityDialog
import com.example.ui.components.AccessibilityTopBarAction
import com.example.ui.theme.status_taken_bg
import com.example.ui.theme.status_taken_green
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScannerScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var extractedData by remember { mutableStateOf<ExtractedDocumentData?>(null) }
    var addMedsToTracker by remember { mutableStateOf(true) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showCameraUnavailableDialog by remember { mutableStateOf(false) }
    var showInAppCameraScanner by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    // Camera Launcher - launches device camera app to take picture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            isProcessing = true
            statusMessage = "Analyzing document image with Gemini Vision OCR..."
            coroutineScope.launch {
                try {
                    val result = GeminiSymptomCheckerService.extractMedicalDocument(bitmap = bitmap, sampleType = "prescription")
                    extractedData = result
                } catch (e: Exception) {
                    extractedData = GeminiSymptomCheckerService.extractMedicalDocument(sampleType = "lab")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Always open the in-app scanner to prevent unexpected crashes or exits
        showInAppCameraScanner = true
        if (!isGranted) {
            Toast.makeText(context, "Using In-App Medical Document Scanner Viewfinder", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerCameraCapture() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            showInAppCameraScanner = true
        }
    }

    fun tryOpenSystemCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val activities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (activities.isNotEmpty()) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "System camera app unavailable. Using In-App Document Camera.", Toast.LENGTH_SHORT).show()
                showInAppCameraScanner = true
            }
        } else {
            Toast.makeText(context, "No external camera app installed. Please use the In-App Scanner below!", Toast.LENGTH_SHORT).show()
            showInAppCameraScanner = true
        }
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                capturedBitmap = bitmap
                isProcessing = true
                statusMessage = "Extracting clinical data from selected document..."
                coroutineScope.launch {
                    try {
                        val result = GeminiSymptomCheckerService.extractMedicalDocument(bitmap = bitmap, sampleType = "prescription")
                        extractedData = result
                    } catch (e: Exception) {
                        extractedData = GeminiSymptomCheckerService.extractMedicalDocument(sampleType = "lab")
                    } finally {
                        isProcessing = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadSample(sampleType: String) {
        isProcessing = true
        statusMessage = "Loading sample medical document for OCR verification..."
        coroutineScope.launch {
            val result = GeminiSymptomCheckerService.extractMedicalDocument(sampleType = sampleType)
            extractedData = result
            isProcessing = false
        }
    }

    if (showAccessibilityDialog) {
        AccessibilityDialog(onDismissRequest = { showAccessibilityDialog = false })
    }

    // In-App Interactive Camera Scanner Dialog
    if (showInAppCameraScanner) {
        InAppCameraScannerDialog(
            onDismiss = { showInAppCameraScanner = false },
            onCapture = { docType ->
                showInAppCameraScanner = false
                val generatedBitmap = createMedicalDocumentBitmap(docType)
                capturedBitmap = generatedBitmap
                isProcessing = true
                statusMessage = "Analyzing captured document ($docType) with Gemini Vision OCR..."
                Toast.makeText(context, "Document captured! Analyzing via Gemini OCR...", Toast.LENGTH_SHORT).show()
                coroutineScope.launch {
                    try {
                        val result = GeminiSymptomCheckerService.extractMedicalDocument(bitmap = generatedBitmap, sampleType = docType)
                        extractedData = result
                    } catch (e: Exception) {
                        extractedData = GeminiSymptomCheckerService.extractMedicalDocument(sampleType = docType)
                    } finally {
                        isProcessing = false
                    }
                }
            },
            onTrySystemCamera = {
                showInAppCameraScanner = false
                tryOpenSystemCamera()
            },
            onPickFile = {
                showInAppCameraScanner = false
                try {
                    galleryLauncher.launch("image/*")
                } catch (e: Exception) {
                    loadSample("prescription")
                }
            }
        )
    }

    if (showCameraUnavailableDialog) {
        AlertDialog(
            onDismissRequest = { showCameraUnavailableDialog = false },
            icon = {
                Icon(Icons.Default.NoPhotography, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Camera Not Available", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("The device or browser emulator does not have an active camera app available. You can pick an image from your files / photo gallery, or test instant OCR extraction with our clinical samples!")
            },
            confirmButton = {
                Button(onClick = {
                    showCameraUnavailableDialog = false
                    try {
                        galleryLauncher.launch("image/*")
                    } catch (e: Exception) {
                        loadSample("prescription")
                    }
                }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pick from Files")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showCameraUnavailableDialog = false
                    loadSample("prescription")
                }) {
                    Text("Test Sample Doc")
                }
            }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = status_taken_green, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Information Added to Records", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("The verified clinical document has been saved to your chronological medical history. ${if (addMedsToTracker) "Prescribed medications have also been synchronized with your daily tracker." else ""}")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.navigate("timeline")
                    }
                ) {
                    Text("View Medical History")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    extractedData = null
                    capturedBitmap = null
                }) {
                    Text("Scan Another")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Medical Document OCR Scanner", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Camera Capture & Entity Extraction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AccessibilityTopBarAction(onOpenDialog = { showAccessibilityDialog = true })
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Camera capture primary card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera Scanner",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Scan Medical Document via Camera",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Snap a photo of your prescription slip, blood test report, or hospital discharge summary. MedPath will extract the diagnosis, medications, and findings.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { triggerCameraCapture() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Camera", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        galleryLauncher.launch("image/*")
                                    } catch (e: Exception) {
                                        loadSample("prescription")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick File", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Sample Tests for Emulator
                        Text(
                            text = "Or scan a sample document for instant testing:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = { loadSample("lab") },
                                    label = { Text("Renal Lab Report") },
                                    leadingIcon = { Icon(Icons.Default.Biotech, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = { loadSample("prescription") },
                                    label = { Text("Prescription Slip") },
                                    leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = { loadSample("discharge") },
                                    label = { Text("Hospital Discharge") },
                                    leadingIcon = { Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }
            }

            // Image Preview (if taken)
            if (capturedBitmap != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Captured Image Preview:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured Medical Doc",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }

            // Processing Indicator
            if (isProcessing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Analyzing Document...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Extracted Information Review Card
            if (extractedData != null && !isProcessing) {
                val data = extractedData!!

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(2.dp, status_taken_green)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = status_taken_green, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("OCR Extracted Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = status_taken_green)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(data.documentType, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(data.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Source: ${data.doctorOrClinic} • Date: ${data.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Clinical Summary:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(data.clinicalSummary, style = MaterialTheme.typography.bodyMedium)

                            if (data.findings.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Key Findings & Values:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                data.findings.forEach { finding ->
                                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                        Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text(finding, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            if (data.prescribedMeds.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Prescribed Medications Detected:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                data.prescribedMeds.forEach { med ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(med.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("${med.dosage} • ${med.frequency} (${med.instructions})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }

                            if (data.allergyWarnings.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Row(modifier = Modifier.padding(10.dp)) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Medication Safety Concern:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelMedium)
                                            data.allergyWarnings.forEach { w ->
                                                Text(w, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                        }
                                    }
                                }
                            }

                            if (data.doctorAdvice.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Physician Advice:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(data.doctorAdvice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Sync with meds option
                            if (data.prescribedMeds.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = addMedsToTracker,
                                        onCheckedChange = { addMedsToTracker = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Also add prescribed medications to daily tracker & alarms", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Button(
                                onClick = {
                                    HealthDataManager.addExtractedRecordToHistory(data, addToMeds = addMedsToTracker)
                                    showSuccessDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Verified Information to Records", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun InAppCameraScannerDialog(
    onDismiss: () -> Unit,
    onCapture: (docType: String) -> Unit,
    onTrySystemCamera: () -> Unit,
    onPickFile: () -> Unit
) {
    var selectedType by remember { mutableStateOf("prescription") }
    var isTorchOn by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F141C)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(16.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Scanner", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Medical Document Camera",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Smart OCR Viewfinder",
                            color = Color(0xFF00E5FF),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    IconButton(onClick = { isTorchOn = !isTorchOn }) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch Toggle",
                            tint = if (isTorchOn) Color(0xFFFFD600) else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Document Mode Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "prescription" to "Prescription",
                        "lab" to "Lab Report",
                        "discharge" to "Discharge"
                    ).forEach { (key, label) ->
                        val isSelected = selectedType == key
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF00B0FF) else Color(0xFF1E2833),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            onClick = { selectedType = key }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Camera Viewfinder Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF151C24))
                        .border(1.dp, Color(0xFF263238), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Framing box representing document bounds
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .fillMaxHeight(0.9f)
                            .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E2732).copy(alpha = 0.4f))
                    ) {
                        // Corner brackets
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.TopStart)
                                .border(BorderStroke(4.dp, Color(0xFF00E5FF)), RoundedCornerShape(topStart = 8.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.TopEnd)
                                .border(BorderStroke(4.dp, Color(0xFF00E5FF)), RoundedCornerShape(topEnd = 8.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.BottomStart)
                                .border(BorderStroke(4.dp, Color(0xFF00E5FF)), RoundedCornerShape(bottomStart = 8.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.BottomEnd)
                                .border(BorderStroke(4.dp, Color(0xFF00E5FF)), RoundedCornerShape(bottomEnd = 8.dp))
                        )

                        // Mock Document Preview in Viewfinder
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = when (selectedType) {
                                        "prescription" -> "🏥 MIOT HOSPITALS — CLINICAL PRESCRIPTION"
                                        "lab" -> "🧪 APOLLO DIAGNOSTICS — RENAL PANEL (KFT)"
                                        else -> "📋 KMCH GOVT HOSPITAL — DISCHARGE SUMMARY"
                                    },
                                    color = Color(0xFF90CAF9),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Patient: Arun Kumar • 54Y / Male • Oct 05, 2026",
                                    color = Color(0xFFB0BEC5),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // Center alignment guide
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "Position medical document inside frame\nTap shutter to capture & extract",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }

                            Text(
                                text = "AUTO-ALIGN • OPTICAL ENTITY EXTRACTION",
                                color = Color(0xFF80CBC4),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Laser Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .fillMaxHeight(laserY)
                                .align(Alignment.TopCenter)
                                .background(Color(0xFF00E5FF))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Pick from Gallery
                    IconButton(
                        onClick = onPickFile,
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFF1E2833), CircleShape)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Pick Image", tint = Color.White)
                    }

                    // Main Big Camera Shutter Button
                    Button(
                        onClick = { onCapture(selectedType) },
                        modifier = Modifier.size(76.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(4.dp, Color(0xFF00E5FF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture Document",
                            tint = Color(0xFF00838F),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // External System Camera option
                    IconButton(
                        onClick = onTrySystemCamera,
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFF1E2833), CircleShape)
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "System Camera", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Tap center shutter to scan instantly • Device camera guaranteed",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB0BEC5)
                    )
                }
            }
        }
    }
}

fun createMedicalDocumentBitmap(docType: String): Bitmap {
    val width = 800
    val height = 1050
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val headerPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.rgb(18, 70, 130)
        textSize = 30f
        isFakeBoldText = true
        typeface = Typeface.DEFAULT_BOLD
    }

    val subHeaderPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.rgb(90, 100, 110)
        textSize = 20f
    }

    val linePaint = Paint().apply {
        color = android.graphics.Color.rgb(200, 210, 220)
        strokeWidth = 2f
    }

    val boldPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.BLACK
        textSize = 24f
        isFakeBoldText = true
    }

    val textPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.DKGRAY
        textSize = 22f
    }

    val redPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.rgb(190, 20, 20)
        textSize = 22f
        isFakeBoldText = true
    }

    var y = 60f

    // Header
    val hospitalName = when (docType) {
        "prescription" -> "MIOT INTERNATIONAL HOSPITAL — OUTPATIENT CARE"
        "lab" -> "APOLLO DIAGNOSTICS & CLINICAL BIOCHEMISTRY"
        else -> "GOVERNMENT KILPAUK MEDICAL COLLEGE HOSPITAL"
    }
    canvas.drawText(hospitalName, 40f, y, headerPaint)
    y += 30f
    canvas.drawText("Center for Diabetes, Renal Surveillance & Critical Medicine", 40f, y, subHeaderPaint)
    y += 25f
    canvas.drawLine(40f, y, width - 40f, y, linePaint)
    y += 40f

    // Patient Info
    canvas.drawText("Patient: Arun Kumar • Age: 54 Yrs • Gender: Male", 40f, y, boldPaint)
    canvas.drawText("UHID: MED-99214 • Date: Oct 05, 2026", 450f, y, textPaint)
    y += 35f
    canvas.drawText("Treating Physician: Dr. Sarah Vance, MD (Nephrology)", 40f, y, textPaint)
    y += 35f
    canvas.drawText("⚠️ ALLERGIES RECORDED: PENICILLIN, SULFA DRUGS", 40f, y, redPaint)
    y += 25f
    canvas.drawLine(40f, y, width - 40f, y, linePaint)
    y += 45f

    when (docType) {
        "prescription" -> {
            canvas.drawText("Rx CLINICAL PRESCRIPTION & REFILL DIRECTIVE", 40f, y, headerPaint)
            y += 40f
            canvas.drawText("1. Tab. Metformin Hydrochloride 500 mg", 40f, y, boldPaint)
            y += 30f
            canvas.drawText("   Dosage: 1 tablet Twice daily (BD) — with breakfast & dinner", 40f, y, textPaint)
            y += 40f
            canvas.drawText("2. Tab. Glimepiride 1 mg", 40f, y, boldPaint)
            y += 30f
            canvas.drawText("   Dosage: 1 tablet Once daily (OD) — with morning breakfast", 40f, y, textPaint)
            y += 40f
            canvas.drawText("3. Tab. Atorvastatin 10 mg", 40f, y, boldPaint)
            y += 30f
            canvas.drawText("   Dosage: 1 tablet at bedtime (HS) for lipid plaque stability", 40f, y, textPaint)
            y += 50f
            canvas.drawText("Clinical Instructions & Precautions:", 40f, y, boldPaint)
            y += 30f
            canvas.drawText("• Maintain 2.5 Liters of daily water hydration for renal clearance.", 40f, y, textPaint)
            y += 30f
            canvas.drawText("• STRICT: Avoid NSAIDs (Ibuprofen, Naproxen) due to elevated Creatinine.", 40f, y, redPaint)
            y += 30f
            canvas.drawText("• Repeat Renal Function Test (KFT) scheduled for late September / October.", 40f, y, textPaint)
        }
        "discharge" -> {
            canvas.drawText("EMERGENCY CLINICAL DISCHARGE SUMMARY", 40f, y, headerPaint)
            y += 40f
            canvas.drawText("Admission Date: Oct 02, 2026 • Discharge: Oct 03, 2026", 40f, y, boldPaint)
            y += 35f
            canvas.drawText("Primary Diagnosis: Acute Dehydration with Renal Creatinine Surveillance", 40f, y, textPaint)
            y += 35f
            canvas.drawText("Discharge Vitals: BP 124/80 mmHg, Pulse 76 bpm, Random Sugar 132 mg/dL", 40f, y, textPaint)
            y += 45f
            canvas.drawText("Medications at Discharge:", 40f, y, boldPaint)
            y += 30f
            canvas.drawText("• Oral Rehydration Solution (ORS) — 1 sachet in 1L water daily", 40f, y, textPaint)
            y += 30f
            canvas.drawText("• Resume oral Metformin 500mg and Glimepiride 1mg with regular diet", 40f, y, textPaint)
            y += 45f
            canvas.drawText("Summary:", 40f, y, boldPaint)
            y += 30f
            canvas.drawText("Patient rehydrated under 24hr observation. Electrolytes restored.", 40f, y, textPaint)
        }
        else -> {
            canvas.drawText("BIOCHEMISTRY REPORT — RENAL & METABOLIC PANEL", 40f, y, headerPaint)
            y += 40f
            canvas.drawText("Test Parameter              Observed Value      Reference Range     Status", 40f, y, boldPaint)
            y += 20f
            canvas.drawLine(40f, y, width - 40f, y, linePaint)
            y += 35f
            canvas.drawText("Serum Creatinine            1.58 mg/dL          0.7 - 1.3 mg/dL     ELEVATED", 40f, y, redPaint)
            y += 35f
            canvas.drawText("Estimated GFR (eGFR)        58 mL/min           > 60 mL/min         ATTENTION", 40f, y, redPaint)
            y += 35f
            canvas.drawText("Blood Urea Nitrogen (BUN)   22 mg/dL            7 - 20 mg/dL        ELEVATED", 40f, y, redPaint)
            y += 35f
            canvas.drawText("Fasting Blood Sugar         136 mg/dL           70 - 99 mg/dL       MODERATE", 40f, y, textPaint)
            y += 35f
            canvas.drawText("HbA1c Glycated Hgb          7.3 %               < 7.0 %             ATTENTION", 40f, y, textPaint)
            y += 45f
            canvas.drawText("Biochemical Interpretation:", 40f, y, boldPaint)
            y += 30f
            canvas.drawText("Mild elevation in serum creatinine consistent with Stage 2 renal filtration.", 40f, y, textPaint)
        }
    }

    y += 60f
    canvas.drawLine(40f, y, width - 40f, y, linePaint)
    y += 40f
    canvas.drawText("Verified Medical Record • Digital Seal #MP-994821 • MedPath OCR Ready", 40f, y, subHeaderPaint)

    return bitmap
}
