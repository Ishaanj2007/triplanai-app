package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.viewmodel.PlanningState
import com.example.viewmodel.TripViewModel
import com.example.data.remote.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit
) {
    val planningState by viewModel.planningState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeTripPlan by viewModel.activeTripPlan.collectAsState()
    val activeTab by viewModel.activeItineraryTab.collectAsState()

    val savedTrips by viewModel.savedTrips.collectAsState()
    val matchingSavedTrip = remember(activeTripPlan, savedTrips) {
        activeTripPlan?.let { plan ->
            savedTrips.find { saved ->
                saved.destination.equals(plan.destination, ignoreCase = true) &&
                saved.durationDays == plan.durationDays
            }
        }
    }
    val isSavedByMe = matchingSavedTrip != null

    var showExitConfirmationDialog by remember { mutableStateOf(false) }

    // Intercept hardware system back button if the trip is generated but not saved
    androidx.activity.compose.BackHandler(enabled = planningState is PlanningState.Success && !isSavedByMe) {
        showExitConfirmationDialog = true
    }

    if (showExitConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmationDialog = false },
            title = { Text("Leave Trip?", color = ArtTextDark, fontWeight = FontWeight.Bold) },
            text = { Text("You haven't saved this itinerary. Save this trip for later?", color = ArtGrayMuted) },
            containerColor = ArtCardBackground,
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveActiveTrip()
                        showExitConfirmationDialog = false
                        android.widget.Toast.makeText(context, "Trip saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                ) {
                    Text("Save & Close", color = ArtPrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            showExitConfirmationDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Discard", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = { showExitConfirmationDialog = false }
                    ) {
                        Text("Cancel", color = ArtGrayMuted)
                    }
                }
            },
            modifier = Modifier.border(2.dp, ArtBorderDark, RoundedCornerShape(28.dp))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArtBackground)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (planningState is PlanningState.Success && !isSavedByMe) {
                            showExitConfirmationDialog = true
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(ArtCardBackground, CircleShape)
                        .border(2.dp, ArtBorderDark, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to home",
                        tint = ArtTextDark
                    )
                }

                Text(
                    text = if (planningState is PlanningState.Planning) "" else "TriplanAi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    color = ArtTextDark
                )

                // Save Trip Toggle (Reactive & State-driven)
                if (planningState is PlanningState.Success) {
                    IconButton(
                        onClick = {
                            if (isSavedByMe) {
                                matchingSavedTrip?.let { 
                                    viewModel.deleteSavedTrip(it.id)
                                    android.widget.Toast.makeText(context, "Trip removed from Saved Trips!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                viewModel.saveActiveTrip()
                                android.widget.Toast.makeText(context, "Trip saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isSavedByMe) ArtMintGreen else ArtCardBackground,
                                CircleShape
                            )
                            .border(
                                2.dp,
                                ArtBorderDark,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isSavedByMe) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save Trip",
                            tint = ArtTextDark
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            // Central state viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (planningState) {
                    is PlanningState.Planning -> {
                        val userApiKeyVal by viewModel.userApiKey.collectAsState()
                        LoadingPlannerScreen(isUsingCustomKey = userApiKeyVal.isNotBlank())
                    }
                    is PlanningState.Error -> {
                        val errState = planningState as PlanningState.Error
                        ErrorScreen(
                            message = errState.message,
                            errorKind = errState.errorKind,
                            viewModel = viewModel,
                            onRetry = {
                                viewModel.startTripGeneration()
                            },
                            onNavigateBack = onNavigateBack
                        )
                    }
                    is PlanningState.Success -> {
                        activeTripPlan?.let { plan ->
                            SuccessItineraryViewport(
                                plan = plan,
                                activeTab = activeTab,
                                onTabSelected = { viewModel.activeItineraryTab.value = it },
                                viewModel = viewModel
                            )
                        } ?: ErrorScreen(
                            message = "No travel plan received from AI.",
                            errorKind = com.example.viewmodel.ErrorKind.GENERAL,
                            viewModel = viewModel,
                            onRetry = {
                                viewModel.startTripGeneration()
                            },
                            onNavigateBack = onNavigateBack
                        )
                    }
                    else -> {
                        // Idle fallback
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Please enter a destination to start planning.", color = Color.White)
                        }
                    }
                }
            }
        }

        if (planningState is PlanningState.Success) {
            activeTripPlan?.let { plan ->
                TripAskChatbot(viewModel = viewModel, plan = plan)
            }
        }
    }
}

