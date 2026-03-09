# Functional Requirements: CEOH (Campus Event & Organization Hub)

## [cite_start]1. User Roles & Access Control [cite: 8, 18, 23]
* [cite_start]**Students**: View events, register, and receive notifications based on tags[cite: 6, 16, 17, 20].
* [cite_start]**Organization Officers**: Create/manage events, track attendance, and filter target audiences[cite: 7, 13, 18, 20].
* [cite_start]**Administrators**: Approve events, monitor all org activity, and generate system-wide reports[cite: 8, 15, 19].

## 2. Core Features
### A. Event Management
* [cite_start]**Centralized Feed**: A mobile-first display of all approved campus activities[cite: 9, 12, 16].
* [cite_start]**Tag-Based Filtering**: Events are configured with student tags (e.g., Course, Department) to control eligibility[cite: 20].
* [cite_start]**Registration**: Students must be able to sign up for events directly in-app[cite: 6, 17].

### B. Attendance System (The "Redemption" Logic)
* [cite_start]**Code Validation**: A single redemption code system for "Time In" and "Time Out"[cite: 21].
* [cite_start]**Sequential Logic**: Users are only permitted to "Time Out" after a valid "Time In" code has been submitted[cite: 21].

## 3. Constraints & Limitations
* [cite_start]**Platform**: Exclusively an Android application[cite: 5].
* [cite_start]**Access**: Restricted to UCC students and authorized personnel[cite: 23].
* [cite_start]**UI Consistency**: Use stretchable 9-patch graphics (`.9.png`) for all card backgrounds, buttons, and containers to ensure visual integrity across different screen sizes.
* [cite_start]**Connectivity**: High-dependency on internet access for real-time updates[cite: 24].
* [cite_start]**Financials**: No processing of payments or fees[cite: 24].
* [cite_start]**Academic Integration**: No connection to enrollment or grading systems[cite: 25].