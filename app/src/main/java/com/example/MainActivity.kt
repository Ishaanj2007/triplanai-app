package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ItineraryScreen
import com.example.ui.screens.QuestionsScreen
import com.example.ui.screens.SavedTripsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TripViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val tripViewModel: TripViewModel = viewModel()
            val themeMode by tripViewModel.selectedTheme.collectAsState()
            
            MyApplicationTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("home") {
                        HomeScreen(
                            viewModel = tripViewModel,
                            onNavigateToQuestions = { navController.navigate("questions") },
                            onNavigateToItinerary = { navController.navigate("itinerary") },
                            onNavigateToSavedTrips = { navController.navigate("saved_trips") }
                        )
                    }
                    composable("questions") {
                        QuestionsScreen(
                            viewModel = tripViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToItinerary = { 
                                navController.navigate("itinerary") {
                                    popUpTo("home")
                                }
                            }
                        )
                    }
                    composable("itinerary") {
                        ItineraryScreen(
                            viewModel = tripViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("saved_trips") {
                        SavedTripsScreen(
                            viewModel = tripViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToItinerary = { navController.navigate("itinerary") }
                        )
                    }
                }
            }
        }
    }
}
