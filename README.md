# TriplanAI

> **Plan less. Explore more.**

TriplanAI is an AI-powered travel planning application designed to turn a simple destination idea into a personalized, structured travel plan.

Instead of manually searching for places, estimating budgets, deciding how many days are needed, finding suitable stays, and figuring out routes, TriplanAI brings the planning process together in one place.

Enter your destination, choose your trip preferences, and let TriplanAI build an itinerary around **your time, budget, travel style, and preferences**.

---

## ✨ What is TriplanAI?

Planning a trip often means jumping between multiple apps and websites:

* Where should I go?
* How many days do I need?
* How much will the trip cost?
* Where should I stay?
* What should I eat?
* Which places are actually worth visiting?
* What's the best route?
* Should I travel by train, flight, bus, or car?
* Is my budget realistic?

TriplanAI is designed to answer these questions through a single travel-planning experience.

The application combines structured trip preferences with AI-generated recommendations to create a practical itinerary tailored to the traveller.

---

## 🧭 Core Features

### 🤖 AI-Powered Itinerary Generation

Generate a personalized day-by-day itinerary using Google Gemini.

TriplanAI considers factors such as:

* Destination
* Trip duration
* Number of travellers
* Travel style
* Budget
* Transportation preference
* Traveller type
* Accommodation preferences

The result is a structured travel plan rather than a generic list of recommendations.

---

### 💰 Budget Planning

Choose the type of trip you want:

* Budget Friendly
* Mid Range
* Luxury

The generated itinerary provides estimated spending across different categories such as:

* Accommodation
* Food
* Transportation
* Activities
* Entry fees
* Miscellaneous expenses

The goal is to help travellers understand the approximate cost before committing to a trip.

---

### 🗓️ Day-by-Day Trip Planning

Trips are organized into individual days with activities arranged into a practical sequence.

Each day can include:

* Places to visit
* Activities
* Food suggestions
* Estimated costs
* Transportation
* Timing suggestions
* Useful travel notes

This makes the generated itinerary easier to follow during an actual trip.

---

### 🏨 Stay & Food Suggestions

TriplanAI can provide accommodation and food recommendations based on the selected destination and budget.

The recommendations are intended to help users understand where they could stay, eat, and explore without manually researching everything from scratch.

> **Note:** AI-generated recommendations, prices, availability, and travel information should always be verified before travelling.

---

### 🧭 Route Planning

The app provides simplified route information for the generated trip.

Users can explore:

* Starting location
* Destination
* Transportation options
* Approximate travel routes
* Alternative route plans

The application is designed to provide a simplified planning view while allowing users to continue navigation through external map services when appropriate.

---

### 💾 Saved Trips

Generated itineraries can be saved so users don't need to regenerate the same trip repeatedly.

Saved trips can be revisited later, helping reduce unnecessary AI generations and making previously planned trips easier to access.

---

## ✨ TripAsk

TriplanAI also includes **TripAsk**, a lightweight AI assistant specifically designed for the currently generated itinerary.

TripAsk is intentionally **not a general-purpose chatbot**.

It focuses on short, useful travel questions such as:

> "What should I pack?"

> "Is this route good?"

> "Can I save money?"

> "What food should I try?"

> "Is Day 2 too hectic?"

Responses are intentionally short and direct so TripAsk feels like a quick travel utility rather than another ChatGPT-style conversation.

### TripAsk Technology

TripAsk uses:

**Groq + Llama 3.1 8B Instant**

The model is optimized for quick contextual questions and short responses.

The full itinerary generation and TripAsk systems are intentionally separated:

```text
Full Itinerary
      ↓
Google Gemini

TripAsk
      ↓
Groq
      ↓
Llama 3.1 8B Instant
```

---

## 🎨 Design

TriplanAI uses a modern pastel visual language with a soft neobrutalist influence.

The design focuses on:

* Clear visual hierarchy
* Rounded cards
* Pastel accents
* Expressive typography
* Compact travel information
* Responsive layouts
* Minimal visual clutter

The interface is designed to work across different phone sizes and adapt to larger screens where possible.

---

## 📱 Application Flow

