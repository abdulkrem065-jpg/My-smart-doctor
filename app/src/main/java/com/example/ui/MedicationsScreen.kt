package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.R
import com.example.data.AppRepository
import com.example.data.Medication
import com.example.workers.MedicationReminderWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedicationsViewModel(private val repository: AppRepository) : ViewModel() {
    val medications: StateFlow<List<Medication>> = repository.allMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMockMedication() {
        viewModelScope.launch {
            repository.insertMedication(
                Medication(
                    name = "Paracetamol",
                    dosage = "500mg",
                    timesPerDay = 3,
                    timeToTake = System.currentTimeMillis()
                )
            )
        }
    }
}

class MedicationsViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicationsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(navController: NavController, repository: AppRepository) {
    val viewModel: MedicationsViewModel = viewModel(factory = MedicationsViewModelFactory(repository))
    val medications by viewModel.medications.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.medications)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.addMockMedication() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_medication))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    // Trigger worker directly for testing
                    androidx.work.WorkManager.getInstance(context).enqueue(
                        androidx.work.OneTimeWorkRequestBuilder<MedicationReminderWorker>().build()
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("تذكير الآن")
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(medications) { med ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = med.name, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Dosage: ${med.dosage}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "${med.timesPerDay} times a day", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
