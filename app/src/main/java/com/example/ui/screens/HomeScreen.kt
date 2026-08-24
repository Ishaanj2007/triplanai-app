package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.data.local.TripEntity
import com.example.ui.theme.*
import com.example.viewmodel.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TripViewModel,
    onNavigateToQuestions: () -> Unit,
    onNavigateToItinerary: () -> Unit,
    onNavigateToSavedTrips: () -> Unit
) {
    val savedTrips by viewModel.savedTrips.collectAsState()
    val destinationInput by viewModel.destination.collectAsState()

    var apiSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    val targetSettingsTab by viewModel.shouldOpenSettingsTab.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedSettingsTab by remember { mutableStateOf(0) }

    LaunchedEffect(targetSettingsTab) {
        targetSettingsTab?.let { tab ->
            selectedSettingsTab = tab
            showSettingsDialog = true
            viewModel.consumeOpenSettings()
        }
    }

    LaunchedEffect(destinationInput) {
        if (destinationInput.isBlank() || destinationInput.length < 2) {
            apiSuggestions = emptyList()
            return@LaunchedEffect
        }

        kotlinx.coroutines.delay(300)

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(destinationInput, "UTF-8")
                val url = java.net.URL("https://api.teleport.org/api/cities/?search=$encodedQuery")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000

                if (connection.responseCode == 200) {
                    val text = connection.inputStream.bufferedReader().use { it.readText() }

                    val regex = """"matching_full_name":\s*"([^"]+)"""".toRegex()
                    val matches = regex.findAll(text)
                        .map { it.groupValues[1] }
                        .map {
                            val parts = it.split(",")
                            if (parts.size >= 3) {
                                "${parts[0].trim()}, ${parts[parts.lastIndex].trim()}"
                            } else {
                                it
                            }
                        }
                        .distinct()
                        .take(3)
                        .toList()

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        apiSuggestions = matches
                    }
                } else {
                    throw Exception("Non-200 response")
                }
            } catch (e: Exception) {
                val allGlobalCities = listOf(
                    "Goa, India", "Japan", "Manali, India", "Paris, France", "Ladakh, India", "Bali, Indonesia",
                    "London, UK", "New York, USA", "Tokyo, Japan", "Rome, Italy", "Barcelona, Spain",
                    "Maldives", "Singapore", "Dubai, UAE", "Bangkok, Thailand", "Kashmir, India",
                    "Kerala, India", "Agra, India", "Jaipur, India", "Udaipur, India", "Sydney, Australia",
                    "Switzerland", "Mumbai, India", "Delhi, India", "Bengaluru, India", "Chennai, India",
                    "Kolkata, India", "Hyderabad, India", "Pune, India", "Amsterdam, Netherlands",
                    "Berlin, Germany", "Vienna, Austria", "Prague, Czech Republic", "Lisbon, Portugal",
                    "Madrid, Spain", "Athens, Greece", "Cairo, Egypt", "Cape Town, South Africa",
                    "Rio de Janeiro, Brazil", "Buenos Aires, Argentina", "Toronto, Canada", "Vancouver, Canada",
                    "San Francisco, USA", "Los Angeles, USA", "Chicago, USA", "Miami, USA", "Honolulu, USA"
                )
                val matches = allGlobalCities.filter {
                    it.contains(destinationInput, ignoreCase = true) && !it.equals(destinationInput, ignoreCase = true)
                }.take(3)

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    apiSuggestions = matches
                }
            }
        }
    }

    var showDeleteDialogForId by remember { mutableStateOf<Int?>(null) }
    var showAllTrendingDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArtBackground)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // 1. HERO SECTION
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
                ) {
                    // Header Bar with Brand and Settings Gear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = ArtPrimaryPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TriplanAi",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = ArtTextDark
                            )
                        }

                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                                .background(ArtCardBackground, CircleShape)
                                .border(1.dp, ArtBorderDark, CircleShape)
                                .shadow(elevation = 2.dp, shape = CircleShape)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = ArtTextDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Responsive Hero Container
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val availableWidth = maxWidth

                        val headingAnnotatedString = buildAnnotatedString {
                            append("Where ")
                            withStyle(
                                SpanStyle(
                                    color = ArtPrimaryPurple,
                                    fontStyle = FontStyle.Italic,
                                    fontFamily = FontFamily.Serif
                                )
                            ) {
                                append("do you")
                            }
                            append("\nwant to go?")
                        }

                        val subtitleText = "Answer a few questions and our AI will build your perfect, optimized adventure in seconds."

                        if (availableWidth >= 600.dp) {
                            // TABLET / EXPANDED LAYOUT (Two-column: Left = Heading + Subtitle, Right = Illustration)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 24.dp)
                                ) {
                                    Text(
                                        text = headingAnnotatedString,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ArtTextDark,
                                        lineHeight = 40.sp,
                                        letterSpacing = (-0.8).sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = subtitleText,
                                        fontSize = 14.sp,
                                        color = ArtGrayMuted,
                                        lineHeight = 21.sp
                                    )
                                }

                                HeroTravelIllustration(
                                    modifier = Modifier
                                        .widthIn(min = 140.dp, max = 180.dp)
                                        .aspectRatio(1.3f)
                                )
                            }
                        } else {
                            // PHONE LAYOUT (Responsive Row with Heading on left and Illustration on right, Subtitle below)
                            val isSmallPhone = availableWidth < 345.dp
                            val isLargePhone = availableWidth >= 480.dp

                            val headingFontSize = when {
                                isSmallPhone -> 25.sp
                                isLargePhone -> 33.sp
                                else -> 30.sp
                            }
                            val headingLineHeight = when {
                                isSmallPhone -> 29.sp
                                isLargePhone -> 37.sp
                                else -> 34.sp
                            }
                            val illustrationWidth = when {
                                isSmallPhone -> 64.dp
                                isLargePhone -> 120.dp
                                else -> 95.dp
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = headingAnnotatedString,
                                        fontSize = headingFontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = ArtTextDark,
                                        lineHeight = headingLineHeight,
                                        letterSpacing = (-0.7).sp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = if (isSmallPhone) 6.dp else 12.dp)
                                    )

                                    HeroTravelIllustration(
                                        modifier = Modifier
                                            .width(illustrationWidth)
                                            .aspectRatio(1.3f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(if (isSmallPhone) 8.dp else 10.dp))

                                Text(
                                    text = subtitleText,
                                    fontSize = if (isSmallPhone) 12.sp else 13.sp,
                                    color = ArtGrayMuted,
                                    lineHeight = if (isSmallPhone) 17.sp else 19.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. DESTINATION / PLAN CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = ArtPrimaryPurple.copy(alpha = 0.12f),
                            ambientColor = ArtPrimaryPurple.copy(alpha = 0.08f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                    border = BorderStroke(1.dp, ArtBorderDark)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "ENTER DESTINATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArtPrimaryPurple,
                            letterSpacing = 1.2.sp
                        )

                        // Location Input Field
                        TextField(
                            value = destinationInput,
                            onValueChange = { viewModel.destination.value = it },
                            placeholder = {
                                Text(
                                    "Where do you want to go?",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = ArtGrayLight,
                                unfocusedContainerColor = ArtGrayLight,
                                focusedTextColor = ArtTextDark,
                                unfocusedTextColor = ArtTextDark,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = ArtPrimaryPurple
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, ArtBorderDark, RoundedCornerShape(16.dp))
                                .testTag("destination_input"),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = ArtPrimaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        viewModel.destination.value = "Goa, India"
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GpsFixed,
                                        contentDescription = "Current location",
                                        tint = ArtGrayMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )

                        // API Suggestions
                        if (apiSuggestions.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    Text("Suggestions:", fontSize = 11.sp, color = ArtGrayMuted, fontWeight = FontWeight.Bold)
                                }
                                items(apiSuggestions) { suggestion ->
                                    Box(
                                        modifier = Modifier
                                            .background(ArtSecondaryPurple, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.destination.value = suggestion }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ArtPrimaryPurple
                                        )
                                    }
                                }
                            }
                        }

                        // Primary Purple CTA Button
                        Button(
                            onClick = {
                                if (destinationInput.isNotBlank()) {
                                    onNavigateToQuestions()
                                }
                            },
                            enabled = destinationInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("search_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ArtPrimaryPurple,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFE2E8F0),
                                disabledContentColor = Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "PLAN MY TRIP",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Feature Strip inside card below CTA
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = ArtGrayLight
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FeaturePill(icon = Icons.Default.AutoAwesome, label = "AI Powered")
                                FeaturePill(icon = Icons.Default.VerifiedUser, label = "Optimized Plans")
                                FeaturePill(icon = Icons.Default.Bolt, label = "In Seconds")
                            }
                        }
                    }
                }
            }

            // 3. TRENDING DESTINATIONS
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRENDING DESTINATIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArtPrimaryPurple,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "View all →",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArtPrimaryPurple,
                        modifier = Modifier.clickable { showAllTrendingDialog = true }
                    )
                }
            }

            item {
                val trendingList = listOf(
                    TrendingDest("Goa", "Beaches, Nightlife &\nmore", Icons.Default.BeachAccess, ArtPeachGold, ArtPeachGold.copy(alpha = 0.6f)),
                    TrendingDest("Japan", "Cherry Blossoms, Temples\n& Culture", Icons.Default.CameraAlt, ArtSoftLavender, ArtSoftLavender.copy(alpha = 0.6f)),
                    TrendingDest("Manali", "Snowy Mountains\n& Adventures", Icons.Default.Terrain, ArtMintGreen, ArtMintGreen.copy(alpha = 0.6f)),
                    TrendingDest("Paris", "Art, Romance &\nCafes", Icons.Default.Palette, ArtTertiaryPink, ArtTertiaryPink.copy(alpha = 0.6f)),
                    TrendingDest("Ladakh", "Roadtrip & Blue\nLakes", Icons.Default.DirectionsBike, ArtSecondaryPurple, ArtSecondaryPurple.copy(alpha = 0.6f)),
                    TrendingDest("Kerala", "Serene Backwaters &\nHouseboats", Icons.Default.DirectionsBoat, ArtMintGreen, ArtMintGreen.copy(alpha = 0.6f)),
                    TrendingDest("Bali", "Tropical Beaches &\nSurf", Icons.Default.Spa, ArtTertiaryPink, ArtTertiaryPink.copy(alpha = 0.6f)),
                    TrendingDest("Singapore", "Futuristic Skylines &\nGardens", Icons.Default.Business, ArtSoftLavender, ArtSoftLavender.copy(alpha = 0.6f))
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(trendingList) { dest ->
                        TrendingDestinationCard(dest = dest) {
                            viewModel.destination.value = dest.name
                            onNavigateToQuestions()
                        }
                    }
                }
            }

            // 4. MY SAVED TRIPS
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MY SAVED TRIPS (${savedTrips.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArtPrimaryPurple,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "View all →",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArtPrimaryPurple,
                        modifier = Modifier.clickable { onNavigateToSavedTrips() }
                    )
                }
            }

            if (savedTrips.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                        border = BorderStroke(1.dp, ArtBorderDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = ArtPrimaryPurple,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                "No saved itineraries yet.",
                                color = ArtTextDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Plan an AI trip and tap 'Save Trip' to archive your itineraries here.",
                                color = ArtGrayMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(savedTrips) { trip ->
                    SavedTripRowCard(
                        trip = trip,
                        onView = {
                            viewModel.viewSavedTrip(trip)
                            onNavigateToItinerary()
                        },
                        onDelete = {
                            showDeleteDialogForId = trip.id
                        }
                    )
                }
            }
        }
    }

    // Modal: All Trending Destinations
    if (showAllTrendingDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAllTrendingDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 420.dp)
                    .heightIn(max = 520.dp)
                    .border(2.dp, ArtBorderDark, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ArtCardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trending Destinations",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArtTextDark
                        )
                        IconButton(
                            onClick = { showAllTrendingDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = ArtTextDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val allDestinations = listOf(
                        TrendingDest("Goa", "Beaches, Nightlife & Water Sports", Icons.Default.BeachAccess, ArtPeachGold, ArtPeachGold.copy(alpha = 0.6f)),
                        TrendingDest("Japan", "Cherry Blossoms, Temples & Tech", Icons.Default.CameraAlt, ArtSoftLavender, ArtSoftLavender.copy(alpha = 0.6f)),
                        TrendingDest("Manali", "Snowy Mountains & Adventures", Icons.Default.Terrain, ArtMintGreen, ArtMintGreen.copy(alpha = 0.6f)),
                        TrendingDest("Paris", "Art, Romance, Fashion & Cafes", Icons.Default.Palette, ArtTertiaryPink, ArtTertiaryPink.copy(alpha = 0.6f)),
                        TrendingDest("Ladakh", "Himalayan Passes & Blue Lakes", Icons.Default.DirectionsBike, ArtSecondaryPurple, ArtSecondaryPurple.copy(alpha = 0.6f)),
                        TrendingDest("Kerala", "Serene Backwaters & Houseboats", Icons.Default.DirectionsBoat, ArtMintGreen, ArtMintGreen.copy(alpha = 0.6f)),
                        TrendingDest("Bali", "Tropical Beaches & Volcanic Views", Icons.Default.Spa, ArtTertiaryPink, ArtTertiaryPink.copy(alpha = 0.6f)),
                        TrendingDest("Singapore", "Futuristic Skylines & Gardens", Icons.Default.Business, ArtSoftLavender, ArtSoftLavender.copy(alpha = 0.6f))
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(allDestinations) { dest ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(dest.badgeBg, RoundedCornerShape(16.dp))
                                    .border(1.dp, ArtBorderDark.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        viewModel.destination.value = dest.name
                                        showAllTrendingDialog = false
                                        onNavigateToQuestions()
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = null,
                                    tint = ArtTextDark,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dest.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ArtTextDark)
                                    Text(dest.vibe, fontSize = 12.sp, color = ArtGrayMuted, maxLines = 1)
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = ArtPrimaryPurple, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialogForId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialogForId = null },
            title = { Text("Delete Itinerary", color = ArtTextDark, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this trip plan?", color = ArtGrayMuted) },
            containerColor = ArtCardBackground,
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialogForId?.let { id ->
                            viewModel.deleteSavedTrip(id)
                        }
                        showDeleteDialogForId = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialogForId = null }) {
                    Text("Cancel", color = ArtGrayMuted)
                }
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        val currentTheme by viewModel.selectedTheme.collectAsState()
        val currentModel by viewModel.selectedModel.collectAsState()
        val userKeyVal by viewModel.userApiKey.collectAsState()
        val groqKeyVal by viewModel.groqApiKey.collectAsState()
        val autoSaveVal by viewModel.autoSaveEnabled.collectAsState()
        val selectedPersonality by viewModel.selectedPersonality.collectAsState()
        val testStatus by viewModel.testConnectionStatus.collectAsState()
        val isTesting by viewModel.isTestingConnection.collectAsState()
        val groqTestStatus by viewModel.groqTestStatus.collectAsState()
        val isTestingGroq by viewModel.isTestingGroq.collectAsState()

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSettingsDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = ArtBackground
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(ArtSecondaryPurple, RoundedCornerShape(12.dp))
                                .clickable { showSettingsDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ArtPrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Settings",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArtTextDark
                        )
                    }

                    TabRow(
                        selectedTabIndex = selectedSettingsTab,
                        containerColor = Color.Transparent,
                        contentColor = ArtPrimaryPurple,
                        divider = {
                            HorizontalDivider(color = ArtBorderDark.copy(alpha = 0.35f), thickness = 1.dp)
                        },
                        indicator = { tabPositions ->
                            if (selectedSettingsTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedSettingsTab]),
                                    color = ArtPrimaryPurple,
                                    height = 3.dp
                                )
                            }
                        }
                    ) {
                        listOf("General", "AI Providers", "Support & About").forEachIndexed { index, title ->
                            val selected = selectedSettingsTab == index
                            Tab(
                                selected = selected,
                                onClick = { selectedSettingsTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                },
                                selectedContentColor = ArtPrimaryPurple,
                                unselectedContentColor = ArtGrayMuted
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (selectedSettingsTab) {
                            0 -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        top = 16.dp,
                                        end = 16.dp,
                                        bottom = 48.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    // -------------------------------------------------------------
                                    // SECTION 1: APPEARANCE
                                    // -------------------------------------------------------------
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .widthIn(max = 680.dp)
                                        ) {
                                            Text(
                                                text = "APPEARANCE",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ArtPrimaryPurple,
                                                letterSpacing = 0.8.sp,
                                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                            )

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(22.dp),
                                                border = BorderStroke(1.dp, ArtBorderDark.copy(alpha = 0.6f)),
                                                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    val themeOptions = listOf(
                                                        0 to Pair("Auto (System Default)", "Follow Android system Dark / Light mode"),
                                                        1 to Pair("Deep Dark Mode", "Comfortable dark theme for low light"),
                                                        2 to Pair("Soft Pastel (Light)", "Clean and modern pastel look"),
                                                        3 to Pair("Coral Sunset (Warm)", "Warm and cozy sunset palette")
                                                    )

                                                    themeOptions.forEachIndexed { index, (themeKey, details) ->
                                                        val (name, subtitle) = details
                                                        val selected = currentTheme == themeKey

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(14.dp))
                                                                .background(if (selected) ArtSecondaryPurple else Color.Transparent)
                                                                .clickable { viewModel.selectedTheme.value = themeKey }
                                                                .padding(horizontal = 14.dp, vertical = 13.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(22.dp)
                                                                    .border(
                                                                        width = if (selected) 2.dp else 1.5.dp,
                                                                        color = if (selected) ArtPrimaryPurple else Color(0xFF6B7280).copy(alpha = 0.7f),
                                                                        shape = CircleShape
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (selected) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(10.dp)
                                                                            .background(ArtPrimaryPurple, CircleShape)
                                                                    )
                                                                }
                                                            }

                                                            Spacer(modifier = Modifier.width(14.dp))

                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = name,
                                                                    color = ArtTextDark,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                Text(
                                                                    text = subtitle,
                                                                    color = ArtGrayMuted,
                                                                    fontSize = 12.sp,
                                                                    modifier = Modifier.padding(top = 2.dp)
                                                                )
                                                            }
                                                        }

                                                        if (index < themeOptions.size - 1) {
                                                            val nextSelected = currentTheme == themeOptions[index + 1].first
                                                            if (!selected && !nextSelected) {
                                                                HorizontalDivider(
                                                                    color = ArtBorderDark.copy(alpha = 0.4f),
                                                                    thickness = 0.8.dp,
                                                                    modifier = Modifier.padding(horizontal = 14.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // -------------------------------------------------------------
                                    // SECTION 2: AUTO-SAVE
                                    // -------------------------------------------------------------
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .widthIn(max = 680.dp)
                                        ) {
                                            Text(
                                                text = "AUTO-SAVE",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ArtPrimaryPurple,
                                                letterSpacing = 0.8.sp,
                                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                            )

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(22.dp),
                                                border = BorderStroke(1.dp, ArtBorderDark.copy(alpha = 0.6f)),
                                                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(46.dp)
                                                            .background(ArtSecondaryPurple, RoundedCornerShape(14.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.CloudUpload,
                                                            contentDescription = "Auto-save",
                                                            tint = ArtPrimaryPurple,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(14.dp))

                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(
                                                            text = "Auto-save itineraries",
                                                            color = ArtTextDark,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        )
                                                        Text(
                                                            text = "Automatically save your generated trip plans",
                                                            color = ArtGrayMuted,
                                                            fontSize = 12.sp,
                                                            modifier = Modifier.padding(top = 2.dp)
                                                        )
                                                    }

                                                    Switch(
                                                        checked = autoSaveVal,
                                                        onCheckedChange = { viewModel.setAutoSaveEnabled(it) },
                                                        colors = SwitchDefaults.colors(
                                                            checkedThumbColor = Color.White,
                                                            checkedTrackColor = ArtPrimaryPurple,
                                                            uncheckedThumbColor = ArtGrayMuted,
                                                            uncheckedTrackColor = ArtBorderDark.copy(alpha = 0.5f)
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // -------------------------------------------------------------
                                    // SECTION 3: TRAVEL ASSISTANT TONE
                                    // -------------------------------------------------------------
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .widthIn(max = 680.dp)
                                        ) {
                                            Text(
                                                text = "TRAVEL ASSISTANT TONE",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ArtPrimaryPurple,
                                                letterSpacing = 0.8.sp,
                                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                            )

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(22.dp),
                                                border = BorderStroke(1.dp, ArtBorderDark.copy(alpha = 0.6f)),
                                                colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    val tones = listOf(
                                                        "Formal" to "Clear, structured and professional guidance",
                                                        "Casual" to "Warm, friendly and conversational travel buddy",
                                                        "Funny" to "Light humor, playful tips and witty remarks",
                                                        "Roast My Plan" to "Honest, bold and brutally fun feedback"
                                                    )

                                                    tones.forEachIndexed { index, (toneName, subtitle) ->
                                                        val normalizedCurrent = TripViewModel.normalizeTone(selectedPersonality)
                                                        val selected = normalizedCurrent.equals(toneName, ignoreCase = true)

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(14.dp))
                                                                .background(if (selected) ArtSecondaryPurple else Color.Transparent)
                                                                .clickable { viewModel.selectTone(toneName) }
                                                                .padding(horizontal = 14.dp, vertical = 13.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(22.dp)
                                                                    .border(
                                                                        width = if (selected) 2.dp else 1.5.dp,
                                                                        color = if (selected) ArtPrimaryPurple else Color(0xFF6B7280).copy(alpha = 0.7f),
                                                                        shape = CircleShape
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (selected) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(10.dp)
                                                                            .background(ArtPrimaryPurple, CircleShape)
                                                                    )
                                                                }
                                                            }

                                                            Spacer(modifier = Modifier.width(14.dp))

                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = toneName,
                                                                    color = ArtTextDark,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 14.sp
                                                                )
                                                                Text(
                                                                    text = subtitle,
                                                                    color = ArtGrayMuted,
                                                                    fontSize = 12.sp,
                                                                    modifier = Modifier.padding(top = 2.dp)
                                                                )
                                                            }
                                                        }

                                                        if (index < tones.size - 1) {
                                                            val nextSelected = selectedPersonality.equals(tones[index + 1].first, ignoreCase = true)
                                                            if (!selected && !nextSelected) {
                                                                HorizontalDivider(
                                                                    color = ArtBorderDark.copy(alpha = 0.4f),
                                                                    thickness = 0.8.dp,
                                                                    modifier = Modifier.padding(horizontal = 14.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(28.dp))
                                    }
                                }
                            }
                            1 -> {
                                var showGeminiSetupModal by remember { mutableStateOf(false) }
                                var geminiSetupInitialStep by remember { mutableStateOf(0) }
                                var showRemoveKeyConfirmDialog by remember { mutableStateOf(false) }
                                var showGroqKeyDialog by remember { mutableStateOf(false) }
                                var showRevealedGeminiKey by remember { mutableStateOf(false) }
                                val context = LocalContext.current

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        top = 16.dp,
                                        end = 16.dp,
                                        bottom = 48.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    item {
                                        // -------------------------------------------------------------
                                        // SECTION 1: GEMINI CONNECTION & STATUS
                                        // -------------------------------------------------------------
                                        Card(
                                            modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.5.dp, if (userKeyVal.isNotBlank()) ArtPrimaryPurple else ArtBorderDark),
                                            colors = CardDefaults.cardColors(containerColor = ArtCardBackground)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .background(ArtSecondaryPurple, CircleShape)
                                                                .border(1.dp, ArtPrimaryPurple.copy(alpha = 0.3f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Key,
                                                                contentDescription = null,
                                                                tint = ArtPrimaryPurple,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "GEMINI",
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 14.sp,
                                                            color = ArtTextDark
                                                        )
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = when {
                                                            isTesting -> ArtSecondaryPurple
                                                            userKeyVal.isNotBlank() -> Color(0xFFDCFCE7)
                                                            else -> Color(0xFFF3F4F6)
                                                        },
                                                        border = BorderStroke(
                                                            1.dp,
                                                            when {
                                                                isTesting -> ArtPrimaryPurple
                                                                userKeyVal.isNotBlank() -> Color(0xFF86EFAC)
                                                                else -> Color(0xFFE5E7EB)
                                                            }
                                                        )
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            if (isTesting) {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(10.dp),
                                                                    strokeWidth = 1.5.dp,
                                                                    color = ArtPrimaryPurple
                                                                )
                                                            }
                                                            Text(
                                                                text = when {
                                                                    isTesting -> "Testing..."
                                                                    userKeyVal.isNotBlank() -> "✓ Connected"
                                                                    else -> "○ Not connected"
                                                                },
                                                                color = when {
                                                                    isTesting -> ArtPrimaryPurple
                                                                    userKeyVal.isNotBlank() -> Color(0xFF166534)
                                                                    else -> ArtGrayMuted
                                                                },
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = "TripPlanAI uses Google Gemini to generate complete, personalized travel itineraries.",
                                                    fontSize = 12.sp,
                                                    color = ArtGrayMuted,
                                                    lineHeight = 16.sp
                                                )

                                                if (userKeyVal.isNotBlank()) {
                                                    val maskedKey = if (userKeyVal.length > 4) {
                                                        "************${userKeyVal.takeLast(4)}"
                                                    } else {
                                                        "************"
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(ArtBackground, RoundedCornerShape(12.dp))
                                                            .border(1.dp, ArtBorderDark.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text("SAVED ON THIS DEVICE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ArtGrayMuted)
                                                                Text(
                                                                    text = maskedKey,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = ArtTextDark
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                geminiSetupInitialStep = 4
                                                                showGeminiSetupModal = true
                                                            },
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = ArtPrimaryPurple)
                                                        ) {
                                                            Text("Change Key", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                                        }

                                                        OutlinedButton(
                                                            onClick = { viewModel.testGeminiConnection() },
                                                            enabled = !isTesting,
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(12.dp),
                                                            border = BorderStroke(1.dp, ArtPrimaryPurple),
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ArtPrimaryPurple)
                                                        ) {
                                                            if (isTesting) {
                                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = ArtPrimaryPurple)
                                                            } else {
                                                                Text("Test Connection", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                                            }
                                                        }
                                                    }

                                                    if (testStatus != null) {
                                                        val isSuccess = testStatus!!.startsWith("SUCCESS", ignoreCase = true)
                                                        val isError = testStatus!!.startsWith("ERROR", ignoreCase = true)
                                                        val displayMessage = testStatus!!.removePrefix("SUCCESS: ").removePrefix("ERROR: ")

                                                        Surface(
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = if (isSuccess) Color(0xFFDCFCE7) else if (isError) Color(0xFFFEE2E2) else ArtSecondaryPurple,
                                                            border = BorderStroke(1.dp, if (isSuccess) Color(0xFF86EFAC) else if (isError) Color(0xFFFCA5A5) else ArtPrimaryPurple),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(10.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else if (isError) Icons.Default.Warning else Icons.Default.Info,
                                                                    contentDescription = null,
                                                                    tint = if (isSuccess) Color(0xFF166534) else if (isError) Color(0xFFB91C1C) else ArtPrimaryPurple,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Text(
                                                                    text = displayMessage,
                                                                    fontSize = 11.sp,
                                                                    color = if (isSuccess) Color(0xFF166534) else if (isError) Color(0xFFB91C1C) else ArtPrimaryPurple,
                                                                    fontWeight = FontWeight.Medium,
                                                                    lineHeight = 14.sp
                                                                )
                                                            }
                                                        }
                                                    }

                                                    TextButton(
                                                        onClick = { showRemoveKeyConfirmDialog = true },
                                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                                    ) {
                                                        Text("Disconnect Key", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            geminiSetupInitialStep = 0
                                                            showGeminiSetupModal = true
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(46.dp),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = ArtPrimaryPurple)
                                                    ) {
                                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Connect Gemini", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    }
                                                }

                                                HorizontalDivider(color = ArtBorderDark.copy(alpha = 0.3f), thickness = 1.dp)

                                                TextButton(
                                                    onClick = {
                                                        geminiSetupInitialStep = 1
                                                        showGeminiSetupModal = true
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        ) {
                                                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = ArtPrimaryPurple, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("How do I get a Gemini API key?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ArtPrimaryPurple)
                                                        }
                                                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = ArtPrimaryPurple, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        // -------------------------------------------------------------
                                        // SECTION 2: GEMINI AI MODEL SELECTOR
                                        // -------------------------------------------------------------
                                        Card(
                                            modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, ArtBorderDark),
                                            colors = CardDefaults.cardColors(containerColor = ArtCardBackground)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .background(ArtSecondaryPurple, CircleShape)
                                                                .border(1.dp, ArtPrimaryPurple.copy(alpha = 0.3f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(Icons.Default.Tune, contentDescription = null, tint = ArtPrimaryPurple, modifier = Modifier.size(16.dp))
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("GEMINI AI MODEL", fontWeight = FontWeight.Black, fontSize = 13.sp, color = ArtTextDark)
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = ArtSecondaryPurple
                                                    ) {
                                                        Text(
                                                            text = currentModel.replace("gemini-", "").replace("-preview", "").uppercase(),
                                                            color = ArtPrimaryPurple,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = "Choose the Gemini model for your travel plans.",
                                                    fontSize = 12.sp,
                                                    color = ArtGrayMuted,
                                                    lineHeight = 16.sp
                                                )

                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    val isKeyConnected = userKeyVal.isNotBlank()
                                                    
                                                    viewModel.availableGeminiModels.forEach { modelOption ->
                                                        val isSelected = currentModel == modelOption.id
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .alpha(if (isKeyConnected) 1f else 0.5f)
                                                                .background(
                                                                    if (isSelected) ArtSecondaryPurple else Color.Transparent,
                                                                    RoundedCornerShape(14.dp)
                                                                )
                                                                .border(
                                                                    1.5.dp,
                                                                    if (isSelected) ArtPrimaryPurple else ArtBorderDark.copy(alpha = 0.3f),
                                                                    RoundedCornerShape(14.dp)
                                                                )
                                                                .clickable(enabled = isKeyConnected) {
                                                                    viewModel.selectGeminiModel(modelOption.id)
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            RadioButton(
                                                                selected = isSelected,
                                                                onClick = { viewModel.selectGeminiModel(modelOption.id) },
                                                                colors = RadioButtonDefaults.colors(selectedColor = ArtPrimaryPurple)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = modelOption.displayName,
                                                                        color = ArtTextDark,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 12.sp
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
                                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Text(
                                                                    text = modelOption.description,
                                                                    color = ArtGrayMuted,
                                                                    fontSize = 11.sp,
                                                                    lineHeight = 14.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(28.dp))
                                    }
                                }

                                if (showGeminiSetupModal) {
                                    GeminiSetupDialog(
                                        viewModel = viewModel,
                                        initialStep = geminiSetupInitialStep,
                                        onDismiss = { showGeminiSetupModal = false },
                                        onConnectedSuccess = {
                                            showGeminiSetupModal = false
                                        }
                                    )
                                }

                                if (showRemoveKeyConfirmDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showRemoveKeyConfirmDialog = false },
                                        shape = RoundedCornerShape(20.dp),
                                        containerColor = ArtCardBackground,
                                        title = {
                                            Text(
                                                "Disconnect Gemini API Key?",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp,
                                                color = ArtTextDark
                                            )
                                        },
                                        text = {
                                            Text(
                                                "You will need to connect a Gemini API key again before generating itineraries. Your saved trips will remain intact.",
                                                fontSize = 13.sp,
                                                color = ArtGrayMuted,
                                                lineHeight = 18.sp
                                            )
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    viewModel.disconnectUserApiKey()
                                                    showRemoveKeyConfirmDialog = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Disconnect", fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        },
                                        dismissButton = {
                                            OutlinedButton(
                                                onClick = { showRemoveKeyConfirmDialog = false },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, ArtBorderDark)
                                            ) {
                                                Text("Cancel", fontWeight = FontWeight.Bold, color = ArtTextDark)
                                            }
                                        }
                                    )
                                }

                                if (showGroqKeyDialog) {
                                    var enteredGroqKey by remember { mutableStateOf(groqKeyVal) }
                                    AlertDialog(
                                        onDismissRequest = { showGroqKeyDialog = false },
                                        shape = RoundedCornerShape(20.dp),
                                        containerColor = ArtCardBackground,
                                        title = {
                                            Text(
                                                "TripAsk (Groq) API Key",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp,
                                                color = ArtTextDark
                                            )
                                        },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text(
                                                    "Enter your personal Groq API key (starts with gsk_) to power TripAsk questions.",
                                                    fontSize = 12.sp,
                                                    color = ArtGrayMuted,
                                                    lineHeight = 16.sp
                                                )
                                                OutlinedTextField(
                                                    value = enteredGroqKey,
                                                    onValueChange = { enteredGroqKey = it },
                                                    placeholder = { Text("gsk_...", fontSize = 12.sp, color = ArtGrayMuted) },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    viewModel.saveGroqApiKey(enteredGroqKey.trim())
                                                    showGroqKeyDialog = false
                                                    viewModel.testGroqConnection()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ArtPrimaryPurple),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Save & Test", fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        },
                                        dismissButton = {
                                            OutlinedButton(
                                                onClick = { showGroqKeyDialog = false },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, ArtBorderDark)
                                            ) {
                                                Text("Cancel", fontWeight = FontWeight.Bold, color = ArtTextDark)
                                            }
                                        }
                                    )
                                }
                            }
                            else -> {
                                val context = LocalContext.current

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    contentPadding = PaddingValues(
                                        start = 20.dp,
                                        top = 20.dp,
                                        end = 20.dp,
                                        bottom = 48.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(18.dp)
                                ) {
                                    item {
                                        // 1. ABOUT THE CREATOR
                                        Card(
                                            modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            border = BorderStroke(1.dp, ArtBorderDark),
                                            colors = CardDefaults.cardColors(containerColor = ArtCardBackground)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = ArtPrimaryPurple,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = "ABOUT THE CREATOR",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = ArtPrimaryPurple,
                                                        letterSpacing = 1.2.sp
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .background(ArtSecondaryPurple, CircleShape)
                                                            .border(1.5.dp, ArtPrimaryPurple.copy(alpha = 0.5f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "IJ",
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 17.sp,
                                                            color = ArtPrimaryPurple
                                                        )
                                                    }
                                                    Column {
                                                        Text(
                                                            text = "Ishaan Jadhav",
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ArtTextDark
                                                        )
                                                        Text(
                                                            text = "Creator & Developer of TriplanAI",
                                                            fontSize = 13.sp,
                                                            color = ArtGrayMuted
                                                        )
                                                    }
                                                }

                                                // Responsive social/contact buttons
                                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                                    val isNarrow = maxWidth < 340.dp

                                                    val contacts = listOf(
                                                        SocialContactItem(
                                                            platform = "Instagram",
                                                            handle = "@ishaanj_19",
                                                            uri = "https://instagram.com/ishaanj_19",
                                                            icon = Icons.Default.CameraAlt,
                                                            accentColor = Color(0xFFE1306C),
                                                            bgColor = Color(0xFFFCE7F3)
                                                        ),
                                                        SocialContactItem(
                                                            platform = "GitHub",
                                                            handle = "ishaanj2007",
                                                            uri = "https://github.com/ishaanj2007",
                                                            icon = Icons.Default.Code,
                                                            accentColor = Color(0xFF333333),
                                                            bgColor = ArtSecondaryPurple
                                                        ),
                                                        SocialContactItem(
                                                            platform = "Email",
                                                            handle = "ishaanjadhav64@gmail.com",
                                                            uri = "mailto:ishaanjadhav64@gmail.com",
                                                            icon = Icons.Default.Email,
                                                            accentColor = ArtPrimaryPurple,
                                                            bgColor = Color(0xFFEDE9FE)
                                                        )
                                                    )

                                                    if (isNarrow) {
                                                        Column(
                                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            contacts.forEach { contact ->
                                                                DeveloperContactButton(
                                                                    contact = contact,
                                                                    onClick = {
                                                                        try {
                                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contact.uri))
                                                                            context.startActivity(intent)
                                                                        } catch (e: Exception) {}
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Column(
                                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                DeveloperContactButton(
                                                                    contact = contacts[0],
                                                                    modifier = Modifier.weight(1f),
                                                                    onClick = {
                                                                        try {
                                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contacts[0].uri))
                                                                            context.startActivity(intent)
                                                                        } catch (e: Exception) {}
                                                                    }
                                                                )
                                                                DeveloperContactButton(
                                                                    contact = contacts[1],
                                                                    modifier = Modifier.weight(1f),
                                                                    onClick = {
                                                                        try {
                                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contacts[1].uri))
                                                                            context.startActivity(intent)
                                                                        } catch (e: Exception) {}
                                                                    }
                                                                )
                                                            }
                                                            DeveloperContactButton(
                                                                contact = contacts[2],
                                                                modifier = Modifier.fillMaxWidth(),
                                                                onClick = {
                                                                    try {
                                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contacts[2].uri))
                                                                        context.startActivity(intent)
                                                                    } catch (e: Exception) {}
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        // 2. SUPPORT TRIPLANAI
                                        Card(
                                            modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            border = BorderStroke(1.dp, ArtBorderDark),
                                            colors = CardDefaults.cardColors(containerColor = ArtCardBackground)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                verticalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = "☕",
                                                        fontSize = 15.sp
                                                    )
                                                    Text(
                                                        text = "SUPPORT TRIPLANAI",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = ArtPrimaryPurple,
                                                        letterSpacing = 1.2.sp
                                                    )
                                                }

                                                Text(
                                                    text = "☕ Support TriplanAI",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ArtTextDark
                                                )

                                                Text(
                                                    text = "Enjoying the app?\n\nYour support helps me continue improving TriplanAI, AI features, and future updates.",
                                                    fontSize = 13.sp,
                                                    color = ArtGrayMuted,
                                                    lineHeight = 19.sp
                                                )

                                                // Clean UPI ID badge container
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = ArtSecondaryPurple.copy(alpha = 0.45f),
                                                    border = BorderStroke(1.dp, ArtBorderDark),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "UPI ID",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = ArtGrayMuted,
                                                                letterSpacing = 0.8.sp
                                                            )
                                                            Text(
                                                                text = "jadhavishaan64@okaxis",
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = ArtTextDark
                                                            )
                                                        }

                                                        TextButton(
                                                            onClick = {
                                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                                val clip = android.content.ClipData.newPlainText("UPI ID", "jadhavishaan64@okaxis")
                                                                clipboard.setPrimaryClip(clip)
                                                                android.widget.Toast.makeText(context, "UPI ID copied", android.widget.Toast.LENGTH_SHORT).show()
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            colors = ButtonDefaults.textButtonColors(
                                                                containerColor = ArtPrimaryPurple.copy(alpha = 0.12f),
                                                                contentColor = ArtPrimaryPurple
                                                            )
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ContentCopy,
                                                                contentDescription = "Copy",
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Copy",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }

                                                // Primary Button: Support via UPI
                                                Button(
                                                    onClick = {
                                                        try {
                                                            val upiUri = Uri.parse("upi://pay?pa=jadhavishaan64@okaxis&pn=Ishaan&cu=INR")
                                                            val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
                                                            context.startActivity(upiIntent)
                                                        } catch (e: Exception) {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "No UPI app found. You can copy the UPI ID and pay manually.",
                                                                android.widget.Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(48.dp)
                                                        .testTag("support_upi_button"),
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = ArtPrimaryPurple,
                                                        contentColor = Color.White
                                                    )
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AccountBalance,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Text(
                                                            text = "Support via UPI 🇮🇳",
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                // Secondary Small Action: Copy UPI ID
                                                OutlinedButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        val clip = android.content.ClipData.newPlainText("UPI ID", "jadhavishaan64@okaxis")
                                                        clipboard.setPrimaryClip(clip)
                                                        android.widget.Toast.makeText(context, "UPI ID copied", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(42.dp)
                                                        .testTag("copy_upi_button"),
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = BorderStroke(1.2.dp, ArtBorderDark),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = ArtTextDark
                                                    )
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ContentCopy,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(15.dp),
                                                            tint = ArtPrimaryPurple
                                                        )
                                                        Text(
                                                            text = "Copy UPI ID",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        // 3. ABOUT TRIPLANAI
                                        Card(
                                            modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            border = BorderStroke(1.dp, ArtBorderDark),
                                            colors = CardDefaults.cardColors(containerColor = ArtCardBackground)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Row(
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
                                                        text = "ABOUT TRIPLANAI",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = ArtPrimaryPurple,
                                                        letterSpacing = 1.2.sp
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "TriplanAi",
                                                            fontSize = 22.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ArtTextDark
                                                        )
                                                        Text(
                                                            text = "Smart Travel Planner & Itinerary Architect",
                                                            fontSize = 12.sp,
                                                            color = ArtPrimaryPurple,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = ArtSecondaryPurple,
                                                        border = BorderStroke(1.dp, ArtPrimaryPurple.copy(alpha = 0.3f))
                                                    ) {
                                                        Text(
                                                            text = "Version ${BuildConfig.VERSION_NAME}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ArtPrimaryPurple,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = "TriplanAi is an intelligent travel companion designed to transform wanderlust into well-crafted, realistic journeys. Whether you are venturing solo across historic European alleys, backpacking through Southeast Asia, or organizing a relaxing family retreat, TriplanAi structures every day with logistical precision, cultural insight, and personalized flair.",
                                                    fontSize = 13.sp,
                                                    color = ArtGrayMuted,
                                                    lineHeight = 19.sp
                                                )

                                                Text(
                                                    text = "By combining modern generative AI with smart geographic clustering and customizable travel vibes, TriplanAi eliminates hours of tedious itinerary drafting, research fatigue, and logistical guesswork.",
                                                    fontSize = 13.sp,
                                                    color = ArtGrayMuted,
                                                    lineHeight = 19.sp
                                                )

                                                // CHECK FOR UPDATES SECTION
                                                val appConfigState by viewModel.appConfigState.collectAsState()
                                                val isUpdateAvailable = viewModel.remoteConfigManager.isUpdateAvailable()

                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = ArtCardBackground,
                                                    border = BorderStroke(1.5.dp, ArtBorderDark),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("check_for_updates_card")
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(16.dp),
                                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(38.dp)
                                                                        .background(ArtSecondaryPurple, CircleShape)
                                                                        .border(1.dp, ArtPrimaryPurple.copy(alpha = 0.4f), CircleShape),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = if (isUpdateAvailable) Icons.Default.SystemUpdate else Icons.Default.CheckCircle,
                                                                        contentDescription = null,
                                                                        tint = if (isUpdateAvailable) ArtPrimaryPurple else Color(0xFF16A34A),
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                }
                                                                Column {
                                                                    Text(
                                                                        text = "App Updates",
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 14.sp,
                                                                        color = ArtTextDark
                                                                    )
                                                                    Text(
                                                                        text = if (isUpdateAvailable) "Update v${appConfigState.latestVersion} available" else "Current Version: v${BuildConfig.VERSION_NAME}",
                                                                        fontSize = 11.sp,
                                                                        color = if (isUpdateAvailable) ArtPrimaryPurple else ArtGrayMuted,
                                                                        fontWeight = if (isUpdateAvailable) FontWeight.Bold else FontWeight.Normal
                                                                    )
                                                                }
                                                            }

                                                            if (isUpdateAvailable) {
                                                                Button(
                                                                    onClick = { viewModel.openUpdateDialog() },
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = ArtPrimaryPurple,
                                                                        contentColor = Color.White
                                                                    ),
                                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                                    modifier = Modifier
                                                                        .height(36.dp)
                                                                        .testTag("about_update_now_button")
                                                                ) {
                                                                    Text("Update", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            } else {
                                                                OutlinedButton(
                                                                    onClick = { viewModel.checkForUpdates() },
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    border = BorderStroke(1.2.dp, ArtBorderDark),
                                                                    enabled = !appConfigState.isCheckingForUpdate,
                                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                                    modifier = Modifier
                                                                        .height(36.dp)
                                                                        .testTag("check_for_updates_button")
                                                                ) {
                                                                    if (appConfigState.isCheckingForUpdate) {
                                                                        CircularProgressIndicator(
                                                                            modifier = Modifier.size(14.dp),
                                                                            strokeWidth = 2.dp,
                                                                            color = ArtPrimaryPurple
                                                                        )
                                                                    } else {
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Default.Refresh,
                                                                                contentDescription = null,
                                                                                modifier = Modifier.size(14.dp),
                                                                                tint = ArtTextDark
                                                                            )
                                                                            Text("Check", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ArtTextDark)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        appConfigState.checkStatusFeedback?.let { feedback ->
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = if (isUpdateAvailable) ArtSecondaryPurple.copy(alpha = 0.5f) else Color(0xFFF0FDF4),
                                                                border = BorderStroke(1.dp, if (isUpdateAvailable) ArtPrimaryPurple.copy(alpha = 0.3f) else Color(0xFFBBF7D0)),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Text(
                                                                    text = feedback,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = if (isUpdateAvailable) ArtPrimaryPurple else Color(0xFF15803D),
                                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                HorizontalDivider(color = ArtBorderDark.copy(alpha = 0.4f))

                                                // CORE CAPABILITIES
                                                Text(
                                                    text = "CORE CAPABILITIES",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ArtPrimaryPurple,
                                                    letterSpacing = 1.sp
                                                )

                                                val appFeatures = listOf(
                                                    AppFeatureItem(
                                                        icon = Icons.Default.CalendarMonth,
                                                        title = "Precision Day-by-Day Scheduling",
                                                        description = "Structured morning, afternoon, and evening breakdowns with realistic pacing.",
                                                        tint = ArtPrimaryPurple
                                                    ),
                                                    AppFeatureItem(
                                                        icon = Icons.Default.AccountBalanceWallet,
                                                        title = "Intelligent Budget Estimation",
                                                        description = "Accurate expense breakdowns across lodging, dining, activities, and transit.",
                                                        tint = Color(0xFF10B981)
                                                    ),
                                                    AppFeatureItem(
                                                        icon = Icons.Default.Hotel,
                                                        title = "Curated Stays & Lodging",
                                                        description = "Vetted recommendations ranging from boutique stays to scenic resorts.",
                                                        tint = Color(0xFF3B82F6)
                                                    ),
                                                    AppFeatureItem(
                                                        icon = Icons.Default.Restaurant,
                                                        title = "Authentic Dining & Food Hubs",
                                                        description = "Highlights must-try regional delicacies and iconic local culinary markets.",
                                                        tint = Color(0xFFF59E0B)
                                                    ),
                                                    AppFeatureItem(
                                                        icon = Icons.Default.AltRoute,
                                                        title = "Geographic Route Optimization",
                                                        description = "Clusters nearby attractions to minimize transit time and transit fatigue.",
                                                        tint = Color(0xFF8B5CF6)
                                                    ),
                                                    AppFeatureItem(
                                                        icon = Icons.Default.AutoAwesome,
                                                        title = "TripAsk Instant AI Companion",
                                                        description = "Real-time context-aware answers for packing, weather, and local etiquette.",
                                                        tint = ArtPrimaryPurple
                                                    ),
                                                    AppFeatureItem(
                                                        icon = Icons.Default.Bookmark,
                                                        title = "Offline Local Persistence",
                                                        description = "Securely saved on-device itineraries accessible anytime, even offline.",
                                                        tint = Color(0xFF0284C7)
                                                    ),
                                                    AppFeatureItem(
                                                        icon = Icons.Default.Tune,
                                                        title = "Customizable Assistant Tone",
                                                        description = "Select your guide's personality from Friendly to Witty or Professional.",
                                                        tint = Color(0xFFD97706)
                                                    )
                                                )

                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    appFeatures.forEach { feature ->
                                                        FeatureItemCard(feature = feature)
                                                    }
                                                }

                                                HorizontalDivider(color = ArtBorderDark.copy(alpha = 0.4f))

                                                // HOW IT WORKS
                                                Text(
                                                    text = "HOW TRIPLANAI WORKS",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ArtPrimaryPurple,
                                                    letterSpacing = 1.sp
                                                )

                                                val workflowSteps = listOf(
                                                    Triple("1", "Set Destination & Duration", "Input any global city, region, or hidden gem along with your trip length."),
                                                    Triple("2", "Customize Travel Style", "Specify your budget tier, travel companions, transportation mode, and preferred vibe."),
                                                    Triple("3", "AI Generates Itinerary", "Our smart planning engine constructs an hour-by-hour roadmap with logistical cohesion."),
                                                    Triple("4", "Explore, Ask & Save", "Interact with your itinerary, ask TripAsk questions, and save trips for offline access.")
                                                )

                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    workflowSteps.forEach { (num, title, desc) ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(ArtSecondaryPurple.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                                                .border(1.dp, ArtBorderDark.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                                                .padding(12.dp),
                                                            verticalAlignment = Alignment.Top,
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(26.dp)
                                                                    .background(ArtPrimaryPurple, CircleShape),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = num,
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 12.sp
                                                                )
                                                            }
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = title,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 13.sp,
                                                                    color = ArtTextDark
                                                                )
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text(
                                                                    text = desc,
                                                                    fontSize = 11.sp,
                                                                    color = ArtGrayMuted,
                                                                    lineHeight = 15.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                HorizontalDivider(color = ArtBorderDark.copy(alpha = 0.4f))

                                                // PRIVACY & LOCAL-FIRST
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFFF0FDF4), RoundedCornerShape(14.dp))
                                                        .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(14.dp))
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .background(Color(0xFFDCFCE7), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Shield,
                                                            contentDescription = null,
                                                            tint = Color(0xFF166534),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "Privacy & Local-First Storage",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = Color(0xFF166534)
                                                        )
                                                        Text(
                                                            text = "Your saved trips, custom API keys, and preferences are stored locally on your device using Android Room. We do not track or sell your personal travel logs.",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF15803D),
                                                            lineHeight = 15.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        // 3. BUILT WITH
                                        Card(
                                            modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            border = BorderStroke(1.dp, ArtBorderDark),
                                            colors = CardDefaults.cardColors(containerColor = ArtCardBackground)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                verticalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Layers,
                                                        contentDescription = null,
                                                        tint = ArtPrimaryPurple,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = "BUILT WITH",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = ArtPrimaryPurple,
                                                        letterSpacing = 1.2.sp
                                                    )
                                                }

                                                val techStack = listOf(
                                                    TechItem("Google Gemini", "Itinerary generation", Icons.Default.AutoAwesome, ArtPrimaryPurple),
                                                    TechItem("Groq + Llama 3.1", "TripAsk", Icons.Default.Bolt, Color(0xFFF59E0B)),
                                                    TechItem("Jetpack Compose", "Android UI", Icons.Default.Widgets, Color(0xFF10B981)),
                                                    TechItem("Room", "Local storage", Icons.Default.Storage, Color(0xFF3B82F6))
                                                )

                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    techStack.forEach { tech ->
                                                        TechStackRow(tech = tech)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        // 4. TRAVEL INFORMATION DISCLAIMER
                                        Card(
                                            modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(Color(0xFFFEF3C7), CircleShape)
                                                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = null,
                                                        tint = Color(0xFFD97706),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = "TRAVEL INFORMATION",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFB45309),
                                                        letterSpacing = 0.8.sp
                                                    )
                                                    Text(
                                                        text = "Prices, availability, routes, images, and recommendations generated by TriplanAi may change or be inaccurate. Always verify important travel information with official sources before travelling.",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF92400E),
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        // 5. FOOTER
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .widthIn(max = 680.dp)
                                                .padding(top = 4.dp, bottom = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = "TriplanAi",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = ArtTextDark
                                            )
                                            Text(
                                                text = "Version ${BuildConfig.VERSION_NAME}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = ArtPrimaryPurple
                                            )
                                            Text(
                                                text = "Built with curiosity & code.",
                                                fontSize = 11.sp,
                                                color = ArtGrayMuted
                                            )
                                            Text(
                                                text = "© 2026 Ishaan Jadhav",
                                                fontSize = 10.sp,
                                                color = ArtGrayMuted.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(28.dp))
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

// -------------------------------------------------------------------------
// SUPPORT & ABOUT HELPER MODELS & COMPOSABLES
// -------------------------------------------------------------------------

private data class SocialContactItem(
    val platform: String,
    val handle: String,
    val uri: String,
    val icon: ImageVector,
    val accentColor: Color,
    val bgColor: Color
)

@Composable
private fun DeveloperContactButton(
    contact: SocialContactItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = ArtSecondaryPurple.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, ArtBorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(contact.bgColor, CircleShape)
                    .border(1.dp, contact.accentColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = contact.icon,
                    contentDescription = contact.platform,
                    tint = contact.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.platform,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArtGrayMuted
                )
                Text(
                    text = contact.handle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArtTextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open",
                tint = ArtPrimaryPurple.copy(alpha = 0.7f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private data class AppFeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val tint: Color? = null
)

@Composable
private fun FeatureItemCard(
    feature: AppFeatureItem,
    modifier: Modifier = Modifier
) {
    val effectiveTint = feature.tint ?: ArtPrimaryPurple
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ArtSecondaryPurple.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, ArtBorderDark.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(effectiveTint.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, effectiveTint.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.title,
                    tint = effectiveTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArtTextDark
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = feature.description,
                    fontSize = 11.sp,
                    color = ArtGrayMuted,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

private data class TechItem(
    val name: String,
    val purpose: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
private fun TechStackRow(tech: TechItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ArtSecondaryPurple.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .border(1.dp, ArtBorderDark.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(tech.accentColor.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, tech.accentColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tech.icon,
                    contentDescription = null,
                    tint = tech.accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = tech.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = ArtTextDark
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ArtCardBackground,
            border = BorderStroke(1.dp, ArtBorderDark.copy(alpha = 0.6f))
        ) {
            Text(
                text = tech.purpose,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ArtPrimaryPurple,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

// -------------------------------------------------------------------------
// HELPER COMPOSABLES & MODELS
// -------------------------------------------------------------------------

@Composable
fun HeroTravelIllustration(
    modifier: Modifier = Modifier
) {
    val primaryColor = ArtPrimaryPurple
    val lavenderColor = ArtSoftLavender
    val secondaryColor = ArtSecondaryPurple

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Sun
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFDE047), Color(0xFFF472B6)),
                center = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.35f),
                radius = h * 0.45f
            ),
            center = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.35f),
            radius = w * 0.25f
        )

        // Distant mountains
        val path1 = Path().apply {
            moveTo(0f, h)
            lineTo(w * 0.18f, h * 0.55f)
            lineTo(w * 0.42f, h * 0.8f)
            lineTo(w * 0.72f, h * 0.38f)
            lineTo(w, h * 0.75f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = path1,
            color = lavenderColor.copy(alpha = 0.8f)
        )

        // Foreground mountains
        val path2 = Path().apply {
            moveTo(w * 0.12f, h)
            lineTo(w * 0.45f, h * 0.35f)
            lineTo(w * 0.68f, h * 0.65f)
            lineTo(w * 0.88f, h * 0.45f)
            lineTo(w, h * 0.58f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = path2,
            color = primaryColor.copy(alpha = 0.65f)
        )

        // Peak highlight
        val pathPeak = Path().apply {
            moveTo(w * 0.45f, h * 0.35f)
            lineTo(w * 0.39f, h * 0.48f)
            lineTo(w * 0.45f, h * 0.45f)
            lineTo(w * 0.51f, h * 0.5f)
            close()
        }
        drawPath(
            path = pathPeak,
            color = Color.White.copy(alpha = 0.65f)
        )

        // Dotted flight route line
        val minDim = minOf(w, h)
        val strokeWidth = (minDim * 0.04f).coerceIn(2f, 4f)
        val stroke = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 2.2f, strokeWidth * 2.2f), 0f)
        )
        val routePath = Path().apply {
            moveTo(w * 0.25f, h * 0.85f)
            quadraticTo(w * 0.6f, h * 0.68f, w * 0.84f, h * 0.32f)
        }
        drawPath(
            path = routePath,
            color = primaryColor,
            style = stroke
        )

        // Pin head
        val outerRadius = (minDim * 0.12f).coerceIn(6f, 13f)
        val innerRadius = outerRadius * 0.42f
        drawCircle(
            color = primaryColor,
            radius = outerRadius,
            center = androidx.compose.ui.geometry.Offset(w * 0.84f, h * 0.32f)
        )
        drawCircle(
            color = Color.White,
            radius = innerRadius,
            center = androidx.compose.ui.geometry.Offset(w * 0.84f, h * 0.32f)
        )
    }
}

@Composable
fun FeaturePill(
    icon: ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ArtPrimaryPurple,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ArtTextDark
        )
    }
}

@Composable
fun TrendingDestinationCard(
    dest: TrendingDest,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .height(210.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
        border = BorderStroke(1.dp, ArtBorderDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
            ) {
                AsyncImage(
                    model = getDestinationImageUrl(dest.name),
                    contentDescription = dest.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .size(32.dp)
                        .background(dest.badgeBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = null,
                        tint = ArtTextDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = dest.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArtTextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dest.vibe,
                        fontSize = 11.sp,
                        color = ArtGrayMuted,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(dest.btnBg, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Select ${dest.name}",
                            tint = ArtTextDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedTripRowCard(
    trip: TripEntity,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clickable(onClick = onView),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
        border = BorderStroke(1.dp, ArtBorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = getDestinationImageUrl(trip.destination),
                    contentDescription = trip.destination,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = ArtPrimaryPurple,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = trip.destination,
                            color = ArtTextDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${trip.durationDays} Days • ${trip.travelerGroup}",
                        color = ArtGrayMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ArtSecondaryPurple
                    ) {
                        Text(
                            text = "Style • ${trip.travelStyle}",
                            color = ArtPrimaryPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = ArtBorderDark.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onView,
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ArtPrimaryPurple, contentColor = Color.White),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("View Trip", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Bouncing button component
@Composable
fun BounceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "bounceScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.scale(scale),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFFE2E8F0),
            disabledContentColor = Color(0xFF94A3B8)
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        content = content
    )
}

fun getDestinationImageUrl(destinationName: String): String {
    val name = destinationName.lowercase().trim()
    return when {
        name.contains("goa") -> "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=500&auto=format&fit=crop&q=80"
        name.contains("japan") || name.contains("tokyo") -> "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=500&auto=format&fit=crop&q=80"
        name.contains("manali") -> "https://images.unsplash.com/photo-1626621341517-bbf3d9990a23?w=500&auto=format&fit=crop&q=80"
        name.contains("paris") || name.contains("france") -> "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=500&auto=format&fit=crop&q=80"
        name.contains("ladakh") -> "https://images.unsplash.com/photo-1581793745862-99fde7fa73d2?w=500&auto=format&fit=crop&q=80"
        name.contains("kerala") -> "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=500&auto=format&fit=crop&q=80"
        name.contains("bali") -> "https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=500&auto=format&fit=crop&q=80"
        name.contains("singapore") -> "https://images.unsplash.com/photo-1525625293386-3f8f99389edd?w=500&auto=format&fit=crop&q=80"
        name.contains("thailand") || name.contains("bangkok") -> "https://images.unsplash.com/photo-1508009603885-50cf7c579365?w=500&auto=format&fit=crop&q=80"
        name.contains("rajasthan") || name.contains("jaipur") || name.contains("udaipur") -> "https://images.unsplash.com/photo-1477587458883-47145ed94245?w=500&auto=format&fit=crop&q=80"
        name.contains("dubai") -> "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=500&auto=format&fit=crop&q=80"
        name.contains("maldives") -> "https://images.unsplash.com/photo-1514282401047-d79a71a590e8?w=500&auto=format&fit=crop&q=80"
        name.contains("kashmir") -> "https://images.unsplash.com/photo-1595815771614-ade9d652a65d?w=500&auto=format&fit=crop&q=80"
        name.contains("agra") -> "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=500&auto=format&fit=crop&q=80"
        name.contains("narmadapuram") -> "https://images.unsplash.com/photo-1600100397608-f010e423b961?w=500&auto=format&fit=crop&q=80"
        else -> "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=500&auto=format&fit=crop&q=80"
    }
}

data class TrendingDest(
    val name: String,
    val vibe: String,
    val icon: ImageVector,
    val badgeBg: Color,
    val btnBg: Color
)