@Composable
fun TripAskChatbot(
    viewModel: TripViewModel,
    plan: TripPlan
) {
    var showChatBot by remember { mutableStateOf(false) }
    var chatInputText by remember { mutableStateOf("") }
    
    var currentQuestion by remember { mutableStateOf<String?>(null) }
    var currentAnswer by remember { mutableStateOf<String?>(null) }
    var isChatLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    val groqApiKeyVal by viewModel.groqApiKey.collectAsState()
    val hasGroqApiKey = groqApiKeyVal.isNotBlank() || (com.example.BuildConfig.GROQ_API_KEY != "MY_GROQ_API_KEY" && com.example.BuildConfig.GROQ_API_KEY.isNotBlank())

    val coroutineScope = rememberCoroutineScope()

    // Drag to dismiss gesture states
    var swipeOffset by remember { mutableStateOf(0f) }
    val swipeThreshold = 180f

    fun answerLocally(question: String, plan: TripPlan): String? {
        val q = question.lowercase().trim().removeSuffix("?").trim()

        // Exact stored facts retrieval only
        return when {
            // Destination
            q == "what is my destination" || q == "where am i going" || q == "destination" || q == "where is my trip" -> {
                "Your destination is ${plan.destination}."
            }
            // Duration / Days
            q == "how many days is my trip" || q == "how many days is the trip" || q == "how long is my trip" || q == "trip duration" || q == "how many days" -> {
                "Your trip to ${plan.destination} is planned for ${plan.durationDays} days."
            }
            // Total Budget
            q == "what is my total budget" || q == "what is my total estimated budget" || q == "what is my budget" || q == "total budget" || q == "budget" -> {
                "Your estimated total budget is ${plan.estimatedTotalBudget}."
            }
            // Hotel / Accommodation Selected
            q == "what hotel did i select" || q == "what accommodation did i select" || q == "where am i staying" || q == "what hotel is selected" -> {
                val suggestionsList = plan.accommodationGuide.suggestions.take(2).joinToString { it.name }
                "For ${plan.destination}, recommended stay area is ${plan.accommodationGuide.bestAreaToStay}. Options: $suggestionsList."
            }
            // Transport Selected
            q == "which transport did i select" || q == "what transport did i select" || q == "how am i traveling" -> {
                "Recommended route: ${plan.routes.recommendedRoute}. Transport options: " +
                plan.routes.options.joinToString { "${it.transportMode} (${it.estimatedFare})" } + "."
            }
            // Day Plan
            q.matches(Regex("^(what is (planned|on) day (\\d+)|day (\\d+) plan)$")) -> {
                val dayRegex = Regex("^(what is (planned|on) day (\\d+)|day (\\d+) plan)$")
                val match = dayRegex.find(q)
                val dayNumStr = match?.groupValues?.get(3)?.ifEmpty { match.groupValues.get(4) } ?: ""
                val dayNum = dayNumStr.toIntOrNull()
                if (dayNum != null) {
                    val day = plan.days.find { it.dayNumber == dayNum }
                    if (day != null) {
                        "Day $dayNum (${day.theme}): " + day.activities.joinToString { it.title } + "."
                    } else null
                } else null
            }
            else -> null
        }
    }

    fun handleSend(question: String) {
        if (question.isBlank()) return
        currentQuestion = question
        currentAnswer = null
        isChatLoading = true
        hasError = false

        android.util.Log.d("TripAsk", "[TripAsk] Question started: $question")

        val localAnswer = answerLocally(question, plan)
        if (localAnswer != null) {
            android.util.Log.d("TripAsk", "[TripAsk] Route: LOCAL - Answered from local itinerary data")
            coroutineScope.launch {
                delay(300)
                currentAnswer = localAnswer
                isChatLoading = false
            }
        } else {
            android.util.Log.d("TripAsk", "[TripAsk] Route: GROQ (llama-3.1-8b-instant)")

            val apiKey = if (groqApiKeyVal.isNotBlank()) {
                groqApiKeyVal
            } else {
                com.example.BuildConfig.GROQ_API_KEY
            }

            val keyExists = apiKey.isNotBlank() && apiKey != "MY_GROQ_API_KEY"
            android.util.Log.d("TripAsk", "[TripAsk] Groq key exists: $keyExists")

            if (!keyExists) {
                isChatLoading = false
                hasError = true
                currentAnswer = "TripAsk requires a Groq API Key. Please configure your Groq key in Settings."
                return
            }

            coroutineScope.launch {
                try {
                    val systemContextPrompt = """
                        You are TripAsk, a short contextual travel assistant. Answer only about the user's current itinerary and travel question. Give one concise sentence, normally 15-30 words. Do not behave like a general chatbot.
                    """.trimIndent()

                    val itineraryContext = """
                        Current Itinerary Details:
                        Destination: ${plan.destination}
                        Duration: ${plan.durationDays} days
                        Traveler Group: ${plan.travelerGroup}
                        Travel Style: ${plan.travelStyle}
                        Total Budget: ${plan.estimatedTotalBudget}
                        Best Area To Stay: ${plan.accommodationGuide.bestAreaToStay}
                        Hotels: ${plan.accommodationGuide.suggestions.joinToString { it.name }}
                        Must Try Food: ${plan.foodGuide.mustTryDishes.joinToString()}
                        Recommended Route: ${plan.routes.recommendedRoute}
                        Day Plans: ${plan.days.joinToString("; ") { "Day ${it.dayNumber} (${it.theme}): " + it.activities.joinToString { a -> a.title } }}
                        Local Tips: ${plan.localTips.joinToString()}
                        Avoid: ${plan.thingsToAvoid.joinToString()}
                        Packing: ${plan.packingList.joinToString()}

                        User Question: $question
                    """.trimIndent()

                    val request = GroqChatRequest(
                        messages = listOf(
                            GroqMessage(role = "system", content = systemContextPrompt),
                            GroqMessage(role = "user", content = itineraryContext)
                        ),
                        model = "llama-3.1-8b-instant",
                        temperature = 0.2f,
                        max_completion_tokens = 60
                    )

                    android.util.Log.d("TripAsk", "[TripAsk] Starting Groq request. Model: ${request.model}")
                    android.util.Log.d("TripAsk", "[TripAsk] Request body: $request")
                    android.util.Log.d("TripAsk", "[TripAsk] Authorization Header: Bearer ${if (apiKey.length > 10) apiKey.take(6) + "..." + apiKey.takeLast(4) else "SHORT_KEY"}")

                    val response = GroqRetrofitClient.service.getChatCompletion(
                        authorization = "Bearer " + apiKey,
                        request = request
                    )

                    val reply = response.choices?.firstOrNull()?.message?.content?.trim()
                    android.util.Log.d("TripAsk", "[TripAsk] Response received successfully.")
                    android.util.Log.d("TripAsk", "[TripAsk] Response parsing result: ${if (reply != null) "SUCCESS. Length: ${reply.length}" else "EMPTY"}")

                    if (!reply.isNullOrBlank()) {
                        currentAnswer = reply
                    } else {
                        android.util.Log.e("TripAsk", "[TripAsk] Error: Received empty reply from Groq.")
                        hasError = true
                        currentAnswer = "TripAsk couldn't reach AI. Please check your Groq connection and try again."
                    }
                } catch (e: retrofit2.HttpException) {
                    val statusCode = e.code()
                    val errorBody = e.response()?.errorBody()?.string() ?: "NO_ERROR_BODY"
                    android.util.Log.e("TripAsk", "[TripAsk] HTTP Error code: $statusCode")
                    android.util.Log.e("TripAsk", "[TripAsk] HTTP Error body: $errorBody")
                    
                    val errorMessage = when (statusCode) {
                        401 -> "Unauthorized: Invalid API Key."
                        404 -> "Not Found: Check the API endpoint or model name."
                        429 -> "Rate Limit Exceeded: Slow down requests."
                        else -> "HTTP Error $statusCode"
                    }
                    
                    hasError = true
                    currentAnswer = "TripAsk Error ($errorMessage). Please try again later."
                } catch (e: java.io.IOException) {
                    android.util.Log.e("TripAsk", "[TripAsk] Network IO Exception: ${e.message}")
                    e.printStackTrace()
                    hasError = true
                    currentAnswer = "Network error. Please check your internet connection."
                } catch (e: Exception) {
                    android.util.Log.e("TripAsk", "[TripAsk] Caught Unexpected Exception: ${e.javaClass.simpleName} - ${e.message}")
                    e.printStackTrace()
                    hasError = true
                    currentAnswer = "TripAsk encountered an unexpected error. Please try again."
                } finally {
                    isChatLoading = false
                }
            }
        }
    }

    // 1. FAB overlay (bottom-right)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { 
                showChatBot = true 
                swipeOffset = 0f
            },
            containerColor = ArtPrimaryPurple,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .size(56.dp)
                .border(2.5.dp, ArtBorderDark, CircleShape)
                .testTag("trip_ask_fab")
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Ask TripAsk",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // 2. Full-screen scrim & sliding bottom sheet layout
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Transparent Dark Scrim
        AnimatedVisibility(
            visible = showChatBot,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { showChatBot = false }
            )
        }

        // Draggable sliding bottom sheet
        AnimatedVisibility(
            visible = showChatBot,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 280)
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .fillMaxHeight(0.62f)
                    .offset { androidx.compose.ui.unit.IntOffset(0, swipeOffset.coerceAtLeast(0f).toInt()) }
                    .draggable(
                        state = rememberDraggableState { delta ->
                            if (delta > 0 || swipeOffset > 0) {
                                swipeOffset += delta
                            }
                        },
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            if (swipeOffset > swipeThreshold) {
                                showChatBot = false
                            }
                            swipeOffset = 0f
                        }
                    )
                    .clickable(enabled = false) {}, // prevent clicks leaking through
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF6FF)), // Elegant pastel lilac-white
                border = BorderStroke(2.5.dp, ArtBorderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    // Small top drag indicator bar
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .background(ArtBorderDark.copy(alpha = 0.4f), CircleShape)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title Header section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ArtPrimaryPurple,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "TripAsk",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ArtTextDark
                                )
                                Text(
                                    text = "Ask anything about your trip",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ArtGrayMuted
                                )
                            }
                        }

                        // Circular Close Button (X)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White, CircleShape)
                                .border(1.5.dp, ArtBorderDark, CircleShape)
                                .clickable { showChatBot = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = ArtTextDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Suggested Questions Horizontal list
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val suggestionItems = listOf(
                            Triple("Best food?", Icons.Default.Restaurant, Color(0xFFE57373)),
                            Triple("Is my budget enough?", Icons.Default.AccountBalanceWallet, Color(0xFF81C784)),
                            Triple("What should I pack?", Icons.Default.Luggage, Color(0xFF64B5F6)),
                            Triple("Weather?", Icons.Default.WbSunny, Color(0xFFFFB74D)),
                            Triple("Best time to visit?", Icons.Default.CalendarToday, Color(0xFFBA68C8)),
                            Triple("Save money?", Icons.Default.Savings, Color(0xFF4DB6AC)),
                            Triple("Is this route good?", Icons.Default.Map, Color(0xFF90A4AE))
                        )
                        itemsIndexed(suggestionItems) { _, item ->
                            SuggestionChip(
                                text = item.first,
                                icon = item.second,
                                iconColor = item.third,
                                onClick = {
                                    if (!isChatLoading) {
                                        chatInputText = ""
                                        handleSend(item.first)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Question Input Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { chatInputText = it },
                            placeholder = { Text("Type your question...", fontSize = 14.sp, color = ArtGrayMuted) },
                            singleLine = true,
                            enabled = !isChatLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = ArtPrimaryPurple,
                                unfocusedBorderColor = ArtBorderDark,
                                focusedTextColor = ArtTextDark,
                                unfocusedTextColor = ArtTextDark
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    if (chatInputText.isNotBlank()) {
                                        val textToSend = chatInputText
                                        chatInputText = ""
                                        handleSend(textToSend)
                                    }
                                }
                            )
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(
                                    if (chatInputText.isNotBlank() && !isChatLoading) ArtPrimaryPurple else ArtPrimaryPurple.copy(alpha = 0.5f),
                                    CircleShape
                                )
                                .border(2.dp, ArtBorderDark, CircleShape)
                                .clickable(enabled = chatInputText.isNotBlank() && !isChatLoading) {
                                    val textToSend = chatInputText
                                    chatInputText = ""
                                    handleSend(textToSend)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Answer / Setup Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (isChatLoading) {
                            // Typing indicator state
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, ArtBorderDark, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EFFF))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = ArtPrimaryPurple
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "TripAsk is typing...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ArtTextDark
                                    )
                                }
                            }
                        } else if (hasError && currentQuestion != null && !hasGroqApiKey) {
                            // Custom API Key configuration card within sheet
                            var enteredKey by remember { mutableStateOf("") }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, ArtBorderDark, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF6FF))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(ArtPeachGold, CircleShape)
                                                .border(1.5.dp, ArtBorderDark, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VpnKey,
                                                contentDescription = null,
                                                tint = ArtTextDark,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Groq API Key Required",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = ArtTextDark
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Custom queries require a Groq API Key. Common questions (budget, hotels, days, food, wear) are answered locally for free!",
                                        fontSize = 11.sp,
                                        color = ArtGrayMuted,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = enteredKey,
                                            onValueChange = { enteredKey = it },
                                            placeholder = { Text("gsk_...", fontSize = 12.sp, color = ArtGrayMuted) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White,
                                                focusedBorderColor = ArtPrimaryPurple,
                                                unfocusedBorderColor = ArtBorderDark,
                                                focusedTextColor = ArtTextDark,
                                                unfocusedTextColor = ArtTextDark
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                if (enteredKey.isNotBlank()) {
                                                    viewModel.saveGroqApiKey(enteredKey)
                                                    handleSend(currentQuestion ?: "")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ArtPrimaryPurple),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .height(46.dp)
                                                .border(1.5.dp, ArtBorderDark, RoundedCornerShape(12.dp))
                                        ) {
                                            Text(
                                                text = "Connect",
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (currentAnswer != null) {
                            // Beautiful ONE Answer Card display (NO powered by labels, NO chat list history)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, ArtBorderDark, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EFFF))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = ArtPrimaryPurple,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Answer",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = ArtTextDark
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color(0xFF2E7D32).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "New",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = ArtGrayMuted,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Just now",
                                                fontSize = 11.sp,
                                                color = ArtGrayMuted
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = currentAnswer ?: "",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ArtTextDark,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        } else {
                            // Unasked / Greeting state
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, ArtBorderDark, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "How can I help?",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ArtTextDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap a suggestion above or type a quick question about ${plan.destination}!",
                                        fontSize = 12.sp,
                                        color = ArtGrayMuted,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. Information Card (helper at the bottom of sheet)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF9E6), RoundedCornerShape(16.dp))
                            .border(1.5.dp, ArtBorderDark, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFBC02D),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TripAsk uses your itinerary details to provide quick and relevant travel suggestions.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ArtTextDark,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.5.dp, ArtBorderDark, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ArtTextDark
        )
    }
}


