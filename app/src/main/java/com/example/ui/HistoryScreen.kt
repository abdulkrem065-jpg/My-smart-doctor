package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.data.Diagnosis
import com.example.data.AppRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    repository: AppRepository,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(repository))
) {
    val diagnoses by viewModel.diagnoses.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سجل التشخيصات") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(diagnoses) { diagnosis ->
                DiagnosisCard(diagnosis)
            }
        }
    }
}

@Composable
fun DiagnosisCard(diagnosis: Diagnosis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🩺 التشخيص: ${diagnosis.diagnosisText.take(80)}...",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "📋 الأعراض: ${diagnosis.symptoms}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "⏱️ ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(diagnosis.timestamp))}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
