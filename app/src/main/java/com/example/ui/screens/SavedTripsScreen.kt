package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.TripEntity
import com.example.ui.theme.*
import com.example.viewmodel.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedTripsScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToItinerary: () -> Unit
) {
    val savedTrips by viewModel.savedTrips.collectAsState()
    var tripToDeleteId by remember { mutableStateOf<Int?>(null) }

    if (tripToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { tripToDeleteId = null },
            title = { Text("Delete Saved Trip?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove this itinerary from your saved trips?") },
            confirmButton = {
                Button(
                    onClick = {
                        tripToDeleteId?.let { id ->
                            viewModel.deleteSavedTrip(id)
                        }
                        tripToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToDeleteId = null }) {
                    Text("Cancel", color = ArtTextDark)
                }
            },
            containerColor = ArtCardBackground
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArtBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .background(ArtCardBackground, CircleShape)
                        .border(1.dp, ArtBorderDark, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = ArtTextDark
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Saved Trips",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArtTextDark
                    )
                    Text(
                        text = "${savedTrips.size} adventures saved",
                        fontSize = 12.sp,
                        color = ArtGrayMuted
                    )
                }
            }

            HorizontalDivider(color = ArtBorderDark.copy(alpha = 0.2f))

            if (savedTrips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
                        border = BorderStroke(1.dp, ArtBorderDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(32.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = ArtPrimaryPurple,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No Saved Trips Yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ArtTextDark
                            )
                            Text(
                                text = "Create and save your travel itineraries on the home screen to access them anytime offline.",
                                fontSize = 13.sp,
                                color = ArtGrayMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Button(
                                onClick = onNavigateBack,
                                colors = ButtonDefaults.buttonColors(containerColor = ArtPrimaryPurple),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Plan a Trip", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(savedTrips, key = { it.id }) { trip ->
                        SavedTripFullCard(
                            trip = trip,
                            onView = {
                                viewModel.viewSavedTrip(trip)
                                onNavigateToItinerary()
                            },
                            onDelete = {
                                tripToDeleteId = trip.id
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedTripFullCard(
    trip: TripEntity,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_trip_card_${trip.destination}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ArtCardBackground),
        border = BorderStroke(1.dp, ArtBorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        .size(90.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = ArtPrimaryPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = trip.destination,
                            color = ArtTextDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${trip.durationDays} Days • ${trip.travelerGroup}",
                        color = ArtGrayMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = ArtBorderDark.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

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
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onView,
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ArtPrimaryPurple, contentColor = Color.White),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("View Trip", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
