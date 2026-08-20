package com.example.repository

import com.example.BuildConfig
import com.example.data.local.TripDao
import com.example.data.local.TripEntity
import com.example.data.model.TripPlan
import com.example.data.remote.Content
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.Part
import com.example.data.remote.RetrofitClient
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TripRepository(private val tripDao: TripDao) {

    val allTrips: Flow<List<TripEntity>> = tripDao.getAllTrips()

    suspend fun getTripById(id: Int): TripEntity? {
        return tripDao.getTripById(id)
    }

    suspend fun saveTrip(destination: String, durationDays: Int, travelerGroup: String, travelStyle: String, plan: TripPlan): Long {
        val moshi: Moshi = RetrofitClient.moshiInstance
        val adapter = moshi.adapter(TripPlan::class.java)
        val json = adapter.toJson(plan)
        
        // Prevent duplicate entries by finding if this destination/duration is already saved
        val existing = tripDao.findTripByDestinationAndDuration(destination, durationDays)
        val entity = TripEntity(
            id = existing?.id ?: 0,
            destination = destination,
            durationDays = durationDays,
            travelerGroup = travelerGroup,
            travelStyle = travelStyle,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            tripPlanJson = json
        )
        return tripDao.insertTrip(entity)
    }

    suspend fun deleteTrip(id: Int) {
        tripDao.deleteTripById(id)
    }

    suspend fun generateTripPlan(
        destination: String,
        fromLocation: String,
        travelerGroup: String,
        durationDays: Int,
        totalBudget: String,
        transportation: String,
        travelStyle: String,
        pace: String,
        specialRequirements: String,
        model: String,
        userApiKey: String? = null,
        personality: String = "Friendly"
    ): TripPlan = withContext(Dispatchers.IO) {
        val apiKey = if (!userApiKey.isNullOrBlank()) userApiKey else throw IllegalStateException("NO_GEMINI_KEY")

        val personalityInstruction = when (personality) {
            "Professional" -> """
                TONE / PERSONALITY: Professional.
                Write in a formal, informative, and straightforward manner.
                Provide structured, professional, and clear travel recommendations, budget assessments, and suggestions. No jokes, sarcasm, or slang.
            """.trimIndent()
            "Friendly" -> """
                TONE / PERSONALITY: Friendly.
                Write in a casual, conversational, and warm tone.
                Feel like a close travel buddy who is excited to help plan this trip. Use friendly expressions, emojis, and highly welcoming suggestions.
            """.trimIndent()
            "Funny" -> """
                TONE / PERSONALITY: Funny.
                Write in a playful, humorous, and witty tone.
                Add funny comments, light jokes, witty observations, and playful banter throughout the itinerary descriptions, budget breakdowns, and tips. Keep the travel information accurate but highly entertaining.
            """.trimIndent()
            "Roast Me" -> """
                TONE / PERSONALITY: Roast Me (Savage and entertaining!).
                Be extremely sarcastic, sassy, and hilariously brutal. You must roast unrealistic budgets, overpacked plans, lazy styles, or questionable travel choices in a hilarious and entertaining way (e.g., 'You're trying to visit 14 places in one day. Are you travelling or speedrunning Goa?', or 'Luxury hotels with a backpacker budget? Bold strategy.').
                Roast their travel preferences, budget, pace, and choices inside the 'explanation' fields, 'description' of hotels/restaurants, 'foodSafetyTips', 'localTips', and 'thingsToAvoid'.
                IMPORTANT: The actual names, routes, prices, and locations must remain 100% accurate, useful, and high quality. Only roast their choices and writing tone in a playful, savage, and highly entertaining way.
            """.trimIndent()
            else -> ""
        }

        val prompt = """
            Create a highly detailed, optimized, and personalized day-by-day travel itinerary for a trip to: $destination.
            Here are the details:
            - Travelling from: $fromLocation
            - Traveler group size / configuration: $travelerGroup
            - Duration of stay: $durationDays days
            - Total Budget context: $totalBudget
            - Preferred transportation: $transportation
            - Travel style / primary interest: $travelStyle
            - Pace of the trip: $pace (fast-paced or relaxed)
            - Special requirements / diet: $specialRequirements
            - Writing style / tone: $personality
            
            IMPORTANT WRITING STYLE RULE:
            You MUST write all explanations, descriptions, tips, comments, and recommendations strictly adhering to the selected personality:
            $personalityInstruction

            Think like an expert travel guide. Group nearby attractions logically to minimize travel time.
            Estimate all costs in estimated ranges instead of exact fake figures.
            Use the currency appropriate for the destination or general user's origin (INR ₹ or USD ${'$'} depending on the destination, Goa/Manali -> ₹, Paris/Japan -> ${'$'}/Yen/Euro but provide numeric ranges clearly).
            Generate AT LEAST 3 highly diverse hotel options in the accommodationGuide suggestions (ranging from budget/hostel to mid-range and premium luxury), and always populate the 'directLink' field with a highly accurate Google Search link query for that specific hotel in that destination (e.g., https://www.google.com/search?q=Hotel+Name+Destination).

            You MUST strictly return your answer as a single, valid JSON object fitting the following structure:
            {
              "destination": "Goa",
              "durationDays": 3,
              "travelerGroup": "Couple",
              "travelStyle": "Relaxation",
              "estimatedTotalBudget": "₹15,000 - ₹25,000",
              "budgetBreakdown": [
                {
                  "category": "Hotels",
                  "costRange": "₹3,000 - ₹5,000 per night",
                  "explanation": "Mid-range boutique beach resort near Candolim."
                }
              ],
              "accommodationGuide": {
                "bestAreaToStay": "North Goa (Candolim/Calangute)",
                "whyRecommended": "Best for beach access, vibrant dining, and ease of walking.",
                "suggestions": [
                  {
                    "name": "Boutique Beach Resort",
                    "priceRange": "₹4,000/night",
                    "suitableFor": "Couples & Families",
                    "description": "Lovely garden resort with pool access and beach path.",
                    "directLink": "https://www.google.com/search?q=Boutique+Beach+Resort+Goa"
                  },
                  {
                    "name": "Hostel Backpackers Goa",
                    "priceRange": "₹800/night",
                    "suitableFor": "Solo & Budget Travellers",
                    "description": "Vibrant budget hostel near the beach, ideal for meeting other travelers.",
                    "directLink": "https://www.google.com/search?q=Hostel+Backpackers+Goa"
                  },
                  {
                    "name": "The Grand Hyatt Goa",
                    "priceRange": "₹15,000/night",
                    "suitableFor": "Luxury Travellers",
                    "description": "Premium 5-star resort overlooking the bay with world-class amenities.",
                    "directLink": "https://www.google.com/search?q=Grand+Hyatt+Goa"
                  }
                ]
              },
              "foodGuide": {
                "mustTryDishes": ["Goan Fish Curry", "Bebinca dessert", "Rava Fried Fish"],
                "recommendedRestaurants": [
                  {
                    "name": "Fisherman's Wharf",
                    "type": "Luxury / Fine Dining",
                    "signatureDish": "Seafood Platter",
                    "description": "Riverside dining experience with authentic live music and Goan cuisine."
                  }
                ],
                "foodSafetyTips": "Stick to filtered water and eat seafood from well-reviewed, busy spots."
              },
              "routes": {
                "recommendedRoute": "Fly into Mopa Airport, then take a pre-paid taxi to North Goa.",
                "options": [
                  {
                    "name": "Cheapest",
                    "transportMode": "Train",
                    "duration": "12 hours",
                    "estimatedFare": "₹800",
                    "routeDetails": "Sleeper class from Mumbai to Madgaon station."
                  }
                ]
              },
              "days": [
                {
                  "dayNumber": 1,
                  "theme": "Beach Vibes & Sunset",
                  "activities": [
                    {
                      "timeOfDay": "Morning",
                      "title": "Check-in & Candolim Beach",
                      "description": "Arrive at hotel, settle down, and walk to Candolim Beach for a breezy stroll.",
                      "duration": "2 hours",
                      "approximateCost": "Free",
                      "tips": "Carry sunscreen and beach slippers."
                    }
                  ]
                }
              ],
              "packingList": ["Light cotton clothing", "Sunscreen SPF 50+", "Swimwear", "Insect repellent"],
              "localTips": ["Rent a scooty for ₹400/day for cheap transport", "Bargain at local markets", "Taxis don't run on meters, negotiate first"],
              "thingsToAvoid": ["Avoid swimming in beaches with high tides / red flags", "Do not rent cars without checking for damage first"],
              "bestPhotoSpots": ["Fort Aguada at sunset", "Fontainhas colorful Latin quarter", "Chapora Fort"],
              "moneySavingTips": ["Use public shuttle buses from airport", "Eat at local beach shacks rather than heavy fine-diners"],
              "emergencyInfo": "Goa Tourism Police: +91 832 242 8224. Nearest Hospital: Manipal Hospital, Panaji."
            }

            Make sure all days (1 to $durationDays) are generated inside the 'days' array list.
            DO NOT output any markdown tags like ```json or ``` surrounding your response. Return ONLY raw valid JSON text.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.5f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are a playful, highly skilled, and detailed AI travel planner. You generate professional-grade, beautifully formatted day-by-day travel plans in raw JSON format only. Never return explanations outside the JSON block. Your tone is energetic, knowledgeable, and excited."))
            )
        )

        val response = RetrofitClient.service.generateContent(model, apiKey, request)
        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("No travel plan received from AI.")

        // Parse response
        val moshi = RetrofitClient.moshiInstance
        val adapter = moshi.adapter(TripPlan::class.java)
        
        // Clean markdown backticks and extract the first logical JSON object
        var cleanedJson = responseText.trim()
        val firstBrace = cleanedJson.indexOf('{')
        val lastBrace = cleanedJson.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            cleanedJson = cleanedJson.substring(firstBrace, lastBrace + 1)
        } else {
            cleanedJson = cleanedJson
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        }

        try {
            adapter.fromJson(cleanedJson) ?: throw Exception("Failed to parse AI response.")
        } catch (e: Exception) {
            throw Exception("AI formatting error: The plan was generated but failed to parse into structured cards. Please retry!", e)
        }
    }
}
