# Campus Event & Organization Hub (CEOH)

---

## Description

The **Campus Event & Organization Hub (CEOH)** is a role-based Android mobile application developed for the **University of the Cordilleras (UCC)**. It serves as a centralized digital platform for managing, announcing, and tracking campus events across all colleges and departments.

The application bridges the gap between event organizers (officers), students, and administrators by providing a unified system for event creation, approval, registration, attendance tracking, and real-time notifications. It operates with a dual-database architecture — a local SQLite database for offline functionality and Firebase Firestore for cloud synchronization — ensuring uninterrupted access regardless of network conditions.

CEOH is built on Android (Java), backed by Firebase services (Authentication, Firestore, Cloud Messaging, Storage), and powered by Firebase Cloud Functions for server-side logic including email verification, password management, and push notification delivery.

---

## Objectives

1. **Centralize Campus Event Management**
   Provide a single platform where all approved campus events are visible to students, organized by department, category, and date — eliminating the need for scattered announcements across different channels.

2. **Streamline Event Approval Workflow**
   Implement a structured submission and approval pipeline where officers submit events, and administrators review, approve, reject, postpone, or cancel them — with automatic notifications at each stage.

3. **Enable Accurate and Secure Attendance Tracking**
   Introduce a single-use, time-windowed redemption code system for recording student Time In and Time Out attendance, with rate limiting, audit logging, and server-time enforcement to prevent fraud or manipulation.

4. **Deliver Real-Time Notifications**
   Notify relevant stakeholders (students, officers, admins) of important events such as new event postings, approval decisions, and event changes using Firebase Cloud Messaging push notifications and in-app notification inbox.

5. **Support Offline Access**
   Maintain full event browsing capability when the device has no internet connection through a locally synchronized SQLite database, with automatic cloud re-sync upon reconnection.

6. **Enforce Role-Based Access Control**
   Distinguish between three user roles — Student, Officer, and Admin — each with a clearly scoped set of capabilities, ensuring that sensitive operations (event approval, user management, data export) are restricted to authorized users only.

7. **Provide Data Insights and Reporting**
   Equip officers with per-event analytics (registrations, attendance counts) and provide administrators with system-wide statistics, department breakdowns, and CSV export/import functionality for records management.

8. **Ensure Application Security**
   Apply security best practices throughout the app: BCrypt password hashing, login rate limiting, email verification before first login, and full audit trails on attendance code usage.

---

## Scope

The scope of CEOH covers the following functional areas and user interactions:

### User Management
- User registration with email verification and BCrypt-hashed passwords
- Role-based login (Student, Officer, Admin) with Firebase Authentication
- Password reset via time-limited email tokens
- Profile management (name, department, gender, mobile, profile photo)
- Admin-controlled user promotion, demotion, and deletion

### Event Management
- Officer event creation with fields: title, description, date/time, venue, tags (department), category, and banner image
- Admin approval workflow: Approve, Reject (with reason), Postpone (with suggested date/time), Cancel
- Event visibility filtering: events are shown to students based on department tag matching
- Event status lifecycle: `PENDING → APPROVED / REJECTED / POSTPONED / CANCELLED`
- Admin ability to edit, hide, or restore any event

### Event Discovery and Registration
- Home discovery feed with sub-tabs: Today's Events, Campus-Wide Events, My Department's Events, and Explorer (all departments)
- Full event list with search and category-chip filtering
- One-tap student event registration (with duplicate prevention)
- Student view of all their registered events

### Attendance System
- Officer-generated unique 6-character attendance codes per event (Time In / Time Out)
- Per-student single-use codes (primary) with event-level fallback codes
- Attendance time windows (open/close timestamps) enforced using server time
- Sequential enforcement: Time Out only allowed after successful Time In
- Rate limiting: 5 failed code attempts triggers a timed lockout
- Full audit log per attempt (student ID, event ID, device info, IP address, result, timestamp)

