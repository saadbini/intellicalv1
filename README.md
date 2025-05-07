# IntelliCal - Smart Calendar App

IntelliCal is a feature-rich calendar application for Android that combines traditional event management with intelligent natural language processing.

## Features

### Core Calendar Features
- Clean, intuitive interface for managing events and tasks
- Multiple views: month, week, day, and agenda
- Event reminders and notifications
- Recurring events support
- Color-coded event categories
- CalDAV synchronization
- Widget support for home screen

### Natural Language Event Creation
IntelliCal features a powerful natural language event creation system that allows you to create events using plain English (or other supported languages). Simply describe your event, and IntelliCal will automatically parse the details:

- **Voice or Text Input**: Speak or type your event details naturally
- **Intelligent Parsing**: Automatically extracts event title, date, time, location, and more using Named Entity Recognition (NER)
- **Context-Aware**: Uses your current calendar view as context for relative dates (e.g., "tomorrow", "next week")
- **Confirmation Flow**: Review extracted details before creating the event

Examples of supported inputs:
- "Meeting with John tomorrow at 3pm"
- "Dentist appointment on June 15 at 2:30pm"
- "Weekly team standup every Monday at 9am"
- "Birthday party at Dave's place on Saturday from 7pm to 11pm"

## Technical Specifications

### Technology Stack
- **Programming Language**: Kotlin
- **Minimum Android SDK**: API 21 (Android 5.0 Lollipop)
- **Target Android SDK**: API 33 (Android 13)
- **Build System**: Gradle with Kotlin DSL
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room (SQLite abstraction layer)
- **Date/Time Handling**: Joda-Time
- **UI Components**: Material Design Components
- **Natural Language Processing**: Azure AI Text Analytics with Named Entity Recognition

### Dependencies
- **Android Jetpack**: AndroidX core, AppCompat, ConstraintLayout
- **Data Persistence**: Room Database
- **Concurrency**: Kotlin Coroutines
- **Network**: Azure Core HTTP
- **Cloud Services**: Azure AI Text Analytics (for NER)
- **Voice Input**: Android Speech Recognition API

## Architecture Overview

IntelliCal follows a modular architecture with clear separation of concerns:

### Core Components

1. **UI Layer**
   - Activities and Fragments for different calendar views
   - Custom views for specialized calendar display
   - Event editors and dialogs

2. **ViewModel Layer**
   - Manages UI-related data
   - Handles business logic for calendar operations
   - Communicates between UI and repositories

3. **Repository Layer**
   - Abstracts data sources
   - Provides clean APIs to the ViewModel layer
   - Manages local database and remote data synchronization

4. **Data Layer**
   - Room Database for local storage
   - CalDAV synchronization for remote calendars
   - Data models and entities

### Natural Language Processing Integration

The natural language event creation feature is implemented as a separate module that integrates with the core calendar functionality:

1. **NaturalLanguageEventActivity**
   - Handles user input (text and voice)
   - Provides a dedicated UI for event creation using natural language

2. **NaturalLanguageParser**
   - Core component that processes natural language input
   - Integrates with Azure AI Text Analytics for entity extraction
   - Falls back to basic pattern matching when Azure is unavailable

3. **Event Creation Flow**
   - Extracts structured data from parsed text
   - Populates EventActivity with pre-filled fields
   - Allows users to review and edit before confirmation

## Implementation Details

### Named Entity Recognition (NER) Implementation

The natural language parsing utilizes Azure's Text Analytics API to extract named entities from user input:

1. **Entity Types Used**:
   - `DATETIME`: For event start and end times
   - `LOCATION`: For event venues or places
   - `PERSON`: For potential attendees or participants
   - `QUANTITY`: For detecting recurring patterns

2. **Processing Pipeline**:
   - Input text is sent to Azure Text Analytics API
   - Recognized entities are categorized and processed
   - Title is extracted by removing recognized entities from original text
   - Structured event data is created from processed entities

3. **Context-Aware Processing**:
   - Current calendar view date/time is passed as context
   - Relative time expressions (e.g., "tomorrow", "next week") are resolved against this context
   - Time zones and local preferences are considered

4. **Fallback Mechanism**:
   - When Azure is unavailable or credentials are not configured
   - Local rule-based parser uses regex patterns to extract basic event details
   - This ensures functionality even without cloud connectivity

### Security Implementation

