package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.TripEntity
import com.example.data.model.TripPlan
import com.example.data.remote.AppConfigState
import com.example.data.remote.GroqChatRequest
import com.example.data.remote.GroqMessage
import com.example.data.remote.GroqRetrofitClient
import com.example.data.remote.RemoteConfigManager
import com.example.data.remote.RetrofitClient
import com.example.repository.TripRepository
import com.example.BuildConfig
import com.example.util.NetworkMonitor
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

enum class ErrorKind {
    RATE_LIMIT,
    INVALID_KEY,
    NETWORK,
    GENERAL
}

sealed interface PlanningState {
    object Idle : PlanningState
    object Planning : PlanningState
    data class Success(val plan: TripPlan) : PlanningState
    data class Error(val message: String, val errorKind: ErrorKind = ErrorKind.GENERAL) : PlanningState
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
    fun getGroqApiKey(): String {
        val buildKey = BuildConfig.GROQ_API_KEY
        if (buildKey.isNotBlank() && buildKey != "DEFAULT_GROQ_KEY" && buildKey != "MY_GROQ_API_KEY") {
            return buildKey.trim()
        }
        val savedKey = sharedPrefs.getString("groq_api_key", "") ?: ""
        if (savedKey.isNotBlank() && savedKey != "DEFAULT_GROQ_KEY" && savedKey != "MY_GROQ_API_KEY") {
            return savedKey.trim()
        }
        return ""
    }

    companion object {
        fun normalizeTone(tone: String?): String {
            return when (tone?.trim()?.lowercase()) {
                "formal", "professional", "professional guide" -> "Formal"
                "funny", "funny & witty", "witty" -> "Funny"
                "roast my plan", "roast me", "roast" -> "Roast My Plan"
                else -> "Casual"
            }
        }
    }

    val groqApiKey = MutableStateFlow(getGroqApiKey())
    val autoSaveEnabled = MutableStateFlow(sharedPrefs.getBoolean("auto_save_enabled", false))
    val selectedTheme = MutableStateFlow(sharedPrefs.getInt("selected_theme", 0))
    val selectedPersonality = MutableStateFlow(normalizeTone(sharedPrefs.getString("selected_personality", "Casual")))
    val selectedModel = MutableStateFlow(sharedPrefs.getString("selected_gemini_model", "gemini-2.5-flash") ?: "gemini-2.5-flash")

