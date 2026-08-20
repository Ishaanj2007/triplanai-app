package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.TripViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToItinerary: () -> Unit
) {
    val destination by viewModel.destination.collectAsState()
    val fromLocation by viewModel.fromLocation.collectAsState()
    val durationDays by viewModel.durationDays.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val selectedPersonality by viewModel.selectedPersonality.collectAsState()
    val travelerGroup by viewModel.travelerGroup.collectAsState()
    val totalBudget by viewModel.totalBudget.collectAsState()
    val preferredTransportation by viewModel.preferredTransportation.collectAsState()
    val selectedTravelStyles by viewModel.selectedTravelStyles.collectAsState()
    val travelPace by viewModel.travelPace.collectAsState()
    val selectedSpecialRequirements by viewModel.selectedSpecialRequirements.collectAsState()
    val userApiKey by viewModel.userApiKey.collectAsState()

    var showGeminiSetupDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArtBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Expressive Action TopBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(ArtCardBackground, CircleShape)
                        .border(2.dp, ArtBorderDark, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = ArtTextDark
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLANNING TRIP TO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArtPrimaryPurple,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = destination.ifBlank { "Unknown" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = ArtTextDark
                    )
                }

                // Itinerary Personality Selector
                var showPersonalityDialog by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(ArtCardBackground, CircleShape)
                        .border(2.dp, ArtBorderDark, CircleShape)
                        .clip(CircleShape)
                        .clickable { showPersonalityDialog = true }
                        .testTag("itinerary_personality_selector"),
                    contentAlignment = Alignment.Center
                ) {
                    val personalityIcon = when (selectedPersonality) {
                        "Professional" -> Icons.Default.BusinessCenter
                        "Friendly" -> Icons.Default.SentimentSatisfiedAlt
                        "Funny" -> Icons.Default.SentimentVerySatisfied
                        "Roast Me" -> Icons.Default.LocalFireDepartment
                        else -> Icons.Default.AutoAwesome
                    }
                    Icon(
                        imageVector = personalityIcon,
                        contentDescription = "Select AI Personality",
                        tint = ArtPrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (showPersonalityDialog) {
                    AlertDialog(
                        onDismissRequest = { showPersonalityDialog = false },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .border(3.dp, ArtBorderDark, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        containerColor = ArtBackground,
                        confirmButton = {
                            Button(
                                onClick = { showPersonalityDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = ArtPrimaryPurple),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(2.dp, ArtBorderDark),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("close_personality_dialog_button")
                            ) {
                                Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ArtSoftLavender, CircleShape)
                                        .border(1.5.dp, ArtBorderDark, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = ArtPrimaryPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "AI Response Style",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ArtTextDark
                                )
                            }
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "Choose the AI planner's personality and tone when generating your travel itinerary:",
                                    fontSize = 13.sp,
                                    color = ArtTextDark.copy(alpha = 0.8f),
                                    lineHeight = 18.sp
                                )

                                val personalities = listOf(
                                    Triple("Professional", "Formal & informative. Clear, straightforward travel recommendations.", Icons.Default.BusinessCenter),
                                    Triple("Friendly", "Casual & conversational. Feels like a travel buddy planning with you.", Icons.Default.SentimentSatisfiedAlt),
                                    Triple("Funny", "Playful & humorous. Adds funny remarks and light jokes throughout.", Icons.Default.SentimentVerySatisfied),
                                    Triple("Roast Me", "Savage & sassy. Roasts questionable budget plans or overpacked trips!", Icons.Default.LocalFireDepartment)
                                )

                                personalities.forEach { (name, desc, icon) ->
                                    val isSelected = selectedPersonality == name
                                    val accentColor = when (name) {
                                        "Professional" -> ArtSoftLavender
                                        "Friendly" -> ArtMintGreen
                                        "Funny" -> ArtPeachGold
                                        "Roast Me" -> ArtTertiaryPink
                                        else -> ArtSoftLavender
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelected) accentColor else ArtCardBackground,
                                                RoundedCornerShape(16.dp)
                                            )
                                            .border(
                                                if (isSelected) 3.dp else 1.5.dp,
                                                ArtBorderDark,
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable { viewModel.selectedPersonality.value = name }
                                            .padding(14.dp)
                                            .testTag("personality_option_$name"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(if (isSelected) ArtSecondaryPurple else ArtBackground, CircleShape)
                                                .border(1.5.dp, ArtBorderDark, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) ArtPrimaryPurple else ArtTextDark,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = name.uppercase(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = ArtTextDark
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = desc,
                                                fontSize = 11.sp,
                                                color = ArtTextDark.copy(alpha = 0.75f),
                                                lineHeight = 14.sp
                                            )
                                        }

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(ArtSecondaryPurple, CircleShape)
                                                    .border(1.5.dp, ArtBorderDark, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = ArtPrimaryPurple,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Linear Progress Indicator
            LinearProgressIndicator(
                progress = { 1f },
                color = ArtPrimaryPurple,
                trackColor = ArtSoftLavender,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                // Origin location Card
                item {
                    QuestionCard(
                        title = "Where are you travelling from?",
                        icon = Icons.Default.FlightTakeoff,
                        accentColor = ArtPrimaryPurple
                    ) {
                        TextField(
                            value = fromLocation,
                            onValueChange = { viewModel.fromLocation.value = it },
                            placeholder = { Text("Starting city, e.g. Mumbai, New Delhi, New York", color = Color(0xFF94A3B8)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = ArtBackground,
                                unfocusedContainerColor = ArtBackground,
                                focusedTextColor = ArtTextDark,
                                unfocusedTextColor = ArtTextDark,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = ArtPrimaryPurple
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, ArtBorderDark, RoundedCornerShape(12.dp))
                                .testTag("origin_input"),
                            singleLine = true
                        )
                    }
                }

                // How many days? (Custom selector)
                item {
                    QuestionCard(
                        title = "How many days?",
                        icon = Icons.Default.CalendarMonth,
                        accentColor = ArtMintGreen
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { if (durationDays > 1) viewModel.updateDuration(durationDays - 1) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ArtSoftLavender, RoundedCornerShape(12.dp))
                                    .border(2.dp, ArtBorderDark, RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = ArtTextDark)
                            }

                            Text(
                                text = "$durationDays",
                                color = ArtTextDark,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 28.dp)
                            )

                            IconButton(
                                onClick = { if (durationDays < 14) viewModel.updateDuration(durationDays + 1) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ArtSoftLavender, RoundedCornerShape(12.dp))
                                    .border(2.dp, ArtBorderDark, RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = ArtTextDark)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ArtSoftLavender.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .border(1.5.dp, ArtBorderDark, RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = ArtPrimaryPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val startFormat = "${startDate.dayOfMonth} ${startDate.month.getDisplayName(TextStyle.SHORT, Locale.US)}"
                            val endFormat = "${endDate.dayOfMonth} ${endDate.month.getDisplayName(TextStyle.SHORT, Locale.US)}"
                            Text(
                                text = "Selected: $startFormat - $endFormat ($durationDays days)",
                                color = ArtTextDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        MiniCalendar(
                            startDate = startDate,
                            endDate = endDate,
                            onDateSelected = { start ->
                                viewModel.updateStartDate(start)
                            }
                        )
                    }
                }

                // Who is travelling?
                item {
                    QuestionCard(
                        title = "Who is travelling?",
                        icon = Icons.Default.Group,
                        accentColor = ArtPeachGold
                    ) {
                        val companionOptions = listOf("Solo", "Couple", "Friends", "Family")
                        val companionIcons = listOf(Icons.Default.Person, Icons.Default.Favorite, Icons.Default.Groups, Icons.Default.FamilyRestroom)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            companionOptions.forEachIndexed { idx, opt ->
                                val selected = travelerGroup == opt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (selected) ArtPrimaryPurple else ArtCardBackground,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.5.dp,
                                            ArtBorderDark,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.travelerGroup.value = opt }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = companionIcons[idx],
                                            contentDescription = null,
                                            tint = if (selected) Color.White else ArtTextDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = opt,
                                            color = if (selected) Color.White else ArtTextDark,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // What is your total budget?
                item {
                    QuestionCard(
                        title = "What is your total budget style?",
                        icon = Icons.Default.Payments,
                        accentColor = ArtTertiaryPink
                    ) {
                        val budgetOptions = listOf(
                            "₹5,000 - ₹15,000" to "Backpacker",
                            "₹15,000 - ₹35,000" to "Mid-Range",
                            "₹35,000 - ₹75,000" to "Luxury"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                budgetOptions.forEach { opt ->
                                    val selected = totalBudget == opt.first
                                    val budgetColor = when (opt.second) {
                                        "Backpacker" -> ArtPeachGold
                                        "Mid-Range" -> ArtSoftLavender
                                        "Luxury" -> ArtTertiaryPink
                                        else -> ArtSoftLavender
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (selected) budgetColor else ArtCardBackground,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                1.5.dp,
                                                ArtBorderDark,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.totalBudget.value = opt.first }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = opt.second,
                                                color = if (selected) Color(0xFF070B19) else ArtTextDark,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = opt.first,
                                                color = if (selected) Color(0xFF49454F) else ArtGrayMuted,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            
                            HorizontalDivider(color = ArtBorderDark.copy(alpha = 0.15f))
                            
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Or Customize Total Budget Range:",
                                color = ArtTextDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            var sliderRange by remember { mutableStateOf(5000f..100000f) }
                            
                            // Parse totalBudget state into sliderRange dynamically
                            LaunchedEffect(totalBudget) {
                                try {
                                    val clean = totalBudget.replace("₹", "").replace(",", "").replace("Lakh", "00000").replace(" Lakh", "00000")
                                    val parts = clean.split("-")
                                    if (parts.size == 2) {
                                        var startVal = parts[0].trim().toFloatOrNull() ?: 5000f
                                        var endVal = parts[1].trim().toFloatOrNull() ?: 100000f
                                        
                                        if (parts[0].contains("Lakh") || parts[0].contains("lakh")) {
                                            val lakhNum = parts[0].replace("lakh", "").replace("Lakh", "").trim().toFloatOrNull() ?: 0.5f
                                            startVal = lakhNum * 100000f
                                        }
                                        if (parts[1].contains("Lakh") || parts[1].contains("lakh")) {
                                            val lakhNum = parts[1].replace("lakh", "").replace("Lakh", "").trim().toFloatOrNull() ?: 2.5f
                                            endVal = lakhNum * 100000f
                                        }
                                        
                                        sliderRange = startVal.coerceIn(5000f, 250000f)..endVal.coerceIn(5000f, 250000f)
                                    }
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }

                            // Prominently display the currently selected budget range in the center using bold and larger text
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = totalBudget,
                                    color = ArtPrimaryPurple,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            RangeSlider(
                                value = sliderRange,
                                onValueChange = { range ->
                                    val coercedStart = (Math.round(range.start / 1000f) * 1000f).toFloat().coerceIn(5000f, 250000f)
                                    val coercedEnd = (Math.round(range.endInclusive / 1000f) * 1000f).toFloat().coerceIn(5000f, 250000f)
                                    if (coercedEnd - coercedStart >= 5000f) {
                                        sliderRange = coercedStart..coercedEnd
                                        viewModel.totalBudget.value = "${formatBudgetValue(coercedStart)} - ${formatBudgetValue(coercedEnd)}"
                                    }
                                },
                                valueRange = 5000f..250000f,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = ArtPrimaryPurple,
                                    inactiveTrackColor = ArtGrayLight,
                                    thumbColor = ArtPeachGold,
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("budget_range_slider")
                            )
                        }
                    }
                }

                // Preferred transportation?
                item {
                    QuestionCard(
                        title = "Preferred transportation?",
                        icon = Icons.Default.DirectionsTransit,
                        accentColor = ArtPrimaryPurple
                    ) {
                        val transportOptions = if (travelerGroup == "Solo") {
                            listOf("Train", "Bus", "Flight", "Car", "Bike Drive", "No Preference")
                        } else {
                            listOf("Train", "Bus", "Flight", "Car", "No Preference")
                        }
                        val transportIcons = if (travelerGroup == "Solo") {
                            listOf(
                                Icons.Default.DirectionsTransit,
                                Icons.Default.DirectionsBus,
                                Icons.Default.Flight,
                                Icons.Default.DirectionsCar,
                                Icons.Default.DirectionsBike,
                                Icons.Default.HelpOutline
                            )
                        } else {
                            listOf(
                                Icons.Default.DirectionsTransit,
                                Icons.Default.DirectionsBus,
                                Icons.Default.Flight,
                                Icons.Default.DirectionsCar,
                                Icons.Default.HelpOutline
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(transportOptions.zip(transportIcons)) { (opt, icon) ->
                                val selected = preferredTransportation == opt
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selected) ArtMintGreen else ArtCardBackground,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.5.dp,
                                            ArtBorderDark,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.preferredTransportation.value = opt }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (selected) Color(0xFF070B19) else ArtTextDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = opt,
                                            color = if (selected) Color(0xFF070B19) else ArtTextDark,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Travel Style?
                item {
                    QuestionCard(
                        title = "Choose your Travel Style",
                        icon = Icons.Default.Hiking,
                        accentColor = ArtPrimaryPurple
                    ) {
                        val styles = listOf("Adventure", "Luxury", "Relaxation", "Nature", "Party", "Photography", "Backpacking", "Spiritual", "Food Exploration")

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            styles.forEach { styleName ->
                                val selected = selectedTravelStyles.contains(styleName)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selected) ArtPeachGold else ArtCardBackground,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.5.dp,
                                            ArtBorderDark,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.toggleTravelStyle(styleName) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = styleName,
                                        color = if (selected) Color(0xFF070B19) else ArtTextDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Travel Pace
                item {
                    QuestionCard(
                        title = "What is your travel pace?",
                        icon = Icons.Default.AvTimer,
                        accentColor = ArtPrimaryPurple
                    ) {
                        val paces = listOf("Relaxed", "Fast-paced")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            paces.forEach { opt ->
                                val selected = travelPace == opt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (selected) ArtSoftLavender else ArtCardBackground,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.5.dp,
                                            ArtBorderDark,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.travelPace.value = opt }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt,
                                        color = if (selected) Color(0xFF070B19) else ArtTextDark,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Special Requirements
                item {
                    QuestionCard(
                        title = "Any special requirements?",
                        icon = Icons.Default.AccessibilityNew,
                        accentColor = ArtPrimaryPurple
                    ) {
                        val requirements = listOf("None", "Vegetarian", "Pet Friendly", "Kids", "Senior Citizens", "Wheelchair Friendly", "Workation", "Honeymoon")
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            requirements.forEach { opt ->
                                val selected = selectedSpecialRequirements.contains(opt)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selected) ArtTertiaryPink else ArtCardBackground,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.5.dp,
                                            ArtBorderDark,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.toggleSpecialRequirement(opt) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = opt,
                                        color = if (selected) Color(0xFF070B19) else ArtTextDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Generous Spacer before plan button
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            }

            // Bottom sticky generate button
            Surface(
                color = ArtCardBackground,
                border = BorderStroke(2.dp, ArtBorderDark),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 16.dp
            ) {
                var showModelPicker by remember { mutableStateOf(false) }
                val currentModel by viewModel.selectedModel.collectAsState()
                val userKeyVal by viewModel.userApiKey.collectAsState()

                if (showModelPicker) {
                    AlertDialog(
                        onDismissRequest = { showModelPicker = false },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                        modifier = Modifier
                            .padding(24.dp)
                            .widthIn(max = 440.dp),
                        shape = RoundedCornerShape(24.dp),
                        containerColor = ArtCardBackground,
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = ArtPrimaryPurple)
                                Text(
                                    "Select Gemini AI Model",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = ArtTextDark
                                )
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Choose which Gemini model to generate your trip plan with (${if (userKeyVal.isNotBlank()) "Custom API Key" else "Built-in App Key"}).",
                                    fontSize = 12.sp,
                                    color = ArtGrayMuted,
                                    lineHeight = 16.sp
                                )

                                viewModel.availableGeminiModels.forEach { modelOption ->
                                    val isSelected = currentModel == modelOption.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelected) ArtSecondaryPurple else Color.Transparent,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                1.5.dp,
                                                if (isSelected) ArtPrimaryPurple else ArtBorderDark.copy(alpha = 0.3f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                viewModel.selectGeminiModel(modelOption.id)
                                                showModelPicker = false
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.selectGeminiModel(modelOption.id)
                                                showModelPicker = false
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = ArtPrimaryPurple)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = modelOption.displayName,
                                                    color = ArtTextDark,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (modelOption.badge == "Recommended") Color(0xFFDCFCE7) else ArtSecondaryPurple
                                                ) {
                                                    Text(
                                                        text = modelOption.badge,
                                                        color = if (modelOption.badge == "Recommended") Color(0xFF166534) else ArtPrimaryPurple,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = modelOption.description,
                                                color = ArtGrayMuted,
                                                fontSize = 11.sp,
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showModelPicker = false }) {
                                Text("Done", fontWeight = FontWeight.Bold, color = ArtPrimaryPurple)
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Model Indicator / Quick Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (userApiKey.isNotBlank()) 1f else 0.5f)
                            .clickable(enabled = userApiKey.isNotBlank()) { showModelPicker = true },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ArtSecondaryPurple,
                            border = BorderStroke(1.dp, ArtPrimaryPurple.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = ArtPrimaryPurple,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "AI Model: ${currentModel.replace("gemini-", "").replace("-preview", "").uppercase()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ArtPrimaryPurple
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = ArtPrimaryPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (userApiKey.isBlank()) {
                                showGeminiSetupDialog = true
                            } else {
                                viewModel.startTripGeneration()
                                onNavigateToItinerary()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .border(2.dp, ArtBorderDark, RoundedCornerShape(14.dp))
                            .testTag("generate_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArtPrimaryPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "GENERATE AI ITINERARY",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (showGeminiSetupDialog) {
            GeminiSetupDialog(
                viewModel = viewModel,
                initialStep = 0,
                onDismiss = { showGeminiSetupDialog = false },
                onConnectedSuccess = {
                    showGeminiSetupDialog = false
                    viewModel.startTripGeneration()
                    onNavigateToItinerary()
                }
            )
        }
    }
}

@Composable
fun QuestionCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
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
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ArtSoftLavender, CircleShape)
                            .border(1.5.dp, ArtBorderDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ArtPrimaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArtTextDark
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                content()
            }
        }
    }
}

// Simple FlowRow implementation for Jetpack Compose since standard FlowRow is in material3 experimental
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

@Composable
fun MiniCalendar(
    startDate: LocalDate,
    endDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(startDate)) }
    val today = remember { LocalDate.now() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ArtBackground, RoundedCornerShape(16.dp))
            .border(2.dp, ArtBorderDark, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Month Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(ArtSoftLavender, CircleShape)
                    .border(1.dp, ArtBorderDark, CircleShape)
                    .clickable { currentMonth = currentMonth.minusMonths(1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = ArtTextDark,
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.US)} ${currentMonth.year}",
                color = ArtTextDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(ArtSoftLavender, CircleShape)
                    .border(1.dp, ArtBorderDark, CircleShape)
                    .clickable { currentMonth = currentMonth.plusMonths(1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = ArtTextDark,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Weekdays row
        val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekdays.forEach { day ->
                Text(
                    text = day,
                    color = ArtGrayMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Days Grid
        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7 // 0 = Sunday, 1 = Monday...
        val totalCells = daysInMonth + firstDayOfWeek
        val rowsCount = (totalCells + 6) / 7

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (row in 0 until rowsCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - firstDayOfWeek + 1

                        if (dayNum in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayNum)
                            val isPast = date.isBefore(today)
                            val isStart = date == startDate
                            val isEnd = date == endDate
                            val inRange = date.isAfter(startDate) && date.isBefore(endDate)

                            val bg = when {
                                isPast -> Color.Transparent
                                isStart || isEnd -> ArtPrimaryPurple
                                inRange -> ArtSoftLavender
                                else -> Color.Transparent
                            }

                            val border = when {
                                isPast -> BorderStroke(1.dp, ArtBorderDark.copy(alpha = 0.08f))
                                isStart || isEnd -> BorderStroke(2.dp, ArtBorderDark)
                                inRange -> BorderStroke(1.5.dp, ArtBorderDark.copy(alpha = 0.5f))
                                else -> BorderStroke(1.dp, ArtBorderDark.copy(alpha = 0.15f))
                            }

                            val txtColor = when {
                                isPast -> ArtGrayMuted.copy(alpha = 0.35f)
                                isStart || isEnd -> Color.White
                                inRange -> ArtTextDark
                                else -> ArtTextDark
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .border(border.width, border.brush, RoundedCornerShape(8.dp))
                                    .then(
                                        if (!isPast) {
                                            Modifier.clickable { onDateSelected(date) }
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$dayNum",
                                    color = txtColor,
                                    fontSize = 12.sp,
                                    fontWeight = if (isStart || isEnd) FontWeight.Black else FontWeight.Medium
                                )
                            }
                        } else {
                            Box(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}

fun formatBudgetValue(value: Float): String {
    val intVal = value.toInt()
    return when {
        intVal >= 100000 -> {
            val lakhs = intVal / 100000f
            if (lakhs % 1 == 0f) {
                "₹${lakhs.toInt()} Lakh"
            } else {
                "₹${String.format(Locale.US, "%.1f", lakhs)} Lakh"
            }
        }
        else -> {
            "₹%,d".format(Locale.US, intVal)
        }
    }
}