// 1. GORGEOUS LOADING SCREEN (GAMIFIED BOARD-GAME LOGISTIC STEPS)
@Composable
fun LoadingPlannerScreen(isUsingCustomKey: Boolean) {
    var loadingStep by remember { mutableStateOf(0) }
    val loadingSteps = listOf(
        "Initiating smart routing algorithms...",
        "Evaluating destination climate & seasons...",
        "Sifting boutique accommodation catalogs...",
        "Identifying street food hubs & hidden cafes...",
        "Grouping nearby coordinates for less transit...",
        "Polishing your personalized board-game guide..."
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)
            loadingStep = (loadingStep + 1) % loadingSteps.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "loading_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(ArtSoftLavender, CircleShape)
                .border(2.5.dp, ArtBorderDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = ArtPrimaryPurple,
                modifier = Modifier
                    .size(44.dp)
                    .rotate(angle)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "DESIGNING YOUR TRIP",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ArtPrimaryPurple,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedContent(
            targetState = loadingSteps[loadingStep],
            transitionSpec = {
                slideInVertically { height -> height } + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + fadeOut()
            },
            label = "loading_text"
        ) { text ->
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ArtTextDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This takes about 10-15 seconds. Our AI travel planner is thinking through hotels, logistics, weather, and opening hours...",
            fontSize = 13.sp,
            color = ArtGrayMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            color = if (isUsingCustomKey) ArtMintGreen.copy(alpha = 0.2f) else ArtSecondaryPurple.copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, ArtBorderDark),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isUsingCustomKey) Icons.Default.VpnKey else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isUsingCustomKey) Color(0xFF0F5229) else ArtPrimaryPurple,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isUsingCustomKey) "Processing via Custom Gemini API Key" else "Processing via Default App AI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ArtTextDark
                )
            }
        }
    }
}

// 2. DETAILED ERROR SCREEN
@Composable
fun ErrorScreen(
    message: String,
    errorKind: com.example.viewmodel.ErrorKind? = null,
    viewModel: TripViewModel,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val activeModel by viewModel.selectedModel.collectAsState()
    val userApiKeyVal by viewModel.userApiKey.collectAsState()
    var showGeminiSetupDialog by remember { mutableStateOf(false) }

    val isInvalidKey = errorKind == com.example.viewmodel.ErrorKind.INVALID_KEY || userApiKeyVal.isBlank()
    val isRateLimit = errorKind == com.example.viewmodel.ErrorKind.RATE_LIMIT
    val isNetwork = errorKind == com.example.viewmodel.ErrorKind.NETWORK

    val errorTitle = when {
        isInvalidKey -> "Invalid Gemini API key"
        isRateLimit -> "Gemini rate limit reached"
        isNetwork -> "Network Connection Issue"
        else -> "Couldn't generate this trip"
    }

    val errorDescription = when {
        isInvalidKey -> "Please check your API key and try again."
        isRateLimit -> "Please wait a moment or use your own API key with available quota."
        isNetwork -> "Could not connect to Gemini servers. Please check your internet connection."
        message.isNotBlank() -> message
        else -> "The travel planner couldn't complete the request. Please try again or re-connect your key."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
            border = BorderStroke(1.5.dp, ArtBorderDark)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isInvalidKey || isRateLimit) Color(0xFFFEE2E2) else ArtTertiaryPink, CircleShape)
                        .border(1.5.dp, if (isInvalidKey || isRateLimit) Color(0xFFDC2626) else ArtBorderDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isInvalidKey) Icons.Default.KeyOff else if (isRateLimit) Icons.Default.HourglassTop else Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = if (isInvalidKey || isRateLimit) Color(0xFFDC2626) else ArtTextDark,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = errorTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArtTextDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = errorDescription,
                    fontSize = 13.sp,
                    color = ArtGrayMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                // Active model badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ArtSecondaryPurple,
                    border = BorderStroke(1.dp, ArtPrimaryPurple.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = ArtPrimaryPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Model used: $activeModel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArtPrimaryPurple
                        )
                    }
                }

                // Quick alternative model selector if rate limit or general error
                if (!isInvalidKey) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ArtSoftLavender, RoundedCornerShape(14.dp))
                            .border(1.dp, ArtBorderDark.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "TRY AN ALTERNATIVE MODEL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArtPrimaryPurple,
                            letterSpacing = 0.5.sp
                        )

                        val alternativeModels = viewModel.availableGeminiModels.filter { it.id != activeModel }.take(3)
                        alternativeModels.forEach { altModel ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.selectGeminiModel(altModel.id)
                                    onRetry()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ArtPrimaryPurple.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = ArtTextDark
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = ArtPrimaryPurple,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Switch to ${altModel.displayName}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = altModel.badge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ArtPrimaryPurple
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isInvalidKey || isRateLimit) {
                        Button(
                            onClick = { showGeminiSetupDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ArtPrimaryPurple,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reconnect Key", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, ArtBorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ArtTextDark)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Try Again", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ArtPrimaryPurple,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Try Again", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = { showGeminiSetupDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, ArtBorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ArtTextDark)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        if (showGeminiSetupDialog) {
            GeminiSetupDialog(
                viewModel = viewModel,
                initialStep = if (isInvalidKey) 4 else 0,
                onDismiss = { showGeminiSetupDialog = false },
                onConnectedSuccess = {
                    showGeminiSetupDialog = false
                    onRetry()
                }
            )
        }
    }
}

