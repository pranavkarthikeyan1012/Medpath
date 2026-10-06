package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.HealthDataManager
import com.example.data.MedicalEvent
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadRecordsScreen(navController: NavController) {
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var uploadComplete by remember { mutableStateOf(false) }

    LaunchedEffect(isUploading) {
        if (isUploading) {
            while (uploadProgress < 1f) {
                delay(300)
                uploadProgress += 0.1f
            }
            uploadComplete = true
            isUploading = false
            HealthDataManager.addMedicalEvent(
                MedicalEvent(
                    id = "evt_upload_${System.currentTimeMillis() % 10000}",
                    date = "Today",
                    type = "Lab Result",
                    title = "Uploaded Lab Report: Serum Electrolytes",
                    finding = "Sodium: 138 mEq/L, Potassium: 4.2 mEq/L. Electrolyte panel within normal limits.",
                    source = "Uploaded Medical Document",
                    relatedCondition = "Metabolic Health",
                    severity = "Routine",
                    doctorName = "AI Extraction",
                    notes = "Scanned and verified through MedPath Digital Health Tracker."
                )
            )
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Upload Records", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp).border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tap to upload documents", style = MaterialTheme.typography.titleMedium)
                    Text("PDF, JPG, PNG", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isUploading) {
                Text("Processing document...", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { uploadProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Simulating OCR and Entity Extraction...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (uploadComplete) {
                Text("Document Processed Successfully", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.navigate("timeline") }, modifier = Modifier.fillMaxWidth()) {
                    Text("View Updated Timeline")
                }
            } else {
                Button(
                    onClick = { 
                        isUploading = true 
                        uploadProgress = 0f
                        uploadComplete = false
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Upload Demo Document")
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Text("MedPath securely processes your documents using on-device simulation for this demo.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
