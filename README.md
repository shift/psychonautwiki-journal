# PsychonautWiki Journal

A comprehensive harm reduction desktop application built with Kotlin Multiplatform and Compose Desktop, featuring the complete PsychonautWiki substance database.

## 🎯 Purpose

This application provides evidence-based harm reduction information for psychoactive substances, helping users make informed decisions about substance use through comprehensive data on dosing, duration, interactions, and safety information.

## ✨ Features

- **📊 Complete PsychonautWiki Database**: Access to 289 psychoactive substances with comprehensive harm reduction data
- **💊 Detailed Substance Information**: 
  - Dosing guidelines for different routes of administration
  - Duration data (onset, peak, offset, total duration)
  - Bioavailability information
  - Interaction warnings and contraindications
  - Tolerance and addiction potential information
- **🔍 Advanced Search & Filtering**: Search by substance name, common names, categories, or effects
- **⚡ Fast Performance**: Optimized database loading with health monitoring and metrics
- **🖥️ Modern Desktop UI**: Clean, responsive interface built with Compose Desktop
- **🛡️ Robust Error Handling**: Graceful fallbacks and comprehensive error reporting

## 🛠️ Technology Stack

- **Kotlin Multiplatform**: Cross-platform development
- **Compose Desktop**: Modern declarative UI framework
- **kotlinx.serialization**: JSON parsing and data serialization
- **SQLDelight**: Database management with coroutines support
- **Koin**: Dependency injection
- **NixOS**: Reproducible development environment

## 🚀 Getting Started

### Prerequisites

- **Nix with flakes enabled** (recommended for reproducible builds)
- **Java 17+** (if not using Nix)
- **Gradle 7.6+** (if not using Nix)

### Quick Start with Nix

1. Clone the repository:
   ```bash
   git clone https://github.com/shift/psychonautwiki-journal.git
   cd psychonautwiki-journal
   ```

2. Enter the development environment:
   ```bash
   nix develop
   ```

3. Build and run the application:
   ```bash
   cd psychonautwiki-journal-desktop
   gradle run
   ```

### Traditional Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/shift/psychonautwiki-journal.git
   cd psychonautwiki-journal/psychonautwiki-journal-desktop
   ```

2. Build and run:
   ```bash
   ./gradlew run
   ```

## 📊 Database Integration

The application successfully loads the complete PsychonautWiki database:

- **289 substances** with comprehensive harm reduction data
- **12 substances** with bioavailability information
- **Multiple routes of administration** per substance
- **Interaction warnings** for dangerous combinations
- **Category-based organization** (psychedelics, stimulants, depressants, etc.)

### Database Loading Features

- **Progress Tracking**: Real-time loading progress with metrics
- **Health Monitoring**: Database integrity checks and load time tracking
- **Graceful Fallbacks**: Fallback to essential substances if full database fails
- **Error Recovery**: Robust handling of malformed JSON entries

Expected console output on successful load:
```
🔄 Loading PsychonautWiki substance database...
📁 JSON file loaded: 458192 characters
✅ Successfully loaded PsychonautWiki database in ~500ms
📊 Database metrics:
   • 289 substances
   • 15 categories
   • 180+ substances with dosing/duration data
   • 12 substances with bioavailability data
   • 150+ substances with interaction warnings
   • 400+ total routes of administration
```

## 🏗️ Architecture

### Data Layer
- **PsychonautWikiDatabase**: Singleton database manager with JSON parsing
- **SubstanceRepository**: Repository pattern for data access
- **SubstanceLoader**: Service layer for substance loading operations

### Domain Models
- **SubstanceInfo**: Complete substance information
- **RouteOfAdministration**: Dosing and route-specific data
- **BioavailabilityRange**: Bioavailability percentage ranges
- **DurationRange**: Time-based effect duration data
- **InteractionData**: Substance interaction warnings

### UI Layer
- **Compose Desktop**: Modern declarative UI
- **MVVM Pattern**: ViewModels for state management
- **Responsive Design**: Adaptive layouts for different screen sizes

## 🔧 Build Commands

In the NixOS development environment:

```bash
# Build the application
gradle build

# Run the application
gradle run

# Run tests
gradle test

# Run all checks (including BDD tests)
gradle check

# Build distribution package
gradle distZip
```

## 🧪 Testing

The project includes comprehensive testing:

- **Unit Tests**: Core functionality and data models
- **Integration Tests**: Database loading and parsing
- **BDD Tests**: Cucumber-based behavior testing
- **Kotest**: Modern Kotlin testing framework

## 📁 Project Structure

```
psychonautwiki-journal/
├── psychonautwiki-journal-desktop/    # Desktop application
│   ├── src/commonMain/kotlin/          # Shared Kotlin code
│   │   ├── data/                       # Data layer (repositories, models)
│   │   ├── ui/                         # UI layer (screens, viewmodels)
│   │   └── navigation/                 # Navigation logic
│   ├── src/commonMain/resources/       # Resources (database JSON)
│   ├── src/commonTest/kotlin/          # Shared tests
│   └── src/desktopMain/kotlin/         # Desktop-specific code
├── psychonautwiki-journal-android/     # Android application (legacy)
├── flake.nix                          # NixOS development environment
└── README.md                          # This file
```

## 🔄 Recent Improvements

### JSON Parsing Fixes
- **Fixed bioavailability parsing**: Created `BioavailabilityRange` to handle complex JSON structures
- **Fixed duration range parsing**: Made `min` field optional for partial data
- **Enhanced error handling**: Robust parsing with graceful fallbacks

### Database Enhancements
- **Loading metrics**: Comprehensive health monitoring and performance tracking
- **Progress indicators**: Real-time feedback during database loading
- **Error recovery**: Partial loading support for malformed entries

### UI Improvements
- **Bioavailability display**: Shows bioavailability ranges in substance listings
- **Enhanced search**: Full-text search across complete database
- **Performance optimization**: Efficient rendering of large substance lists

## 🤝 Contributing

This project focuses on harm reduction and evidence-based information. Contributions are welcome for:

- **UI/UX improvements**
- **Additional harm reduction features**
- **Database optimization**
- **Testing and documentation**
- **Accessibility enhancements**

## ⚖️ Legal & Safety

- **Educational Purpose**: This application is for educational and harm reduction purposes only
- **Not Medical Advice**: Information provided is not medical advice; consult healthcare professionals
- **Substance Safety**: Promotes responsible use and harm reduction practices
- **Data Source**: Information sourced from PsychonautWiki, a collaborative harm reduction resource

## 📄 License

[License information to be added]

## 🙏 Acknowledgments

- **PsychonautWiki**: For providing the comprehensive substance database
- **Harm Reduction Community**: For evidence-based safety information
- **Kotlin & Compose Teams**: For excellent development tools
- **NixOS Community**: For reproducible development environments

---

**🛡️ Remember: The safest use is no use. If you choose to use substances, please prioritize harm reduction, start with low doses, test substances when possible, and never use alone.**