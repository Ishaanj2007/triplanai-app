package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TripPlan(
    val destination: String = "",
    val durationDays: Int = 1,
    val travelerGroup: String = "Solo",
    val travelStyle: String = "Relaxation",
    val estimatedTotalBudget: String = "",
    val budgetBreakdown: List<BudgetCategory> = emptyList(),
    val accommodationGuide: AccommodationGuide = AccommodationGuide(),
    val foodGuide: FoodGuide = FoodGuide(),
    val routes: RoutePlanning = RoutePlanning(),
    val days: List<DayPlan> = emptyList(),
    val packingList: List<String> = emptyList(),
    val localTips: List<String> = emptyList(),
    val thingsToAvoid: List<String> = emptyList(),
    val bestPhotoSpots: List<String> = emptyList(),
    val moneySavingTips: List<String> = emptyList(),
    val emergencyInfo: String = ""
)

@JsonClass(generateAdapter = true)
data class BudgetCategory(
    val category: String = "", // "Hotels", "Food", "Activities", etc.
    val costRange: String = "", // e.g. "₹2,000 - ₹4,000 per night"
    val explanation: String = ""
)

@JsonClass(generateAdapter = true)
data class AccommodationGuide(
    val bestAreaToStay: String = "",
    val whyRecommended: String = "",
    val suggestions: List<HotelSuggestion> = emptyList()
)

@JsonClass(generateAdapter = true)
data class HotelSuggestion(
    val name: String = "",
    val priceRange: String = "",
    val suitableFor: String = "", // e.g. "Couples, Luxury"
    val description: String = "",
    val directLink: String = ""
)

@JsonClass(generateAdapter = true)
data class FoodGuide(
    val mustTryDishes: List<String> = emptyList(),
    val recommendedRestaurants: List<RestaurantSuggestion> = emptyList(),
    val foodSafetyTips: String = ""
)

@JsonClass(generateAdapter = true)
data class RestaurantSuggestion(
    val name: String = "",
    val type: String = "", // "Budget", "Luxury", "Hidden Cafe", "Street Food"
    val signatureDish: String = "",
    val description: String = ""
)

@JsonClass(generateAdapter = true)
data class RoutePlanning(
    val recommendedRoute: String = "",
    val options: List<RouteOption> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RouteOption(
    val name: String = "", // e.g., "Cheapest", "Fastest", "Most Comfortable"
    val transportMode: String = "",
    val duration: String = "",
    val estimatedFare: String = "",
    val routeDetails: String = ""
)

@JsonClass(generateAdapter = true)
data class DayPlan(
    val dayNumber: Int = 1,
    val theme: String = "", // e.g. "Beach Exploration"
    val activities: List<ActivityPlan> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ActivityPlan(
    val timeOfDay: String = "", // "Morning", "Afternoon", "Evening"
    val title: String = "",
    val description: String = "",
    val duration: String = "",
    val approximateCost: String = "",
    val tips: String = ""
)
