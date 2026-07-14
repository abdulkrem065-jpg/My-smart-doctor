package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.AppointmentsScreen
import com.example.ui.ChatScreen
import com.example.ui.DashboardScreen
import com.example.ui.MedicationsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val db = AppDatabase.getDatabase(this)
        val repository = AppRepository(db.appointmentDao(), db.medicationDao())
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") { DashboardScreen(navController) }
                        composable("chat") { ChatScreen(navController) }
                        composable("appointments") { AppointmentsScreen(navController, repository) }
                        composable("medications") { MedicationsScreen(navController, repository) }
                        composable("library") { /* Optional placeholder */ }
                        composable("pharmacies") { /* Optional placeholder */ }
                        composable("reports") { /* Optional placeholder */ }
                    }
                }
            }
        }
    }
}