// 3. SUCCESS VIEWPORT
@Composable
fun SuccessItineraryViewport(
    plan: TripPlan,
    activeTab: String,
    onTabSelected: (String) -> Unit,
    viewModel: TripViewModel
) {
    val tabs = listOf("Overview", "Day Plan", "Hotels & Food", "Routes", "Tips & Lists")
    val tabIcons = listOf(Icons.Default.Info, Icons.Default.CalendarToday, Icons.Default.Hotel, Icons.Default.Route, Icons.Default.PlaylistAddCheck)

    var selectedHotel by remember { mutableStateOf<HotelSuggestion?>(null) }
    var selectedPlace by remember { mutableStateOf<Pair<String, String>?>(null) }

    var dragOffset by remember { mutableStateOf(0f) }
    val swipeModifier = Modifier.draggable(
        state = rememberDraggableState { delta -> dragOffset += delta },
        orientation = Orientation.Horizontal,
        onDragStarted = { _ -> dragOffset = 0f },
        onDragStopped = { _ ->
            if (dragOffset > 150f) {
                // Swipe right -> Go to previous tab
                val currentIndex = tabs.indexOf(activeTab)
                if (currentIndex > 0) {
                    onTabSelected(tabs[currentIndex - 1])
                }
            } else if (dragOffset < -150f) {
                // Swipe left -> Go to next tab
                val currentIndex = tabs.indexOf(activeTab)
                if (currentIndex < tabs.lastIndex) {
                    onTabSelected(tabs[currentIndex + 1])
                }
            }
        }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
        ) {
            // Destination Meta Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 6.dp, end = 6.dp)
        ) {
            // Shadow layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 4.dp, y = 4.dp)
                    .background(ArtBorderDark, RoundedCornerShape(20.dp))
            )

            // Foreground Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                border = BorderStroke(2.dp, ArtBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = plan.destination.uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        color = ArtTextDark,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextTag(text = "${plan.durationDays} Days", color = ArtMintGreen)
                        TextTag(text = plan.travelerGroup, color = ArtPeachGold)
                        TextTag(text = plan.travelStyle, color = ArtSoftLavender)
                    }
                }
            }
        }

        // Image Disclaimer Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
                .background(ArtSoftLavender, RoundedCornerShape(12.dp))
                .border(1.5.dp, ArtBorderDark, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = ArtPrimaryPurple,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Note: Images shown are illustrative representation purposes only. Actual service or location may vary.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ArtTextDark,
                lineHeight = 15.sp
            )
        }

        // Horizontal Category Tabs
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(activeTab),
            containerColor = Color.Transparent,
            contentColor = ArtPrimaryPurple,
            edgePadding = 24.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(activeTab)]),
                    color = ArtPrimaryPurple
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == title,
                    onClick = { onTabSelected(title) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tabIcons[index],
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                              )
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = ArtPrimaryPurple,
                    unselectedContentColor = ArtGrayMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Display Area depending on tab selected
        Box(modifier = Modifier.weight(1f).fillMaxWidth().then(swipeModifier)) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    (slideInVertically(
                        initialOffsetY = { 80 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(animationSpec = tween(220))).togetherWith(
                        fadeOut(animationSpec = tween(150))
                    )
                },
                label = "tabTransition"
            ) { targetTab ->
                when (targetTab) {
                    "Overview" -> TabOverview(plan)
                    "Day Plan" -> TabDayPlan(plan) { title, desc ->
                        selectedPlace = title to desc
                    }
                    "Hotels & Food" -> TabHotelsAndFood(
                        plan = plan,
                        onPlaceClick = { title, desc -> selectedPlace = title to desc },
                        onHotelClick = { hotel -> selectedHotel = hotel }
                    )
                    "Routes" -> TabRoutes(plan, viewModel)
                    "Tips & Lists" -> TabTipsAndLists(plan)
                }
            }
        }
    }

    // 1. HOTEL DETAILED OVERLAY DIALOG
    if (selectedHotel != null) {
        val hotel = selectedHotel!!
        AlertDialog(
            onDismissRequest = { selectedHotel = null },
            confirmButton = {
                Button(
                    onClick = { selectedHotel = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ArtPrimaryPurple, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, ArtBorderDark)
                ) {
                    Text("Sounds Great!", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = hotel.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ArtTextDark
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Large visual banner
                    AsyncImage(
                        model = getHotelImageUrl(hotel.name),
                        contentDescription = hotel.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, ArtBorderDark, RoundedCornerShape(16.dp))
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            hotel.suitableFor.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                                TextTag(text = tag, color = ArtPeachGold)
                            }
                        }
                        Text(
                            text = hotel.priceRange,
                            color = ArtPrimaryPurple,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }

                    Text(
                        text = hotel.description,
                        color = ArtTextDark,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    if (hotel.directLink.isNotBlank()) {
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { uriHandler.openUri(hotel.directLink) },
                            colors = ButtonDefaults.buttonColors(containerColor = ArtTertiaryPink, contentColor = ArtTextDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, ArtBorderDark, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp), tint = ArtTextDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Search & Book Hotel", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            containerColor = ArtCardBackground,
            modifier = Modifier.border(2.dp, ArtBorderDark, RoundedCornerShape(28.dp))
        )
    }

    // 2. PLACE DETAILED OVERLAY DIALOG
    if (selectedPlace != null) {
        val (title, desc) = selectedPlace!!
        AlertDialog(
            onDismissRequest = { selectedPlace = null },
            confirmButton = {
                Button(
                    onClick = { selectedPlace = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ArtPrimaryPurple, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, ArtBorderDark)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ArtTextDark
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Large visual banner
                    AsyncImage(
                        model = getPlaceImageUrl(title),
                        contentDescription = title,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, ArtBorderDark, RoundedCornerShape(16.dp))
                    )

                    Text(
                        text = desc,
                        color = ArtTextDark,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            val query = "$title, ${plan.destination}"
                            val encodedName = java.net.URLEncoder.encode(query, "UTF-8")
                            uriHandler.openUri("https://www.google.com/maps/search/?api=1&query=$encodedName")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ArtTertiaryPink, contentColor = ArtTextDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, ArtBorderDark, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp), tint = ArtTextDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open in Google Maps", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            containerColor = ArtCardBackground,
            modifier = Modifier.border(2.dp, ArtBorderDark, RoundedCornerShape(28.dp))
        )
    }
}
}

// --- SUB-VIEWS FOR EACH TAB ---

// A. TAB 1: OVERVIEW & ESTIMATED BUDGET
@Composable
fun TabOverview(plan: TripPlan) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Budget Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                border = BorderStroke(2.dp, ArtBorderDark)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Estimated Total Budget Highlight Badge (Vertical, non-floating, extremely solid layout!)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ArtPrimaryPurple.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .border(1.5.dp, ArtBorderDark, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = ArtPrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ESTIMATED TOTAL BUDGET",
                                color = ArtTextDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = plan.estimatedTotalBudget.ifBlank { "TBD" },
                            color = ArtPrimaryPurple,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    plan.budgetBreakdown.forEach { item ->
                        Row(
                            modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(
                                    text = item.category,
                                    color = ArtTextDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = item.explanation,
                                    color = ArtGrayMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            Text(
                                text = item.costRange,
                                color = ArtPrimaryPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                        HorizontalDivider(color = ArtBorderDark.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }

        // Emergency Information Card
        if (plan.emergencyInfo.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ArtTertiaryPink),
                    border = BorderStroke(2.dp, ArtBorderDark)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = null,
                            tint = ArtTextDark
                        )
                        Column {
                            Text(
                                "Emergency & Safety Info",
                                color = ArtTextDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                plan.emergencyInfo,
                                color = ArtTextDark,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
}

// B. TAB 2: DAY-BY-DAY TIMELINE PLANNER
@Composable
fun TabDayPlan(plan: TripPlan, onPlaceClick: (String, String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        itemsIndexed(plan.days) { index, day ->
            DayExpansionCard(day = day, isFirst = index == 0, onPlaceClick = onPlaceClick)
        }
    }
    }
}

@Composable
fun DayExpansionCard(day: DayPlan, isFirst: Boolean, onPlaceClick: (String, String) -> Unit) {
    var expanded by remember { mutableStateOf(isFirst) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
        border = BorderStroke(2.dp, ArtBorderDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Day title header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ArtMintGreen, RoundedCornerShape(10.dp))
                            .border(1.5.dp, ArtBorderDark, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "D${day.dayNumber}",
                            fontWeight = FontWeight.Bold,
                            color = ArtTextDark,
                            fontSize = 15.sp
                        )
                    }

                    Column {
                        Text(
                            "Day ${day.dayNumber}",
                            fontWeight = FontWeight.Bold,
                            color = ArtTextDark,
                            fontSize = 16.sp
                        )
                        Text(
                            day.theme,
                            color = ArtGrayMuted,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = ArtTextDark
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    day.activities.forEachIndexed { actIdx, act ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Timeline visual path
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(32.dp)
                                    .fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(ArtPrimaryPurple, CircleShape)
                                        .border(2.dp, ArtBorderDark, CircleShape)
                                )

                                if (actIdx < day.activities.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .weight(1f)
                                            .background(ArtBorderDark)
                                    )
                                }
                            }

                            // Activity content card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp, bottom = 16.dp)
                                    .clickable { onPlaceClick(act.title, act.description) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                                border = BorderStroke(1.5.dp, ArtBorderDark.copy(alpha = 0.5f))
                            ) {
                                Column {
                                    // Activity Banner Photo
                                    AsyncImage(
                                        model = getPlaceImageUrl(act.title),
                                        contentDescription = act.title,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    )

                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = act.timeOfDay.uppercase(),
                                                color = ArtPrimaryPurple,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                letterSpacing = 1.sp
                                            )
                                            if (act.approximateCost.isNotBlank()) {
                                                Text(
                                                    text = act.approximateCost,
                                                    color = ArtTextDark,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Text(
                                            text = act.title,
                                            color = ArtTextDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )

                                        Text(
                                            text = act.description,
                                            color = ArtGrayMuted,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )

                                        if (act.tips.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(ArtPeachGold.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .border(1.dp, ArtBorderDark.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    tint = ArtTextDark,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = act.tips,
                                                    color = ArtTextDark,
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// C. TAB 3: ACCOMMODATION GUIDE & FOOD MUST-TRIES
@Composable
fun TabHotelsAndFood(
    plan: TripPlan,
    onPlaceClick: (String, String) -> Unit,
    onHotelClick: (HotelSuggestion) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Neighborhood recommendation Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                border = BorderStroke(2.dp, ArtBorderDark)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "NEIGHBORHOOD TO STAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArtPrimaryPurple,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = plan.accommodationGuide.bestAreaToStay,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArtTextDark,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = plan.accommodationGuide.whyRecommended,
                        fontSize = 13.sp,
                        color = ArtGrayMuted,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Suggested Hotels
        item {
            Text(
                "SUGGESTED HOTELS",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = ArtGrayMuted,
                letterSpacing = 1.sp
            )
        }

        itemsIndexed(plan.accommodationGuide.suggestions) { _, hotel ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHotelClick(hotel) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                border = BorderStroke(1.5.dp, ArtBorderDark)
            ) {
                Column {
                    // Beautiful banner photo of the hotel
                    AsyncImage(
                        model = getHotelImageUrl(hotel.name),
                        contentDescription = hotel.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    )

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = hotel.name,
                                color = ArtTextDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = hotel.priceRange,
                                color = ArtPrimaryPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.End
                            )
                        }

                        FlowRow(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            hotel.suitableFor.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                                TextTag(text = tag, color = ArtPeachGold)
                            }
                        }

                        Text(
                            text = hotel.description,
                            color = ArtGrayMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                "View Details",
                                color = ArtPrimaryPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = ArtPrimaryPurple,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Food & Gastronomy Must-tries
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "LOCAL DISHES TO TRY",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = ArtGrayMuted,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                border = BorderStroke(2.dp, ArtBorderDark)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = ArtPrimaryPurple)
                        Text("Signature Local Eats", color = ArtTextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        plan.foodGuide.mustTryDishes.forEach { dish ->
                            Box(
                                modifier = Modifier
                                    .background(ArtTertiaryPink, RoundedCornerShape(8.dp))
                                    .border(1.5.dp, ArtBorderDark, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(dish, color = ArtTextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Restaurant Recommendations
        item {
            Text(
                "RECOMMENDED CAFES & DINERS",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = ArtGrayMuted,
                letterSpacing = 1.sp
            )
        }

        // Note advising users to check Google Maps for actual visuals
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ArtSecondaryPurple),
                border = BorderStroke(1.5.dp, ArtBorderDark)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = ArtPrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Note: Images shown below are reference style visuals. Please check Google Maps for real-time customer photos, menus, and accurate directions.",
                        color = ArtTextDark,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        itemsIndexed(plan.foodGuide.recommendedRestaurants) { _, rest ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaceClick(rest.name, rest.description) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                border = BorderStroke(1.5.dp, ArtBorderDark)
            ) {
                Column {
                    // Beautiful banner photo of the restaurant with reference badge
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = getPlaceImageUrl(rest.name),
                            contentDescription = rest.name,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(135.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Reference Image",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = rest.name,
                                color = ArtTextDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextTag(text = rest.type, color = ArtSoftLavender)
                        }

                        Text(
                            text = "Signature: ${rest.signatureDish}",
                            color = ArtPrimaryPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            text = rest.description,
                            color = ArtGrayMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
}

// D. TAB 4: ROUTES & LOGISTICS
@Composable
fun TabRoutes(plan: TripPlan, viewModel: TripViewModel) {
    // Collect the user's start city (origin location)
    val fromLocationState by viewModel.fromLocation.collectAsState()
    val startCityName = fromLocationState.ifBlank { "Start Hub" }

    // Map to SelectableRoute models
    val allRoutes = remember(plan) {
        val list = mutableListOf<SelectableRoute>()
        // Add Recommended Route at ID -1
        list.add(
            SelectableRoute(
                id = -1,
                name = "RECOMMENDED DIRECT ROUTE",
                transportMode = "Car", // Default
                duration = "Direct Connection",
                estimatedFare = "Recommended Option",
                details = plan.routes.recommendedRoute
            )
        )
        // Add alternative routes
        plan.routes.options.forEachIndexed { index, option ->
            list.add(
                SelectableRoute(
                    id = index,
                    name = option.name,
                    transportMode = option.transportMode,
                    duration = option.duration,
                    estimatedFare = option.estimatedFare,
                    details = option.routeDetails
                )
            )
        }
        list
    }

    var selectedRouteId by remember { mutableStateOf(-1) }
    val selectedRoute = remember(selectedRouteId, allRoutes) {
        allRoutes.find { it.id == selectedRouteId } ?: allRoutes.first()
    }

    val parsedStops = remember(selectedRoute.details) {
        extractRouteSteps(selectedRoute.details)
    }

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val encodedDestination = remember(plan.destination) {
        try {
            java.net.URLEncoder.encode(plan.destination, "UTF-8")
        } catch (e: Exception) {
            plan.destination
        }
    }
    val travelModeParam = remember(selectedRoute.transportMode) {
        when (selectedRoute.transportMode.lowercase()) {
            "car", "cab", "taxi", "driving" -> "driving"
            "train", "bus", "subway", "transit" -> "transit"
            "walk", "walking", "foot" -> "walking"
            "bicycle", "cycling" -> "bicycling"
            else -> "driving"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Core Visual Navigation Mini Map
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        uriHandler.openUri("https://www.google.com/maps/dir/?api=1&destination=$encodedDestination&travelmode=$travelModeParam")
                    }
            ) {
                UberMiniMapView(
                    modifier = Modifier.fillMaxWidth(),
                    startPoint = startCityName,
                    endPoint = plan.destination,
                    stops = parsedStops,
                    transitMode = selectedRoute.transportMode
                )
            }
        }

        // Section Title
        item {
            Text(
                text = "CHOOSE A ROUTE PLAN",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = ArtGrayMuted,
                letterSpacing = 1.sp
            )
        }

        // List of multiple route options
        itemsIndexed(allRoutes) { index, route ->
            val isSelected = selectedRouteId == route.id
            val mode = route.transportMode.lowercase()
            val cardBgColor = when {
                mode.contains("flight") || mode.contains("air") || mode.contains("plane") -> ArtTertiaryPink
                mode.contains("train") || mode.contains("rail") || mode.contains("subway") || mode.contains("metro") || mode.contains("transit") -> ArtMintGreen
                mode.contains("cab") || mode.contains("taxi") || mode.contains("car") || mode.contains("drive") -> ArtPeachGold
                mode.contains("bike") || mode.contains("bicycle") || mode.contains("motorcycle") || mode.contains("cycle") -> ArtSecondaryPurple
                mode.contains("walk") || mode.contains("foot") || mode.contains("hike") -> ArtSoftLavender
                else -> ArtCardBackground
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedRouteId = route.id },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(
                    width = if (isSelected) 3.5.dp else 2.dp,
                    color = if (isSelected) ArtPrimaryPurple else ArtBorderDark
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = route.name.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ArtPrimaryPurple else ArtGrayMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (route.id == -1) "Recommended Route" else "Alternative ${route.name}",
                                color = ArtTextDark,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }

                        // Transit mode badge
                        Box(
                            modifier = Modifier
                                .background(ArtCardBackground, RoundedCornerShape(8.dp))
                                .border(1.5.dp, ArtBorderDark, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = route.transportMode.uppercase(),
                                color = ArtTextDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DURATION", color = ArtGrayMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(route.duration, color = ArtTextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("EST. COST / FARE", color = ArtGrayMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(route.estimatedFare, color = ArtPrimaryPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stylized timeline or detailed description
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ArtCardBackground.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .border(1.5.dp, ArtBorderDark.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                route.transportMode.lowercase().contains("cab") || route.transportMode.lowercase().contains("taxi") || route.transportMode.lowercase().contains("car") -> Icons.Default.LocalTaxi
                                route.transportMode.lowercase().contains("train") || route.transportMode.lowercase().contains("rail") || route.transportMode.lowercase().contains("subway") -> Icons.Default.Train
                                route.transportMode.lowercase().contains("flight") || route.transportMode.lowercase().contains("plane") || route.transportMode.lowercase().contains("air") -> Icons.Default.Flight
                                route.transportMode.lowercase().contains("walk") || route.transportMode.lowercase().contains("foot") -> Icons.Default.DirectionsWalk
                                else -> Icons.Default.DirectionsBus
                            },
                            contentDescription = null,
                            tint = ArtPrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "ROUTE LOGISTICS & DESCRIPTION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ArtGrayMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = route.details,
                                color = ArtTextDark,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Open Active Google Maps CTA
        item {
            Button(
                onClick = {
                    uriHandler.openUri("https://www.google.com/maps/dir/?api=1&destination=$encodedDestination&travelmode=$travelModeParam")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ArtPrimaryPurple, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, ArtBorderDark)
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Selected Route in Google Maps", fontWeight = FontWeight.Bold)
            }
        }
    }
    }
}

// SelectableRoute class to model items
data class SelectableRoute(
    val id: Int,
    val name: String,
    val transportMode: String,
    val duration: String,
    val estimatedFare: String,
    val details: String
)

// Helper parser to extract route stops
private fun extractRouteSteps(details: String): List<String> {
    if (details.isBlank()) return emptyList()
    
    // Clean some common noise words to make names shorter and cleaner
    val cleanText = details
        .replace(Regex("(?i)\\b(take|arrive at|continue on|drive to|then|via|head toward|get on|switch to|route|highway|road)\\b"), "")
        .trim()
        
    // Split on typical delimiters like arrows, commas, then, etc.
    val parts = cleanText.split(Regex("(?i)->|→|,|;|then|and"))
    val result = parts.map { it.trim() }
        .filter { it.isNotEmpty() && it.length > 2 && !it.contains(Regex("(?i)hour|minute|min|hr|km|mile")) }
        .map { step ->
            // Capitalize each word and truncate if too long
            val wordList = step.split(" ").filter { it.isNotBlank() }
            val formatted = wordList.take(2).joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            if (formatted.length > 14) formatted.substring(0, 11) + "..." else formatted
        }
        .filter { it.isNotBlank() }
        
    return result.distinct().take(2) // Up to 2 clean intermediate stops
}

// E. TAB 5: PACKING CHECKLISTS & HANDY GUIDE TIPS (INTERACTIVE!)
@Composable
fun TabTipsAndLists(plan: TripPlan) {
    // Interactive packing state
    var checkedPackingIndices by remember { mutableStateOf(setOf<Int>()) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Packing Card (Interactive)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                border = BorderStroke(2.dp, ArtBorderDark)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Icon(Icons.Default.Luggage, contentDescription = null, tint = ArtPrimaryPurple)
                        Text("Interactive Packing Checklist", color = ArtTextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    if (plan.packingList.isEmpty()) {
                        Text("No specific packing tips suggested.", color = ArtGrayMuted, fontSize = 13.sp)
                    } else {
                        plan.packingList.forEachIndexed { index, item ->
                            val isChecked = checkedPackingIndices.contains(index)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        checkedPackingIndices = if (isChecked) {
                                            checkedPackingIndices - index
                                        } else {
                                            checkedPackingIndices + index
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isChecked) ArtPrimaryPurple else ArtTextDark,
                                    modifier = Modifier.size(20.dp)
                                )

                                Text(
                                    text = item,
                                    color = if (isChecked) ArtGrayMuted else ArtTextDark,
                                    fontSize = 13.sp,
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }
                        }
                    }
                }
            }
        }

        // Local Experiences & Tips Card
        if (plan.localTips.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                    border = BorderStroke(2.dp, ArtBorderDark)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ArtPeachGold)
                            Text("Expert Local Secrets", color = ArtTextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        plan.localTips.forEach { tip ->
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Label,
                                    contentDescription = null,
                                    tint = ArtPeachGold,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(top = 2.dp)
                                )
                                Text(tip, color = ArtGrayMuted, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        // Things to Avoid Card
        if (plan.thingsToAvoid.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ArtTertiaryPink),
                    border = BorderStroke(2.dp, ArtBorderDark)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Default.DoNotDisturb, contentDescription = null, tint = ArtTextDark)
                            Text("Things to Avoid / Safety", color = ArtTextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        plan.thingsToAvoid.forEach { warning ->
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(6.dp)
                                        .background(ArtTextDark, CircleShape)
                                )
                                Text(warning, color = ArtTextDark, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        // Best Photographic Locations Card
        if (plan.bestPhotoSpots.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                    border = BorderStroke(2.dp, ArtBorderDark)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ArtMintGreen)
                            Text("Best Instagram / Photo Spots", color = ArtTextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        plan.bestPhotoSpots.forEach { spot ->
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ArtMintGreen.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                Text(spot, color = ArtGrayMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

// --- REUSABLE SMALL COMPONENT ---
@Composable
fun TextTag(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(8.dp))
            .border(1.5.dp, ArtBorderDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = ArtTextDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- VISUAL UBER MINI MAP VIEW COMPOSABLE ---
@Composable
fun UberMiniMapView(
    modifier: Modifier = Modifier,
    startPoint: String,
    endPoint: String,
    stops: List<String> = emptyList(),
    transitMode: String = "Car"
) {
    val borderDark = ArtBorderDark
    val primaryPurple = ArtPrimaryPurple
    val mintGreen = ArtMintGreen
    val peachGold = ArtPeachGold
    val tertiaryPink = ArtTertiaryPink

    val isSystemDark = LocalTripColors.current.isDark
    
    // Active color scheme based on transit mode
    val activeColor = remember(transitMode) {
        val mode = transitMode.lowercase()
        when {
            mode.contains("flight") || mode.contains("air") || mode.contains("plane") -> tertiaryPink
            mode.contains("train") || mode.contains("rail") || mode.contains("subway") || mode.contains("metro") -> mintGreen
            mode.contains("walk") || mode.contains("foot") || mode.contains("bicycle") -> peachGold
            else -> primaryPurple
        }
    }

    val activeTextColor = remember(transitMode, isSystemDark) {
        val mode = transitMode.lowercase()
        if (isSystemDark) {
            when {
                mode.contains("flight") || mode.contains("air") || mode.contains("plane") -> Color(0xFFF472B6)
                mode.contains("train") || mode.contains("rail") || mode.contains("subway") || mode.contains("metro") -> Color(0xFF34D399)
                mode.contains("walk") || mode.contains("foot") || mode.contains("bicycle") || mode.contains("bike") -> Color(0xFFFBBF24)
                else -> Color(0xFFA78BFA)
            }
        } else {
            when {
                mode.contains("flight") || mode.contains("air") || mode.contains("plane") -> Color(0xFFC2185B)
                mode.contains("train") || mode.contains("rail") || mode.contains("subway") || mode.contains("metro") -> Color(0xFF0F766E)
                mode.contains("walk") || mode.contains("foot") || mode.contains("bicycle") || mode.contains("bike") -> Color(0xFFB45309)
                else -> Color(0xFF6750A4)
            }
        }
    }

    val destinationPinColor = remember(isSystemDark) {
        if (isSystemDark) Color(0xFFF472B6) else Color(0xFFC2185B)
    }

    val intermediatePinColor = remember(isSystemDark) {
        if (isSystemDark) Color(0xFFFBBF24) else Color(0xFFB45309)
    }
    
    val activeIcon = remember(transitMode) {
        val mode = transitMode.lowercase()
        when {
            mode.contains("flight") || mode.contains("air") || mode.contains("plane") -> Icons.Default.Flight
            mode.contains("train") || mode.contains("rail") || mode.contains("subway") || mode.contains("metro") -> Icons.Default.Train
            mode.contains("walk") || mode.contains("foot") -> Icons.Default.DirectionsWalk
            else -> Icons.Default.DirectionsCar
        }
    }

    // Animated pulsing tracking dot progress
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    // Compile the sequence of display nodes:
    // Keep it ultra-clean and simple: maximum 1 key intermediate stop to guarantee it looks beautiful on compact screens.
    val nodes = remember(startPoint, endPoint, stops) {
        val list = mutableListOf<String>()
        list.add(startPoint)
        if (stops.isNotEmpty()) {
            list.add(stops.first()) // Keep only the primary key stop to avoid any text clutter or overlaps
        }
        list.add(endPoint)
        list
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(ArtCardBackground, RoundedCornerShape(20.dp))
            .border(2.dp, borderDark, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Subtle horizontal dashed guideline background for a clean, tech-inspired minimalist grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridY = size.height * 0.55f
            val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            drawLine(
                color = borderDark.copy(alpha = 0.15f),
                start = Offset(0f, gridY),
                end = Offset(size.width, gridY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = pathEffect
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header of the mini map: Mode indicator & google maps prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(activeTextColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.5.dp, borderDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = activeIcon,
                            contentDescription = null,
                            tint = activeTextColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = "LIVE ROUTE PREVIEW",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = activeTextColor,
                        letterSpacing = 1.sp
                    )
                }

                // Google Maps quick launcher pill
                Box(
                    modifier = Modifier
                        .background(activeTextColor, RoundedCornerShape(8.dp))
                        .border(1.5.dp, borderDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open in Google Maps",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "OPEN MAPS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // The visual timeline representing nodes and paths
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    nodes.forEachIndexed { index, nodeName ->
                        // Node element
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(24.dp)
                            ) {
                                if (index == 0) {
                                    // Start Location
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(activeTextColor.copy(alpha = 0.25f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(activeTextColor, CircleShape)
                                            .border(1.5.dp, borderDark, CircleShape)
                                    )
                                } else if (index == nodes.lastIndex) {
                                    // Destination
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Destination",
                                        tint = destinationPinColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    // Intermediate stop
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(intermediatePinColor, CircleShape)
                                            .border(1.5.dp, borderDark, CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Node name/label below
                            Text(
                                text = nodeName,
                                fontSize = 9.sp,
                                fontWeight = if (index == 0 || index == nodes.lastIndex) FontWeight.Black else FontWeight.Bold,
                                color = if (index == 0 || index == nodes.lastIndex) ArtTextDark else ArtGrayMuted,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }

                        // Flow Arrow indicator between nodes
                        if (index < nodes.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .weight(0.5f),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    // Custom animated chevron dots
                                    val offsetFactor = (pulseProgress * 3).toInt()
                                    repeat(3) { dotIndex ->
                                        val isGlowing = dotIndex == offsetFactor
                                        Text(
                                            text = "▸",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGlowing) activeTextColor else borderDark.copy(alpha = 0.25f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- VISUAL UBER MINI MAP VIEW COMPOSABLE DEPRECATED ---
@Composable
fun ObsoleteUberMiniMapView(
    modifier: Modifier = Modifier,
    startPoint: String,
    endPoint: String,
    stops: List<String> = emptyList(),
    transitMode: String = "Car"
) {
    val borderDark = ArtBorderDark
    val primaryPurple = ArtPrimaryPurple
    val mintGreen = ArtMintGreen
    val peachGold = ArtPeachGold
    val tertiaryPink = ArtTertiaryPink

    // Map-alive pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 6.dp.value,
        targetValue = 24.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )

    val isFlight = transitMode.lowercase().contains("flight") || transitMode.lowercase().contains("plane") || transitMode.lowercase().contains("air")
    val isTrain = transitMode.lowercase().contains("train") || transitMode.lowercase().contains("rail") || transitMode.lowercase().contains("subway")
    val isSystemDark = LocalTripColors.current.isDark

    val groundColor = if (isSystemDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val streetColor = if (isSystemDark) Color(0xFF334155) else Color(0xFFFFFFFF)
    val streetOutlineColor = if (isSystemDark) Color(0xFF111827) else Color(0xFFE2E8F0)
    val parkColor = if (isSystemDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFDCEFDB)
    val riverColor = if (isSystemDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFE0F2FE)
    val buildingColor = if (isSystemDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFE2E8F0)

    val activeColor = if (isFlight) tertiaryPink else if (isTrain) mintGreen else primaryPurple
    val travelIcon = if (isFlight) "✈️" else if (isTrain) "🚇" else "🚗"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(groundColor, RoundedCornerShape(16.dp))
            .border(2.dp, borderDark, RoundedCornerShape(16.dp))
    ) {
        val width = with(androidx.compose.ui.platform.LocalDensity.current) { maxWidth.toPx() }
        val height = with(androidx.compose.ui.platform.LocalDensity.current) { maxHeight.toPx() }

        val p0 = Offset(width * 0.15f, height * 0.75f)
        val p4 = Offset(width * 0.85f, height * 0.25f)

        // Compute active animated offset along active path
        val currentOffset = if (isFlight) {
            val t = progress
            val animatedX = (1 - t) * (1 - t) * p0.x + 2 * (1 - t) * t * (width * 0.5f) + t * t * p4.x
            val animatedY = (1 - t) * (1 - t) * p0.y + 2 * (1 - t) * t * (height * -0.1f) + t * t * p4.y
            Offset(animatedX, animatedY)
        } else if (isTrain) {
            val routePoints = listOf(p0, Offset(width * 0.5f, height * 0.5f), p4)
            val segmentCount = 2
            val segmentProgress = progress * segmentCount
            val segmentIndex = segmentProgress.toInt().coerceIn(0, segmentCount - 1)
            val t = segmentProgress - segmentIndex
            val startSeg = routePoints[segmentIndex]
            val endSeg = routePoints[segmentIndex + 1]
            Offset(
                x = startSeg.x + (endSeg.x - startSeg.x) * t,
                y = startSeg.y + (endSeg.y - startSeg.y) * t
            )
        } else {
            val p1 = Offset(width * 0.15f, height * 0.45f)
            val p2 = Offset(width * 0.5f, height * 0.45f)
            val p3 = Offset(width * 0.5f, height * 0.25f)
            val routePoints = listOf(p0, p1, p2, p3, p4)
            val segmentCount = 4
            val segmentProgress = progress * segmentCount
            val segmentIndex = segmentProgress.toInt().coerceIn(0, segmentCount - 1)
            val t = segmentProgress - segmentIndex
            val startSeg = routePoints[segmentIndex]
            val endSeg = routePoints[segmentIndex + 1]
            Offset(
                x = startSeg.x + (endSeg.x - startSeg.x) * t,
                y = startSeg.y + (endSeg.y - startSeg.y) * t
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val densityVal = this

            // 1. Draw River Path
            val riverPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, height * 0.15f)
                cubicTo(width * 0.35f, height * 0.1f, width * 0.65f, height * 0.4f, width, height * 0.3f)
                lineTo(width, height * 0.45f)
                cubicTo(width * 0.65f, height * 0.55f, width * 0.35f, height * 0.25f, 0f, height * 0.3f)
                close()
            }
            drawPath(riverPath, riverColor)

            // 2. Draw Parks
            drawRoundRect(
                color = parkColor,
                topLeft = Offset(width * 0.65f, height * 0.05f),
                size = androidx.compose.ui.geometry.Size(width * 0.3f, height * 0.35f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawRoundRect(
                color = parkColor,
                topLeft = Offset(width * 0.05f, height * 0.55f),
                size = androidx.compose.ui.geometry.Size(width * 0.25f, height * 0.35f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )

            // 3. Draw Building Blocks
            val blocks = listOf(
                Offset(width * 0.05f, height * 0.05f) to androidx.compose.ui.geometry.Size(width * 0.15f, height * 0.18f),
                Offset(width * 0.35f, height * 0.05f) to androidx.compose.ui.geometry.Size(width * 0.18f, height * 0.15f),
                Offset(width * 0.35f, height * 0.55f) to androidx.compose.ui.geometry.Size(width * 0.18f, height * 0.18f),
                Offset(width * 0.7f, height * 0.55f) to androidx.compose.ui.geometry.Size(width * 0.2f, height * 0.18f)
            )
            blocks.forEach { (pos, bSize) ->
                drawRoundRect(
                    color = buildingColor,
                    topLeft = pos,
                    size = bSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }

            // 4. Draw Street Grids
            val verticalStreets = listOf(0.3f, 0.65f, 0.85f)
            val horizontalStreets = listOf(0.25f, 0.75f)

            // Street Outlines
            verticalStreets.forEach { xPct ->
                drawLine(
                    color = streetOutlineColor,
                    start = Offset(width * xPct, 0f),
                    end = Offset(width * xPct, height),
                    strokeWidth = 12.dp.toPx()
                )
            }
            horizontalStreets.forEach { yPct ->
                drawLine(
                    color = streetOutlineColor,
                    start = Offset(0f, height * yPct),
                    end = Offset(width, height * yPct),
                    strokeWidth = 12.dp.toPx()
                )
            }

            // Street Fills
            verticalStreets.forEach { xPct ->
                drawLine(
                    color = streetColor,
                    start = Offset(width * xPct, 0f),
                    end = Offset(width * xPct, height),
                    strokeWidth = 8.dp.toPx()
                )
            }
            horizontalStreets.forEach { yPct ->
                drawLine(
                    color = streetColor,
                    start = Offset(0f, height * yPct),
                    end = Offset(width, height * yPct),
                    strokeWidth = 8.dp.toPx()
                )
            }

            // 5. Draw Active Route Line
            if (isFlight) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(p0.x, p0.y)
                    quadraticTo(width * 0.5f, height * -0.1f, p4.x, p4.y)
                }
                drawPath(
                    path = path,
                    color = borderDark.copy(alpha = 0.2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                )
                drawPath(
                    path = path,
                    color = tertiaryPink,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                )
            } else if (isTrain) {
                val routePoints = listOf(p0, Offset(width * 0.5f, height * 0.5f), p4)
                for (i in 0 until routePoints.size - 1) {
                    val s = routePoints[i]
                    val e = routePoints[i+1]
                    drawLine(
                        color = borderDark,
                        start = s,
                        end = e,
                        strokeWidth = 5.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                    )
                    drawLine(
                        color = mintGreen,
                        start = s,
                        end = e,
                        strokeWidth = 2.dp.toPx()
                    )
                }
            } else {
                val p1 = Offset(width * 0.15f, height * 0.45f)
                val p2 = Offset(width * 0.5f, height * 0.45f)
                val p3 = Offset(width * 0.5f, height * 0.25f)
                val routePoints = listOf(p0, p1, p2, p3, p4)

                for (i in 0 until routePoints.size - 1) {
                    drawLine(
                        color = borderDark.copy(alpha = 0.25f),
                        start = routePoints[i] + Offset(2.dp.toPx(), 2.dp.toPx()),
                        end = routePoints[i+1] + Offset(2.dp.toPx(), 2.dp.toPx()),
                        strokeWidth = 7.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                for (i in 0 until routePoints.size - 1) {
                    drawLine(
                        color = primaryPurple,
                        start = routePoints[i],
                        end = routePoints[i+1],
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 6. Draw start and destination pins
            // Start pin (green circle)
            drawCircle(color = borderDark, radius = 9.dp.toPx(), center = p0)
            drawCircle(color = mintGreen, radius = 6.dp.toPx(), center = p0)

            // Destination pin (pink circle)
            drawCircle(color = borderDark, radius = 9.dp.toPx(), center = p4)
            drawCircle(color = tertiaryPink, radius = 6.dp.toPx(), center = p4)

            // 7. Draw tracking pulse
            drawCircle(
                color = activeColor.copy(alpha = pulseAlpha),
                radius = pulseRadius * densityVal.density,
                center = currentOffset
            )
            drawCircle(color = borderDark, radius = 8.dp.toPx(), center = currentOffset)
            drawCircle(color = activeColor, radius = 6.dp.toPx(), center = currentOffset)
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = currentOffset)
        }

        // --- MAP OVERLAYS ---

        // 1. Compass Rose (top left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
                .size(28.dp)
                .background(ArtCardBackground.copy(alpha = 0.9f), CircleShape)
                .border(1.dp, ArtBorderDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("N", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ArtTextDark)
        }

        // 2. Scale Bar (bottom center)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .background(ArtCardBackground.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                .border(1.dp, ArtBorderDark.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .background(ArtTextDark)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("500 m", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ArtTextDark)
        }

        // 3. Park Labels
        Text(
            text = "Buena Gardens",
            color = if (isSystemDark) Color(0xFF10B981).copy(alpha = 0.8f) else Color(0xFF15803D),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 40.dp, top = 25.dp)
        )
        Text(
            text = "Union Square",
            color = if (isSystemDark) Color(0xFF10B981).copy(alpha = 0.8f) else Color(0xFF15803D),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 42.dp)
        )

        // 4. Street Labels
        Text(
            text = "Edison St",
            color = if (isSystemDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.35f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 75.dp, bottom = 4.dp)
        )
        Text(
            text = "15th Ave",
            color = if (isSystemDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.35f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 75.dp, end = 25.dp)
        )

        // 5. Floating Arrival bubble pointing to active vehicle
        val density = androidx.compose.ui.platform.LocalDensity.current
        val animatedXInDp = with(density) { currentOffset.x.toDp() }
        val animatedYInDp = with(density) { currentOffset.y.toDp() }

        Box(
            modifier = Modifier
                .offset(x = animatedXInDp - 36.dp, y = animatedYInDp - 54.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.5.dp, ArtBorderDark, RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Arrival", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(travelIcon, fontSize = 9.sp)
                    Text("9 min", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        }

        // 6. Start/End location labels
        // Start text
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 12.dp)
                .background(ArtCardBackground, RoundedCornerShape(6.dp))
                .border(1.dp, ArtBorderDark, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(6.dp).background(ArtMintGreen, CircleShape))
                Text(
                    text = startPoint.take(14) + if (startPoint.length > 14) "..." else "",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArtTextDark
                )
            }
        }

        // Stop text (if present)
        if (stops.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 10.dp)
                    .background(ArtCardBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, ArtBorderDark, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(ArtPeachGold, CircleShape))
                    Text(
                        text = stops.first().take(14) + if (stops.first().length > 14) "..." else "",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArtTextDark
                    )
                }
            }
        }

        // End text
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 12.dp)
                .background(ArtCardBackground, RoundedCornerShape(6.dp))
                .border(1.dp, ArtBorderDark, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(6.dp).background(ArtTertiaryPink, CircleShape))
                Text(
                    text = endPoint.take(14) + if (endPoint.length > 14) "..." else "",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArtTextDark
                )
            }
        }
    }
}

// --- CURATED TRAVEL AND HOTEL IMAGES HELPER FUNCTIONS ---
fun getPlaceImageUrl(name: String): String {
    val query = name.lowercase()
    return when {
        query.contains("sethani ghat") || query.contains("ghat") || query.contains("river") || query.contains("narmada") -> 
            "https://images.unsplash.com/photo-1590050752117-238cb0612b1b?auto=format&fit=crop&w=600&q=80" // Beautiful holy river ghat / sunset
        query.contains("dal bafla") || query.contains("bafla") || query.contains("thali") || query.contains("traditional lunch") -> 
            "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=600&q=80" // Authentic traditional Indian feast
        query.contains("poha") || query.contains("indori poha") || query.contains("breakfast") -> 
            "https://images.unsplash.com/photo-1601050690597-df056fb4ce78?auto=format&fit=crop&w=600&q=80" // Traditional Indian street breakfast / snacks
        query.contains("jalebi") || query.contains("sweet") || query.contains("dessert") -> 
            "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=600&q=80" // Sweet jalebis / Indian sweets
        query.contains("sabudana") || query.contains("khichdi") -> 
            "https://images.unsplash.com/photo-1601050690597-df056fb4ce78?auto=format&fit=crop&w=600&q=80"
        query.contains("fish") || query.contains("curry") || query.contains("seafood") -> 
            "https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?auto=format&fit=crop&w=600&q=80" // Delicious hot fish curry
        query.contains("cafe") || query.contains("restaurant") || query.contains("dining") || query.contains("diner") || query.contains("bistro") || query.contains("food") -> 
            "https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&w=600&q=80" // Aesthetic warm restaurant/cafe view
        query.contains("beach") || query.contains("goa") || query.contains("sea") || query.contains("coast") -> 
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80"
        query.contains("mountain") || query.contains("himalaya") || query.contains("manali") || query.contains("hill") || query.contains("trek") -> 
            "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=600&q=80"
        query.contains("temple") || query.contains("spiritual") || query.contains("church") || query.contains("shrine") -> 
            "https://images.unsplash.com/photo-1545128485-c400e7702796?auto=format&fit=crop&w=600&q=80"
        query.contains("palace") || query.contains("fort") || query.contains("jaipur") || query.contains("castle") || query.contains("monument") -> 
            "https://images.unsplash.com/photo-1477584308800-b44223df0a51?auto=format&fit=crop&w=600&q=80"
        query.contains("tokyo") || query.contains("japan") || query.contains("kyoto") -> 
            "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=600&q=80"
        query.contains("paris") || query.contains("eiffel") || query.contains("france") -> 
            "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=600&q=80"
        query.contains("london") || query.contains("uk") || query.contains("british") -> 
            "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=600&q=80"
        query.contains("museum") || query.contains("art") || query.contains("gallery") || query.contains("exhibition") -> 
            "https://images.unsplash.com/photo-1580136579312-94651dfd596d?auto=format&fit=crop&w=600&q=80"
        query.contains("park") || query.contains("garden") || query.contains("lake") || query.contains("nature") || query.contains("forest") -> 
            "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?auto=format&fit=crop&w=600&q=80"
        query.contains("shopping") || query.contains("market") || query.contains("mall") || query.contains("bazaar") || query.contains("store") -> 
            "https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=600&q=80"
        else -> {
            val images = listOf(
                "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1452421820245-172192da2804?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1530789253388-582c481c54b0?auto=format&fit=crop&w=600&q=80"
            )
            val index = kotlin.math.abs(name.hashCode()) % images.size
            images[index]
        }
    }
}

fun getHotelImageUrl(name: String): String {
    val query = name.lowercase()
    return when {
        query.contains("shree vinayak") || query.contains("vinayak") || query.contains("residency") -> 
            "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=600&q=80" // Premium cozy resort / residency style
        query.contains("resort") || query.contains("spa") || query.contains("beachfront") || query.contains("goa") -> 
            "https://images.unsplash.com/photo-1540541338287-41700207dee6?auto=format&fit=crop&w=600&q=80"
        query.contains("hostel") || query.contains("backpacker") || query.contains("dorm") || query.contains("inn") -> 
            "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=600&q=80"
        query.contains("villa") || query.contains("chalet") || query.contains("cottage") || query.contains("cabin") -> 
            "https://images.unsplash.com/photo-1580587771525-78b9dba3b914?auto=format&fit=crop&w=600&q=80"
        query.contains("luxury") || query.contains("grand") || query.contains("hotel") || query.contains("suites") || query.contains("palace") || query.contains("boutique") -> 
            "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=600&q=80"
        else -> {
            val hotelImages = listOf(
                "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1582719508461-905c673771fd?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1544097652-3d31157d773f?auto=format&fit=crop&w=600&q=80"
            )
            val index = kotlin.math.abs(name.hashCode()) % hotelImages.size
            hotelImages[index]
        }
    }
}