### Notifications
- In-app notification inbox with unread count badge
- Notification types: `NEW_EVENT`, `APPROVED`, `REJECTED`, `POSTPONED`, `CANCELLED`, `NEW_PENDING`, `RESUBMIT`
- FCM push notifications via Firebase Cloud Functions triggered on Firestore document creation
- Three notification channels: Student Events, Officer Decisions, Admin Approvals
- Notification archiving and notification preference settings (All Events or Department-Only)

### Venue Management
- Browsable list of campus venues with capacity information
- Display of today's booked events per venue

### Admin Tools
- System-wide statistics dashboard (user counts, event counts by status, top events, department breakdown)
- User management panel (view, search, promote to Officer, demote to Student, delete accounts)
- CSV data export and import for records management

### Offline Support
- Full SQLite local database (schema version 15) that mirrors Firestore collections
- `SyncManager` performs bidirectional sync at app launch and on connectivity restore
- Offline connectivity banner shown in the main interface when the device is disconnected

### Backend (Firebase Cloud Functions)
- `registerUser` — Creates user in Firestore and sends email verification
- `verifyEmail` — Validates token and activates account
- `resendVerificationEmail` — Issues a new verification token
- `requestPasswordReset` — Sends a password reset email with 1-hour expiry token
- `resetPassword` — Validates reset token and updates BCrypt hash
- `verifyPassword` — Validates BCrypt credentials at login
- `deleteUserAccount` — Removes Firebase Auth user, Firestore user doc, registrations, and notifications
- `sendPushOnNotification` — Auto-triggers FCM push on new notification documents in Firestore

---

## Limitations

1. **Android-Only Platform**
   CEOH is exclusively an Android application (minimum SDK 24 / Android 7.0). There is no iOS, web, or desktop counterpart. Users on non-Android devices cannot access the platform.

2. **No Real-Time Event Editing by Officers**
   Once an event is submitted by an officer, only the Admin can edit its details. Officers cannot modify a submitted event; they can only view its status and await the admin decision.

3. **Hardcoded Venue List**
   The campus venues available for event booking are hardcoded in the application (`VenueFragment`). Adding, removing, or modifying venues requires a code change and app update — there is no admin interface for managing venues dynamically.

4. **Hardcoded Department List**
   The list of colleges and departments (CBA, CCJE, COED, COE, COL, CLAS, GS) is hardcoded in the application resources. Adding a new department requires a code change and app update.

5. **Single-Institution Design**
   The application is designed specifically for the University of the Cordilleras (UCC). It is not a multi-tenant system and cannot support multiple institutions without significant architectural changes.

6. **No In-App Messaging or Discussion**
   CEOH does not include a messaging, comment, or discussion feature. Communication between students, officers, and admins is limited to the structured notification system.

7. **Attendance Requires Officer Participation**
   The attendance system depends on an officer actively generating and distributing redemption codes. If no officer generates a code, students cannot record attendance — there is no automatic or QR-based alternative.

8. **Email Dependency for Account Activation**
   New users must verify their email address before they can log in. Users without reliable email access or in environments where emails are filtered as spam cannot activate their accounts.

9. **Admin Account is Singular and Manually Provisioned**
   The admin role is granted via a Firebase Authentication custom claim (`admin: true`), which must be set manually (e.g., via Firebase Console or a provisioning script). There is no in-app flow to create an initial admin account.

10. **Limited Offline Write Capability**
    While offline browsing is fully supported, actions that require cloud communication (event registration, attendance code submission, notification delivery) will fail or queue until connectivity is restored. There is no offline-first write queue implemented for these operations.

11. **Image Storage Constraints**
    Event banner images are stored either as local URIs (on the device) or in Firebase Storage. Large images increase sync times and storage costs. There is no enforced image size limit beyond in-app compression applied by `ImageUtils`.

12. **No Recurring or Template Events**
    Officers must create each event individually from scratch. There is no feature to create recurring events (e.g., weekly meetings) or reuse a previous event as a template.