    // Network Connectivity Monitor
    val networkMonitor = NetworkMonitor(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    // Remote Config & Update Management
    val remoteConfigManager = RemoteConfigManager(application)
    val appConfigState: StateFlow<AppConfigState> = remoteConfigManager.configState
    val isUpdateDialogDismissed = MutableStateFlow(false)
    val showUpdateDialogExplicitly = MutableStateFlow(false)

    fun dismissUpdateDialog() {
        isUpdateDialogDismissed.value = true
        showUpdateDialogExplicitly.value = false
    }

    fun openUpdateDialog() {
        showUpdateDialogExplicitly.value = true
    }

    fun checkForUpdates(onComplete: ((Boolean) -> Unit)? = null) {
        remoteConfigManager.fetchAndActivate(onComplete)
    }

    fun selectTone(tone: String) {
        val normalized = normalizeTone(tone)
        selectedPersonality.value = normalized
        sharedPrefs.edit().putString("selected_personality", normalized).apply()
    }

    fun selectGeminiModel(modelId: String) {
        sharedPrefs.edit().putString("selected_gemini_model", modelId).apply()
        selectedModel.value = modelId
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TripRepository(database.tripDao())

        // Initial non-blocking background fetch & activation of Remote Config
        remoteConfigManager.fetchAndActivate()
        
        viewModelScope.launch {
            selectedTheme.collect { theme ->
                sharedPrefs.edit().putInt("selected_theme", theme).apply()
            }
        }

        viewModelScope.launch {
            selectedPersonality.collect { personality ->
                val normalized = normalizeTone(personality)
                sharedPrefs.edit().putString("selected_personality", normalized).apply()
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

    val groqTestStatus = MutableStateFlow<String?>(null)
    val isTestingGroq = MutableStateFlow(false)

    fun testGeminiConnection() {
        viewModelScope.launch {
            if (!networkMonitor.isConnected()) {
                testConnectionStatus.value = "ERROR: You're offline. Connect to the internet."
                return@launch
            }
            isTestingConnection.value = true
            testConnectionStatus.value = "Testing Gemini connection..."
            try {
                val key = userApiKey.value.trim()
                if (key.isBlank()) {
                    testConnectionStatus.value = "No Gemini API key connected."
                    isTestingConnection.value = false
                    return@launch
                }
                
                val testPrompt = "Ping. Respond with two words: Connection verified."
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
                    testConnectionStatus.value = "SUCCESS: Gemini connected successfully."
                } else {
                    testConnectionStatus.value = "SUCCESS: Gemini connected."
                }
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val reason = when (code) {
                    400, 401, 403 -> "API key is invalid or unauthorized."
                    429 -> "Gemini usage limit reached for this key."
                    500, 502, 503, 504 -> "Gemini service temporarily busy. Please retry shortly."
                    else -> "Connection test failed (HTTP $code)."
                }
                testConnectionStatus.value = "ERROR: $reason"
            } catch (e: java.io.IOException) {
                testConnectionStatus.value = "ERROR: Network offline. Please check connection."
            } catch (e: Exception) {
                testConnectionStatus.value = "ERROR: Could not verify connection."
            } finally {
                isTestingConnection.value = false
            }
        }
    }

    fun testGroqConnection() {
        viewModelScope.launch {
            if (!networkMonitor.isConnected()) {
                groqTestStatus.value = "ERROR: You're offline. Connect to the internet."
                return@launch
            }
            isTestingGroq.value = true
            groqTestStatus.value = "Testing Groq connection..."
            val key = getGroqApiKey()
            if (key.isBlank()) {
                groqTestStatus.value = "NOT_CONFIGURED: No Groq API key set."
                isTestingGroq.value = false
                return@launch
            }

            try {
                val request = GroqChatRequest(
                    messages = listOf(
                        GroqMessage(
                            role = "system",
                            content = "You are TripAsk, a fast travel assistant for the user's current itinerary. Answer only questions related to the trip. Keep answers direct and useful, maximum 30 words."
                        ),
                        GroqMessage(role = "user", content = "Is my budget enough for this trip?")
                    ),
                    model = "openai/gpt-oss-20b",
                    temperature = 0.2f,
                    max_completion_tokens = 100,
                    reasoning_effort = "low",
                    include_reasoning = false,
                    stream = false
                )
                val response = GroqRetrofitClient.service.getChatCompletion(
                    authorization = "Bearer $key",
                    request = request
                )
                val reply = response.choices?.firstOrNull()?.message?.content?.trim()
                
                if (!reply.isNullOrBlank()) {
                    groqTestStatus.value = "SUCCESS: Groq & openai/gpt-oss-20b connected."
                } else {
                    groqTestStatus.value = "SUCCESS: Groq connection active."
                }
            } catch (e: retrofit2.HttpException) {
                groqTestStatus.value = "ERROR: TripAsk is temporarily unavailable. Please try again."
            } catch (e: java.io.IOException) {
                groqTestStatus.value = "ERROR: Network offline. Please check connection."
            } catch (e: Exception) {
                groqTestStatus.value = "ERROR: Could not verify Groq connection."
            } finally {
                isTestingGroq.value = false
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
            if (!networkMonitor.isConnected()) {
                onResult(false, "You're offline. Connect to the internet and try again.")
                return@launch
            }
            try {
                // Perform a lightweight model listing check to verify key authenticity
                val response = RetrofitClient.service.listModels(apiKey = cleanKey)
                if (response.models != null) {
                    sharedPrefs.edit().putString("user_api_key", cleanKey).apply()
                    userApiKey.value = cleanKey
                    onResult(true, null)
                } else {
                    onResult(false, "Your API key may be invalid.")
                }
            } catch (e: java.io.IOException) {
                onResult(false, "Check your internet connection and try again.")
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val errorMsg = when (code) {
                    400, 401, 403 -> "Your API key may be invalid."
                    429 -> "Your Gemini project may have reached its usage limit."
                    else -> "Couldn't connect to Gemini. Please check your key."
                }
                onResult(false, errorMsg)
            } catch (e: Exception) {
                onResult(false, "Couldn't connect to Gemini. Please check your key.")
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

    fun updateStartDate(start: LocalDate) {
        val today = LocalDate.now()
        val validStart = if (start.isBefore(today)) today else start
        startDate.value = validStart
        endDate.value = validStart.plusDays((durationDays.value - 1).coerceAtLeast(0).toLong())
    }

    val travelerGroup = MutableStateFlow("Couple") // "Solo", "Couple", "Friends", "Family"
    val totalBudget = MutableStateFlow("₹10,000 - ₹20,000")
    val preferredTransportation = MutableStateFlow("No Preference") // "Train", "Bus", "Flight", "Car", "No Preference"
    
    val selectedTravelStyles = MutableStateFlow<Set<String>>(setOf("Relaxation"))
    val travelStyle = MutableStateFlow("Relaxation") // "Adventure", "Luxury", "Relaxation", "Nature", "Party", "Spiritual", etc.

    fun toggleTravelStyle(styleName: String) {
        val current = selectedTravelStyles.value.toMutableSet()
        if (current.contains(styleName)) {
            if (current.size > 1) {
                current.remove(styleName)
            }
        } else {
            current.add(styleName)
        }
        selectedTravelStyles.value = current
        travelStyle.value = current.joinToString(", ")
    }

    val travelPace = MutableStateFlow("Relaxed") // "Fast-paced", "Relaxed"
    
    val selectedSpecialRequirements = MutableStateFlow<Set<String>>(setOf("None"))
    val specialRequirements = MutableStateFlow("None") // "Vegetarian", "Pet Friendly", "Kids", "Senior Citizens", "Wheelchair Friendly", "Honeymoon", etc.

    fun toggleSpecialRequirement(option: String) {
        val current = selectedSpecialRequirements.value.toMutableSet()
        if (option == "None") {
            current.clear()
            current.add("None")
        } else {
            current.remove("None")
            if (current.contains(option)) {
                current.remove(option)
                if (current.isEmpty()) {
                    current.add("None")
                }
            } else {
                current.add(option)
            }
        }
        selectedSpecialRequirements.value = current
        specialRequirements.value = current.joinToString(", ")
    }

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
            if (!networkMonitor.isConnected()) {
                _planningState.value = PlanningState.Error(
                    message = "Connect to the internet to generate your trip.",
                    errorKind = ErrorKind.NETWORK
                )
                return@launch
            }

            if (userApiKey.value.isBlank()) {
                _planningState.value = PlanningState.Error(
                    message = "Connect your Gemini API key to generate your itinerary.",
                    errorKind = ErrorKind.INVALID_KEY
                )
                return@launch
            }

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
                    personality = normalizeTone(selectedPersonality.value)
                )
                _activeTripPlan.value = plan
                _planningState.value = PlanningState.Success(plan)
                
                // Automatically save the trip if auto-save setting is enabled
                if (autoSaveEnabled.value) {
                    saveActiveTrip()
                }
            } catch (e: java.io.IOException) {
                _planningState.value = PlanningState.Error(
                    message = "Connect to the internet to generate your trip.",
                    errorKind = ErrorKind.NETWORK
                )
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                when (code) {
                    429 -> {
                        _planningState.value = PlanningState.Error(
                            message = "Gemini couldn't generate your itinerary right now. Please try again.",
                            errorKind = ErrorKind.RATE_LIMIT
                        )
                    }
                    400, 401, 403 -> {
                        _planningState.value = PlanningState.Error(
                            message = "Your Gemini API key isn't working. Please check the key and try again.",
                            errorKind = ErrorKind.INVALID_KEY
                        )
                    }
                    500, 502, 503, 504 -> {
                        _planningState.value = PlanningState.Error(
                            message = "Gemini couldn't generate your itinerary right now. Please try again.",
                            errorKind = ErrorKind.RATE_LIMIT
                        )
                    }
                    else -> {
                        _planningState.value = PlanningState.Error(
                            message = "Gemini couldn't generate your itinerary right now. Please try again.",
                            errorKind = ErrorKind.GENERAL
                        )
                    }
                }
            } catch (e: IllegalStateException) {
                if (e.message == "NO_GEMINI_KEY") {
                    _planningState.value = PlanningState.Error(
                        message = "Connect your Gemini API key to generate your itinerary.",
                        errorKind = ErrorKind.INVALID_KEY
                    )
                } else {
                    _planningState.value = PlanningState.Error(
                        message = "Gemini couldn't generate your itinerary right now. Please try again.",
                        errorKind = ErrorKind.GENERAL
                    )
                }
            } catch (e: Exception) {
                _planningState.value = PlanningState.Error(
                    message = "Gemini couldn't generate your itinerary right now. Please try again.",
                    errorKind = ErrorKind.GENERAL
                )
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
