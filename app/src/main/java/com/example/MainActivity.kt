package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.MaintenanceScreen
import com.example.ui.components.NetworkStatusBanner
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
            val appConfigState by tripViewModel.appConfigState.collectAsState()
            val isUpdateDismissed by tripViewModel.isUpdateDialogDismissed.collectAsState()
            val showExplicitly by tripViewModel.showUpdateDialogExplicitly.collectAsState()

            val isOnline by tripViewModel.isOnline.collectAsState()
            val isUpdateMandatory = tripViewModel.remoteConfigManager.isUpdateMandatory()
            val isUpdateAvailable = tripViewModel.remoteConfigManager.isUpdateAvailable()
            val shouldShowUpdateDialog = isUpdateMandatory || (isUpdateAvailable && !isUpdateDismissed) || (showExplicitly && isUpdateAvailable)
            
            MyApplicationTheme(themeMode = themeMode) {
                if (appConfigState.maintenanceMode) {
                    MaintenanceScreen(
                        configState = appConfigState,
                        onRetry = { tripViewModel.checkForUpdates() }
                    )
                } else {
                    val navController = rememberNavController()
                    
                    Box(modifier = Modifier.fillMaxSize()) {
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

                        NetworkStatusBanner(
                            isOnline = isOnline,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .zIndex(10f)
                        )
                    }

                    if (shouldShowUpdateDialog) {
                        AppUpdateDialog(
                            configState = appConfigState,
                            isMandatory = isUpdateMandatory,
                            onDismiss = { tripViewModel.dismissUpdateDialog() },
                            onRefresh = { tripViewModel.checkForUpdates() }
                        )
                    }
                }
            }
        }
    }
}
