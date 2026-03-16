# Campus Event & Organization Hub (CEOH)

CEOH is a specialized Android application designed for University of the Cordilleras (UCC) students and organizations. It serves as a centralized platform for event management, registration, and attendance tracking, ensuring students are always updated with campus activities.

##  Key Features

- **Centralized Event Feed**: A real-time mobile feed showcasing all approved campus events and activities.
- **Tag-Based Filtering**: Advanced eligibility control using student tags (e.g., Course, Department). Only students with matching tags can register for specific events.
- **Event Registration**: Seamless in-app sign-up process for students.
- **Attendance System**: A robust "Single Redemption Code" logic for "Time In" and "Time Out" validation, ensuring accurate attendance records.
- **Notification Pipeline**: Instant push notifications triggered when new events are published, automatically filtered by student tags.
- **Admin & Officer Dashboard**: Specialized interfaces for organization officers (to manage events) and administrators (to approve events and generate reports).

## 🏛️ Project Architecture

CEOH follows a modern Android architectural approach:
- **UI Framework**: Android Jetpack (AppCompat, Material Design, ConstraintLayout).
- **Navigation**: Fragment-based navigation using `BottomNavigationView`.
- **Backend**: Integration with Firebase (Firestore) for real-time data synchronization and push notifications.
- **Data Flow**: Mobile App <--> Firebase Firestore <--> Admin/Officer Logic.

##  Tech Stack

- **Platform**: Android (Min SDK 24, Target SDK 36)
- **Language**: Java 11
- **Build System**: Gradle (Kotlin DSL)
- **Database & Auth**: Firebase Firestore
- **UI Components**: RecyclerView, CardView, CoordinatorLayout, SwipeRefreshLayout.

##  Project Structure

```text
app/src/main/java/com/example/campus_event_org_hub/
├── data/           # Database and Sync management
├── model/          # Data models (Event, NotifModel, etc.)
├── ui/             # UI Components (Activities & Fragments)
│   ├── admin/      # Administrator-specific logic
│   ├── auth/       # Authentication logic
│   ├── events/     # Event feed and details
│   └── main/       # Main navigation and hosting
└── util/           # Utility classes (ImageUtils, etc.)
```

##  Getting Started

### Prerequisites
- **Android Studio** (Koala or newer recommended)
- **JDK 11**
- **Android Device or Emulator** (API Level 24+)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/campus-event-organization-hub.git
   ```
2. Open the project in Android Studio.
3. Sync the project with Gradle files.

### Building and Running
To build and run the project from the CLI:

- **Build Debug APK**:
  ```powershell
  ./gradlew assembleDebug
  ```
- **Run Unit Tests**:
  ```powershell
  ./gradlew test
  ```
- **Install on Device**:
  ```powershell
  ./gradlew installDebug
  ```

##  Development Conventions

- **Graphics**: Use **9-patch images** (`*.9.png`) for stretchable UI elements like button backgrounds and container borders to prevent distortion across different screen sizes.
- **Resources**: Centralized management of strings (`strings.xml`), colors (`colors.xml`), and themes (`themes.xml`).
- **Code Style**: Standard Java Android conventions.

##  Contributing

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

##  License

Distributed under the MIT License. See `LICENSE` for more information.

---
*Built for the UCC Community.*