1. **Credentials Management**:
   - API credentials stored in `local.properties` (not in version control)
   - Injected into BuildConfig at compile time
   - Not exposed in UI or stored in user preferences

2. **User Data Protection**:
   - Event data is processed locally first
   - Only minimal necessary data sent to Azure for analysis
   - No persistent storage of user inputs on cloud services

## Use Cases and Benefits

### For End Users

- **Faster Event Creation**: Reduce time spent filling out forms by using natural language
- **Reduced Friction**: Voice input allows hands-free event creation while on the go
- **Easier Planning**: Quick creation of complex recurring events with simple phrases
- **Flexibility**: Works with both typed and spoken input, with or without connectivity

### For Organizations

- **Improved Productivity**: Streamlined calendar management for teams
- **Accessibility**: Voice input benefits users with various accessibility needs
- **Integration Potential**: Framework can be expanded for deeper integration with other systems
- **Cross-platform Usage**: Same natural language patterns work across Android devices

### For Developers

- **Modular Design**: Easy to extend with additional NLP capabilities
- **Fallback Systems**: Graceful degradation when cloud services unavailable
- **Configurable Security**: Flexible credential management for different environments
- **Example Architecture**: Demonstrates best practices for AI service integration in Android

## Responsible AI Practices

IntelliCal implements several responsible AI practices in its natural language processing:

1. **Privacy by Design**:
   - Minimal data collection: Only processes what users explicitly enter
   - Local processing when possible
   - No storage of user queries in the cloud
   - Transparent processing with user review before event creation

2. **Inclusivity and Accessibility**:
   - Voice input benefits users with mobility limitations
   - Simple language patterns work across different English dialects
   - Fallback mechanisms ensure functionality for all users

3. **User Control and Transparency**:
   - All extracted event data is shown to users for review before creation
   - Clear feedback about AI limitations and processing status
   - Option to edit any AI-extracted details before confirming

4. **Graceful Degradation**:
   - Basic functionality works without AI services
   - Clear notifications when falling back to basic parsing
   - No critical dependencies on cloud AI services

5. **Documentation and Disclosure**:
   - Clear documentation of AI features and limitations
   - Transparent about data handling practices
   - Developer guidance for responsible implementation

## Developer Setup

### Basic Setup
1. Clone this repository
2. Open the project in Android Studio
3. Build and run the application

### Azure AI Language Service Setup

The natural language event creation feature uses Azure AI Text Analytics with Named Entity Recognition (NER) for advanced parsing capabilities. To enable this feature in your development environment:

1. **Create an Azure Account and Resource**:
   - Sign up for an Azure account at [portal.azure.com](https://portal.azure.com)
   - Create a Text Analytics resource in the Azure portal
   - Select the pricing tier (there is a free tier available for development)
   - Deploy the resource and navigate to the "Keys and Endpoint" section
   - Note your API key and endpoint URL

2. **Configure NER for Optimal Results**:
   - The default Text Analytics service includes Named Entity Recognition capabilities
   - No additional configuration is required for basic entity extraction
   - Entity categories used: DATETIME, LOCATION, PERSON, QUANTITY

3. **Configure the App**:
   - Add the following to your `local.properties` file (this file is not committed to version control):
   ```properties
   azure.api.key=YOUR_ACTUAL_AZURE_API_KEY
   azure.endpoint=https://your-actual-azure-endpoint.cognitiveservices.azure.com/
   ```

4. **Build and Test**:
   - Rebuild the app after adding these properties
   - Test the natural language creation feature
   - If Azure integration is properly configured, you'll see advanced parsing capabilities
   - If not configured, the app will fall back to basic parsing

### Security Notes

- API credentials are securely stored in `local.properties` and injected into BuildConfig at compile time
- Different credentials can be used for debug and production builds
- No sensitive information is exposed in the app UI or stored in user preferences
- The natural language feature is enabled by default for all users

## Conclusion

IntelliCal demonstrates how modern AI services can enhance traditional mobile applications by reducing friction in common tasks. By integrating Named Entity Recognition with a calendar app, we create a more natural, intuitive experience for event creation.

The modular architecture ensures that the app remains functional even without cloud connectivity, while still providing enhanced capabilities when available. The security-focused design protects user data while leveraging powerful AI services.

This project serves as both a practical calendar application and a reference implementation for integrating Azure AI services into Android applications in a responsible, user-focused manner.