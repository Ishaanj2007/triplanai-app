package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.TripEntity
import com.example.data.model.TripPlan
import com.example.data.remote.RetrofitClient
import com.example.repository.TripRepository
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class GeminiModelOption(
    val id: String,
    val displayName: String,
    val description: String,
    val badge: String
)

sealed interface PlanningState {
    object Idle : PlanningState
    object Planning : PlanningState
    data class Success(val plan: TripPlan) : PlanningState
    data class Error(val message: String) : PlanningState
}

class TripViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TripRepository
    private val sharedPrefs = application.getSharedPreferences("vaygoo_prefs", android.content.Context.MODE_PRIVATE)

    val availableGeminiModels = listOf(
        GeminiModelOption(
            id = "gemini-2.5-flash",
            displayName = "Gemini 2.5 Flash",
            description = "High speed, ultra-reliable formatting & high stability.",
            badge = "Recommended"
        ),
        GeminiModelOption(
            id = "gemini-3.5-flash",
            displayName = "Gemini 3.5 Flash",
            description = "Next-gen balanced reasoning and travel planning.",
            badge = "New"
        ),
        GeminiModelOption(
            id = "gemini-3.1-flash-lite-preview",
            displayName = "Gemini 3.1 Flash Lite",
            description = "Ultra-fast response with minimal latency.",
            badge = "Fast"
        ),
        GeminiModelOption(
            id = "gemini-flash-latest",
            displayName = "Gemini Flash (Latest)",
            description = "Always targets Google's latest stable Flash model.",
            badge = "Auto"
        ),
        GeminiModelOption(
            id = "gemini-3.1-pro-preview",
            displayName = "Gemini 3.1 Pro",
            description = "Maximum depth and complex reasoning for detailed itineraries.",
            badge = "Pro"
        )
    )

    // User connected key, model, and preferences
    val userApiKey = MutableStateFlow(sharedPrefs.getString("user_api_key", "") ?: "")
    val groqApiKey = MutableStateFlow(sharedPrefs.getString("groq_api_key", "") ?: "")
    val autoSaveEnabled = MutableStateFlow(sharedPrefs.getBoolean("auto_save_enabled", false))
    val selectedTheme = MutableStateFlow(sharedPrefs.getInt("selected_theme", 0))
    val selectedPersonality = MutableStateFlow(sharedPrefs.getString("selected_personality", "Friendly") ?: "Friendly")
    val selectedModel = MutableStateFlow(sharedPrefs.getString("selected_gemini_model", "gemini-2.5-flash") ?: "gemini-2.5-flash")

    fun selectGeminiModel(modelId: String) {
        sharedPrefs.edit().putString("selected_gemini_model", modelId).apply()
        selectedModel.value = modelId
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TripRepository(database.tripDao())
        
        viewModelScope.launch {
            selectedTheme.collect { theme ->
                sharedPrefs.edit().putInt("selected_theme", theme).apply()
            }
        }

        viewModelScope.launch {
            selectedPersonality.collect { personality ->
                sharedPrefs.edit().putString("selected_personality", personality).apply()
            }
        }
    }

    private fun sanitizeErrorMessage(message: String?): String {
        if (message == null) return "An unexpected error occurred."
        // Remove API key from URL query parameters (e.g., key=AIzaSy...)
        val keyQueryRegex = "(?i)key=[a-zA-Z0-9_\\-]+".toRegex()
        var sanitized = message.replace(keyQueryRegex, "key=***")
        // Also remove any direct API key matching AIzaSy...
        val apiRegex = "(?i)AIzaSy[a-zA-Z0-9_\\-]+".toRegex()
        sanitized = sanitized.replace(apiRegex, "***")
        return sanitized
    }

    val testConnectionStatus = MutableStateFlow<String?>(null)
    val isTestingConnection = MutableStateFlow(false)

    fun testGeminiConnection() {
        viewModelScope.launch {
            isTestingConnection.value = true
            testConnectionStatus.value = "Testing Gemini connection..."
            try {
                val key = if (userApiKey.value.isNotBlank()) userApiKey.value else BuildConfig.GEMINI_API_KEY
                if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
                    testConnectionStatus.value = "AI service is unconfigured. Please connect a custom Gemini API key in the field below."
                    isTestingConnection.value = false
                    return@launch
                }
                
                val testPrompt = "Ping. Respond in 2 words: Connected successfully."
                val request = com.example.data.remote.GenerateContentRequest(
                    contents = listOf(
                        com.example.data.remote.Content(
                            parts = listOf(com.example.data.remote.Part(text = testPrompt))
                        )
                    ),
                    generationConfig = com.example.data.remote.GenerationConfig(
                        responseMimeType = "text/plain",
                        temperature = 0.1f
                    )
                )
                
                val response = RetrofitClient.service.generateContent(selectedModel.value, key, request)
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!reply.isNullOrBlank()) {
                    testConnectionStatus.value = "SUCCESS: Model ${selectedModel.value} connected and active."
                } else {
                    testConnectionStatus.value = "Connected to service, but received an empty response. Model: ${selectedModel.value}"
                }
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val currentM = selectedModel.value
                val reason = when (code) {
                    400 -> "Invalid request parameter for $currentM."
                    401 -> "Invalid or expired API key."
                    403 -> "Access restricted for this API key or model."
                    404 -> "Model $currentM is not available on this endpoint."
                    429 -> "Rate limit reached for $currentM. Try switching to Gemini 2.5 Flash."
                    500, 502, 503, 504 -> "Service is temporarily overloaded. Please retry in a few moments."
                    else -> "Connection returned HTTP code $code."
                }
                testConnectionStatus.value = "Connection issue: $reason"
            } catch (e: java.io.IOException) {
                testConnectionStatus.value = "Network error: Unable to reach travel planning service. Please check your internet connection."
            } catch (e: Exception) {
                testConnectionStatus.value = "Connection test could not be completed. Please check your network."
            } finally {
                isTestingConnection.value = false
            }
        }
    }

    fun validateAndSetUserApiKey(key: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val cleanKey = key.trim()
            if (cleanKey.isBlank()) {
                onResult(false, "Key cannot be empty.")
                return@launch
            }
            try {
                // Perform a robust model listing check to verify key authenticity
                val response = RetrofitClient.service.listModels(apiKey = cleanKey)
                if (response.models != null) {
                    sharedPrefs.edit().putString("user_api_key", cleanKey).apply()
                    userApiKey.value = cleanKey
                    onResult(true, null)
                } else {
                    onResult(false, "Failed to retrieve models. Verify your key.")
                }
            } catch (e: java.io.IOException) {
                onResult(false, "Network error: Please check your internet connection.")
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val errorMsg = when (code) {
                    400 -> "API Key validation failed. Please check for spelling mistakes."
                    403 -> "Validation failed. This API Key is restricted, blocked, or invalid."
                    404 -> "API endpoint not found. Please double-check your key."
                    429 -> "Rate limit hit. The key is valid, but is currently rate-limited by Google."
                    else -> "Validation failed with HTTP status code $code. Please double-check your key."
                }
                onResult(false, errorMsg)
            } catch (e: Exception) {
                val rawMessage = e.localizedMessage ?: "API validation failed. Verify your key."
                onResult(false, sanitizeErrorMessage(rawMessage))
            }
        }
    }

    fun disconnectUserApiKey() {
        sharedPrefs.edit().remove("user_api_key").apply()
        userApiKey.value = ""
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("auto_save_enabled", enabled).apply()
        autoSaveEnabled.value = enabled
    }

    // List of saved trips from local database
    val savedTrips: StateFlow<List<TripEntity>> = repository.allTrips
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form selection states
    val destination = MutableStateFlow("")
    val fromLocation = MutableStateFlow("Mumbai")
    val durationDays = MutableStateFlow(3)
    val startDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val endDate = MutableStateFlow<LocalDate>(LocalDate.now().plusDays(2))

    fun updateDuration(days: Int) {
        val coercedDays = days.coerceIn(1, 14)
        durationDays.value = coercedDays
        endDate.value = startDate.value.plusDays((coercedDays - 1).coerceAtLeast(0).toLong())
    }

    fun updateDateRange(start: LocalDate, end: LocalDate?) {
        startDate.value = start
        if (end != null) {
            endDate.value = end
            val days = (ChronoUnit.DAYS.between(start, end) + 1).coerceIn(1, 14).toInt()
            durationDays.value = days
        } else {
            endDate.value = start
            durationDays.value = 1
        }
    }

    val travelerGroup = MutableStateFlow("Couple") // "Solo", "Couple", "Friends", "Family"
    val totalBudget = MutableStateFlow("₹10,000 - ₹20,000")
    val preferredTransportation = MutableStateFlow("No Preference") // "Train", "Bus", "Flight", "Car", "No Preference"
    val travelStyle = MutableStateFlow("Relaxation") // "Adventure", "Luxury", "Relaxation", "Nature", "Party", "Spiritual", etc.
    val travelPace = MutableStateFlow("Relaxed") // "Fast-paced", "Relaxed"
    val specialRequirements = MutableStateFlow("None") // "Vegetarian", "Pet Friendly", "Kids", "Senior Citizens", "Wheelchair Friendly", "Honeymoon", etc.

    // Settings states
    val shouldOpenSettingsTab = MutableStateFlow<Int?>(null)

    fun openSettings(tab: Int = 1) {
        shouldOpenSettingsTab.value = tab
    }

    fun consumeOpenSettings() {
        shouldOpenSettingsTab.value = null
    }

    // Active generating / planning state
    private val _planningState = MutableStateFlow<PlanningState>(PlanningState.Idle)
    val planningState: StateFlow<PlanningState> = _planningState.asStateFlow()

    // Plan being currently viewed
    private val _activeTripPlan = MutableStateFlow<TripPlan?>(null)
    val activeTripPlan: StateFlow<TripPlan?> = _activeTripPlan.asStateFlow()

    // To toggle sub-tabs in itinerary screen
    val activeItineraryTab = MutableStateFlow("Overview") // "Overview", "Itinerary", "Stay & Food", "Route", "Tips & Lists"

    fun resetPlanning() {
        _planningState.value = PlanningState.Idle
        _activeTripPlan.value = null
    }

    fun startTripGeneration() {
        viewModelScope.launch {
            _planningState.value = PlanningState.Planning
            try {
                val plan = repository.generateTripPlan(
                    destination = destination.value,
                    fromLocation = fromLocation.value,
                    travelerGroup = travelerGroup.value,
                    durationDays = durationDays.value,
                    totalBudget = totalBudget.value,
                    transportation = preferredTransportation.value,
                    travelStyle = travelStyle.value,
                    pace = travelPace.value,
                    specialRequirements = specialRequirements.value,
                    model = selectedModel.value,
                    userApiKey = userApiKey.value,
                    personality = selectedPersonality.value
                )
                _activeTripPlan.value = plan
                _planningState.value = PlanningState.Success(plan)
                
                // Automatically save the trip if auto-save setting is enabled
                if (autoSaveEnabled.value) {
                    saveActiveTrip()
                }
            } catch (e: java.io.IOException) {
                _planningState.value = PlanningState.Error(
                    "You appear to be offline. Please check your internet connection and try again."
                )
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val currentM = selectedModel.value
                val friendlyMessage = when (code) {
                    400 -> "Invalid request parameters for $currentM. Please check your selections and try again."
                    401 -> "Invalid or expired API key. Please check your key in Settings."
                    403 -> "Access restricted for model $currentM. Try switching models in Settings."
                    404 -> "Model $currentM is currently unavailable. Please select Gemini 2.5 Flash or Gemini 3.5 Flash."
                    429 -> "Rate limit reached on $currentM. Switch to Gemini 2.5 Flash or retry in a minute."
                    500, 502, 503, 504 -> "Service is temporarily overloaded on $currentM. Please retry in a few moments."
                    else -> "The service returned code $code. Please try again."
                }
                _planningState.value = PlanningState.Error(friendlyMessage)
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val friendlyMessage = when {
                    msg.contains("API key", ignoreCase = true) || msg.contains("GEMINI_API_KEY", ignoreCase = true) -> 
                        "AI service is currently unavailable. Please check your internet connection or enter your Gemini API key in Settings."
                    msg.contains("unresolved reference", ignoreCase = true) || msg.contains("host", ignoreCase = true) -> 
                        "No internet connection detected. Please connect to the internet and try again."
                    msg.contains("timeout", ignoreCase = true) -> 
                        "Connection timed out while generating your travel plan. Please try again."
                    msg.contains("parse", ignoreCase = true) || msg.contains("formatting", ignoreCase = true) -> 
                        "The plan was generated but could not be parsed. Please tap Generate again to retry!"
                    else -> "A temporary issue occurred while preparing your travel plan. Please check your settings or try again."
                }
                _planningState.value = PlanningState.Error(sanitizeErrorMessage(friendlyMessage))
            }
        }
    }

    fun viewSavedTrip(entity: TripEntity) {
        viewModelScope.launch {
            try {
                val adapter = RetrofitClient.moshiInstance.adapter(TripPlan::class.java)
                val plan = adapter.fromJson(entity.tripPlanJson)
                if (plan != null) {
                    _activeTripPlan.value = plan
                    _planningState.value = PlanningState.Success(plan)
                } else {
                    _planningState.value = PlanningState.Error("Failed to load this itinerary.")
                }
            } catch (e: Exception) {
                _planningState.value = PlanningState.Error("Failed to parse the saved itinerary: ${e.message}")
            }
        }
    }

    fun saveActiveTrip() {
        val plan = _activeTripPlan.value ?: return
        viewModelScope.launch {
            repository.saveTrip(
                destination = destination.value.ifBlank { plan.destination },
                durationDays = durationDays.value,
                travelerGroup = travelerGroup.value,
                travelStyle = travelStyle.value,
                plan = plan
            )
        }
    }

    fun deleteSavedTrip(id: Int) {
        viewModelScope.launch {
            repository.deleteTrip(id)
        }
    }

    fun saveGroqApiKey(key: String) {
        sharedPrefs.edit().putString("groq_api_key", key.trim()).apply()
        groqApiKey.value = key.trim()
    }

    fun disconnectGroqApiKey() {
        sharedPrefs.edit().remove("groq_api_key").apply()
        groqApiKey.value = ""
    }
}