```text
Open TriplanAI
      ↓
Enter Destination
      ↓
Select Trip Preferences
      ↓
Choose Duration
      ↓
Set Budget
      ↓
Choose Traveller Type
      ↓
Choose Transportation
      ↓
Generate Itinerary
      ↓
Explore Trip
 ┌────┼──────┬──────┐
 ↓    ↓      ↓      ↓
Overview Day Plan Hotels Routes
      ↓
   TripAsk
      ↓
 Ask questions about the current trip
      ↓
 Save Trip
```

---

## 🧠 AI Architecture

TriplanAI intentionally uses different AI systems for different purposes.

### Google Gemini

Used for:

* Full itinerary generation
* Trip planning
* Budget planning
* Day-by-day recommendations
* Travel suggestions
* Route planning context

### Groq + Llama 3.1 8B Instant

Used exclusively for:

* TripAsk
* Short travel questions
* Contextual suggestions
* Lightweight reasoning
* Quick travel assistance

This separation keeps the lightweight assistant fast without replacing the main itinerary-generation system.

---

## 🛠️ Technology Stack

Current project technologies include:

* **Android**
* **Jetpack Compose**
* **Kotlin**
* **Google Gemini**
* **Groq**
* **Llama 3.1 8B Instant**
* **Room / Local Storage**
* **GitHub**

The exact implementation may evolve as the project develops.

---

## 🔐 API & Privacy

TriplanAI may use external AI services to generate travel-related content.

API credentials should **never be committed to this repository**.

For development or personal builds, API configuration may be provided through appropriate local configuration mechanisms.

Never place production API keys directly inside the source code or APK.

---

## ⚠️ Important Disclaimer

TriplanAI uses AI to generate travel information and recommendations.

AI-generated information may contain inaccuracies or become outdated.

This includes:

* Hotel information
* Prices
* Routes
* Travel times
* Weather-related information
* Attraction availability
* Opening hours
* Transportation information
* Recommendations

Always verify important information through official sources before making travel or financial decisions.

TriplanAI does not guarantee the accuracy, availability, pricing, or suitability of any recommendation.

---

## 🚧 Project Status

**Current Version: `2.0.1`**

TriplanAI is currently an evolving personal/development project.

The application is being developed iteratively, with a focus on:

* Better responsive UI
* Improved itinerary generation
* More useful route planning
* Better travel recommendations
* TripAsk improvements
* Saved-trip experience
* More reliable AI integrations
* Real-world travel data integration

Some planned features may not yet be implemented.

---

## 🗺️ Roadmap

Planned improvements include:

* [ ] Better real-world place and hotel imagery
* [ ] Improved map and route visualization
* [ ] More reliable travel information
* [ ] Improved responsive layouts
* [ ] Better saved-trip management
* [ ] Improved TripAsk contextual understanding
* [ ] Calendar and trip-date improvements
* [ ] AI personality / itinerary tone options
* [ ] Travel reality checks
* [ ] AI Roast Mode
* [ ] Trip Mode for active travel
* [ ] More advanced local/offline AI experimentation

---

## 🎯 Project Philosophy

TriplanAI isn't meant to replace every travel application.

The idea is much simpler:

> **Give the app a destination and your preferences, and get a practical starting point for your trip.**

The focus is on reducing the friction between:

**"I want to go somewhere."**

and

**"Here's how I can actually make that trip happen."**

---

## 👨‍💻 Developer

**Ishaan Jadhav**

Independent developer and creator of TriplanAI.

### Connect

* Instagram: `@ishaanj_19`
* GitHub: `@ishaanj2007`
* WhatsApp: `@ishaan_jadhav`
* Email: `ishaanjadhav64@gmail.com`

---

## 📄 License

This project is licensed under the **MIT License**.

See [`LICENSE`](LICENSE) for details.

---

## ⭐ About This Project

TriplanAI started as an idea to make travel planning less fragmented and more personal.

Instead of searching across multiple platforms for destinations, budgets, hotels, routes, food, and activities, the goal is to create one simple experience where AI helps organize the entire trip.

It is a project built around experimentation, learning, and turning an idea into something people can actually use.

**Built with curiosity & code.**
