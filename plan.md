# Medical Records App — Development Plan

## 1. Project Overview

Build a simple, user-friendly Android application for maintaining personal and family medical records.

The app is intended to work like a **digital medical notebook**. A user can maintain records for multiple people, such as:

- Self
- Father
- Mother
- Children
- Spouse
- Other family members

The application will be **completely offline**. There will be no backend server, login system, cloud database, or online account.

All data will be stored locally on the user's Android device.

---

# 2. Main Goals

The application should allow users to:

1. Add multiple people/family members.
2. Maintain separate medical records for each person.
3. Quickly record health measurements.
4. Maintain medical notes.
5. View historical records.
6. View simple trends/charts.
7. Edit and delete records.
8. Search and filter records.
9. Export/backup records.
10. Protect the application with PIN/biometric authentication.

The application should prioritize:

- Simplicity
- Fast data entry
- Readability
- Large touch targets
- Minimal navigation
- Offline functionality
- Privacy

---

# 3. Technology Stack

## Android

- Kotlin
- Jetpack Compose
- Material 3
- Android Jetpack

## Architecture

- MVVM
- Repository pattern
- Kotlin Coroutines
- StateFlow

## Local Database

- Room
- SQLite underneath Room

## Other

- DataStore for application preferences
- Android Biometric API for biometric authentication
- WorkManager only if background operations are later required
- Compose-compatible chart library for trends

No backend or cloud service is required.

---

# 4. Application Structure

The application will have three main areas:

```text
Application
│
├── People
│   ├── Person Profile
│   ├── Records
│   ├── History
│   ├── Trends
│   ├── Notes
│   └── Medications
│
├── Search
│
└── Settings
```

---

# 5. Home Screen — People

The home screen should display all people managed by the user.

Example:

```text
Medical Records

Your People

┌─────────────────────────────┐
│ 👨  Dad                     │
│     62 years                │
│     Last record: Today      │
└─────────────────────────────┘

┌─────────────────────────────┐
│ 👩  Mom                     │
│     57 years                │
│     Last record: Yesterday  │
└─────────────────────────────┘

┌─────────────────────────────┐
│ 👦  Rahul                   │
│     24 years                │
│     Last record: Sep 28     │
└─────────────────────────────┘

        + Add Person
```

Requirements:

- Display person's name.
- Display age where available.
- Display profile image/avatar.
- Display latest record date.
- Tap a person to open their profile.
- Add new person using the `+ Add Person` button.

---

# 6. Person Management

Users should be able to create and manage multiple people.

## Person fields

Required:

- Name

Optional:

- Profile photo
- Relationship
- Date of birth
- Gender
- Notes

Example:

```text
Add Person

Photo
+ Add Photo

Name
[ Dad ]

Relationship
[ Father ]

Date of Birth
[ 12 March 1964 ]

Gender
[ Male ]

Notes
[ Optional ]

[ Save Person ]
```

Users should also be able to:

- Edit person information.
- Delete a person.
- Change profile photo.
- View all records associated with that person.

Deleting a person should clearly warn that their associated records will also be deleted.

---

# 7. Person Profile

When the user selects a person, show an overview.

Example:

```text
Dad

62 years old

Latest Measurements

Blood Pressure
124 / 82 mmHg

Weight
71.2 kg

Blood Sugar
98 mg/dL

Pulse
72 bpm

+ Add Record

Overview | History | Trends | Notes
```

The profile should provide quick access to the person's most recent measurements.

---

# 8. Health Measurements

Initial supported measurements:

### Blood Pressure

Fields:

- Systolic
- Diastolic
- Pulse
- Date
- Time
- Optional note

Example:

```text
Blood Pressure

Systolic
[ 124 ]

Diastolic
[ 82 ]

Pulse
[ 72 ]

Date
[ Today ]

Time
[ 08:30 AM ]

Note
[ Before breakfast ]

[ Save ]
```

---

### Blood Sugar

Fields:

- Value
- Unit
- Measurement context
- Date
- Time
- Optional note

Measurement context:

- Fasting
- Before meal
- After meal
- Random

---

### Weight

Fields:

- Weight
- Unit
- Date
- Time
- Optional note

---

### Pulse

Fields:

- BPM
- Date
- Time
- Optional note

---

### SpO₂

Fields:

- Oxygen saturation percentage
- Pulse
- Date
- Time
- Optional note

---

### Temperature

Fields:

- Temperature
- Unit
- Date
- Time
- Optional note

---

# 9. Add Record Flow

The user should be able to add a record from the person profile.

```text
+ Add Record

Blood Pressure
Blood Sugar
Weight
Pulse
SpO₂
Temperature
Note
```

The add-record screen should be simple and optimized for quick entry.

The user should ideally be able to save a measurement in less than 10–15 seconds.

---

# 10. History

Each person should have a chronological history of their records.

Example:

```text
Dad

October 2026

Today

❤️ Blood Pressure
124 / 82 mmHg
8:30 AM

🩸 Blood Sugar
98 mg/dL
8:35 AM

⚖️ Weight
71.2 kg
8:40 AM

3 October

📝 Doctor Visit
10:30 AM

1 October

❤️ Blood Pressure
128 / 84 mmHg
8:15 AM
```

Features:

- Chronological ordering.
- Group records by date.
- Filter by record type.
- Open record details.
- Edit record.
- Delete record.

Filters:

```text
All
Blood Pressure
Blood Sugar
Weight
Pulse
SpO₂
Temperature
Notes
```

---

# 11. Record Details

When a record is selected, show its complete information.

Example:

```text
Blood Pressure

124 / 82 mmHg

Systolic       124
Diastolic       82
Pulse           72

5 October 2026
8:30 AM

Note:
Before breakfast

[ Edit ]    [ Delete ]
```

---

# 12. Notes

Users should be able to maintain free-form medical notes.

Example:

```text
New Note

Title
[ Doctor Visit ]

Date
[ 5 October 2026 ]

Note

[ Doctor advised continuing the
  current medication. Follow-up
  after one month. ]

[ Save ]
```

Examples of notes:

- Doctor visits
- Symptoms
- Medication changes
- Test observations
- General medical information
- Important instructions from doctors

Notes should belong to a specific person.

---

# 13. Medications

Basic medication tracking should be included in the initial design, but kept simple.

Example:

```text
Medications

Amlodipine
5 mg

Once daily

Metformin
500 mg

Twice daily
```

Fields:

- Medicine name
- Dosage
- Frequency
- Start date
- End date (optional)
- Notes

Medication reminders can be added in a later version.

---

# 14. Trends

Provide simple charts for measurements that can be represented numerically.

Supported:

- Blood pressure
- Blood sugar
- Weight
- Pulse
- SpO₂
- Temperature

Example:

```text
Blood Pressure

Last 30 Days

140 ┤
130 ┤     ●
120 ┤ ● ●   ●  ●
110 ┤   ●
100 ┤
    └────────────────

Average
124 / 79

[ 7 Days ] [ 30 Days ] [ 3 Months ]
```

The goal is to show the **change in recorded values**, not to diagnose medical conditions.

Avoid automatically labeling a person as having a medical condition based solely on recorded values.

---

# 15. Search

The application should provide a global search.

Example:

```text
Search

[ 🔍 Search records ]

Results:

Dad
Blood Pressure
124 / 82
5 Oct 2026

Mom
Blood Sugar
108 mg/dL
4 Oct 2026

Dad
Doctor Visit
3 Oct 2026
```

Search should support:

- Person name
- Record type
- Note title
- Note content
- Date

---

# 16. Data Model

## Person

```text
Person
------
id
name
relationship
dateOfBirth
gender
photoUri
notes
createdAt
updatedAt
```

## HealthRecord

```text
HealthRecord
------------
id
personId
type
value1
value2
unit
context
dateTime
note
createdAt
updatedAt
```

Possible record types:

```text
BLOOD_PRESSURE
BLOOD_SUGAR
WEIGHT
PULSE
SPO2
TEMPERATURE
```

## Note

```text
Note
----
id
personId
title
content
dateTime
createdAt
updatedAt
```

## Medication

```text
Medication
----------
id
personId
name
dosage
frequency
startDate
endDate
notes
createdAt
updatedAt
```

The `personId` is important because every record must belong to a specific person.

---

# 17. Room Database Relationships

Conceptually:

```text
Person
  │
  ├───────────────┐
  │               │
  ▼               ▼
HealthRecord     Note
  │
  │
  └── personId

Person
  │
  ▼
Medication
```

A person can have:

```text
1 Person
   │
   ├── Many HealthRecords
   ├── Many Notes
   └── Many Medications
```

Deleting a person should optionally cascade-delete all associated data after confirmation.

---

# 18. Local Storage

All information should remain on the device.

```text
Android Device
│
└── Application
    │
    └── Room Database
        │
        ├── People
        ├── Health Records
        ├── Notes
        └── Medications
```

There should be:

- No server
- No API
- No Firebase
- No online account
- No cloud database
- No mandatory internet connection

The app should work completely offline.

---

# 19. Backup and Export

Because all data is local, backup is important.

Settings should include:

```text
Data

Export Data
Import Data

Export PDF
Export CSV
```

## Export

Users should be able to select:

- Person
- Date range
- Record types

Example:

```text
Export Records

Person
[ Dad ]

Date
[ 01 Jan 2026 - 05 Oct 2026 ]

Records
☑ Blood Pressure
☑ Blood Sugar
☑ Weight
☑ Pulse
☐ Notes

[ Export PDF ]
```

A PDF should provide a clean medical-record summary that can be shared with a doctor.

---

# 20. Privacy and App Lock

Because the application stores medical information, provide optional app protection.

Options:

```text
Security

App Lock
[ ON ]

Use biometric authentication
[ ON ]

PIN
[ Change PIN ]
```

Use Android's native biometric authentication where supported.

No medical information should be unnecessarily exposed through notifications.

---

# 21. Settings

Settings should remain simple.

```text
Settings

Appearance
- Theme
- Light / Dark / System

Units
- Weight: kg / lb
- Temperature: °C / °F
- Glucose: mg/dL / mmol/L

Security
- App Lock
- Biometric

Data
- Export
- Import
- Backup
- Delete Data

About
- App version
- Privacy
```

---

# 22. Navigation

Recommended navigation:

```text
                 App
                  │
          ┌───────┴────────┐
          │                │
       People            Search
          │
          ▼
       Person
          │
    ┌─────┼─────────────┐
    ▼     ▼      ▼      ▼
 Overview History Trends Notes
```

Settings can be accessible from the top-right menu or bottom navigation.

Avoid having too many bottom-navigation tabs.

---

# 23. UI/UX Guidelines

The application should feel like a **simple personal notebook**, not hospital software.

### Design principles

- Large readable text
- Large touch targets
- Clear icons
- Minimal forms
- Lots of whitespace
- Consistent cards
- Clear primary action
- Simple language
- Minimal navigation
- Avoid unnecessary medical terminology

### Avoid

- Complex dashboards
- Excessive colors
- Too many charts
- Tiny buttons
- Dense tables
- Excessive animations
- Unnecessary onboarding

---

# 24. Color and Visual Style

Recommended visual direction:

- Light background
- Soft neutral surfaces
- One primary accent color
- Different subtle icons for measurement types
- High contrast text
- Rounded cards
- Simple typography

The design should be accessible to older users as well.

Especially for family medical records, consider:

- Larger default font size
- Clear labels
- Large numeric values
- High contrast
- Simple icons

---

# 25. Development Phases

## Phase 1 — Project Setup

- Create Android project.
- Configure Kotlin.
- Configure Jetpack Compose.
- Configure Room.
- Set up MVVM structure.
- Set up navigation.
- Create basic theme.

---

## Phase 2 — People

Implement:

- People screen
- Add person
- Edit person
- Delete person
- Person profile
- Person switching

---

## Phase 3 — Health Records

Implement:

- Add blood pressure
- Add blood sugar
- Add weight
- Add pulse
- Add SpO₂
- Add temperature
- Record details
- Edit records
- Delete records

---

## Phase 4 — History

Implement:

- Chronological timeline
- Date grouping
- Record filtering
- Search
- Record details

---

## Phase 5 — Notes

Implement:

- Create note
- Edit note
- Delete note
- View notes
- Search notes

---

## Phase 6 — Medications

Implement:

- Add medication
- Edit medication
- Delete medication
- View active medications
- Medication details

Medication reminders are optional and can be postponed.

---

## Phase 7 — Trends

Implement:

- Blood pressure chart
- Blood sugar chart
- Weight chart
- Pulse chart
- SpO₂ chart
- Temperature chart
- 7-day view
- 30-day view
- 3-month view

---

## Phase 8 — Data Management

Implement:

- Export
- Import
- CSV
- PDF
- Backup/restore
- Delete all data

---

## Phase 9 — Security

Implement:

- PIN
- Biometric authentication
- App lock
- Secure handling of local data

---

## Phase 10 — Polish and Testing

Test:

- Different Android screen sizes
- Small/large fonts
- Dark mode
- Offline operation
- Database migrations
- Large number of records
- Multiple family members
- Deleting people
- Backup/restore
- Export
- App restart
- Device rotation/configuration changes

---

# 26. MVP Scope

The first release should contain:

### People

- [x] Multiple people
- [x] Add person
- [x] Edit person
- [x] Delete person

### Records

- [x] Blood pressure
- [x] Blood sugar
- [x] Weight
- [x] Pulse
- [x] SpO₂
- [x] Temperature

### Organization

- [x] History
- [x] Search
- [x] Filters
- [x] Notes

### Visualization

- [x] Basic trends
- [x] Simple charts

### Data

- [x] Local Room database
- [x] Export
- [x] Import/backup

### Security

- [x] PIN
- [x] Biometric lock

---

# 27. Features for Future Versions

Do not include these in the initial release unless specifically requested:

- Cloud synchronization
- User accounts
- Multiple-device synchronization
- Health Connect
- Wearable integration
- AI assistant
- OCR medical report extraction
- Automatic diagnosis
- Doctor portal
- Online consultations
- Medication reminders
- Family sharing

These can be added later without changing the core concept.

---

# 28. Final Product Definition

The application should be positioned as:

> **A simple, private, offline medical notebook for managing health records for yourself and your family.**

The key user flow should remain:

```text
Open App
   ↓
Select Person
   ↓
View Latest Records
   ↓
Add / View Record
   ↓
Save
```

The application should not try to replace a hospital management system or provide medical diagnosis.

Its primary purpose is:

> **Store → Organize → Review → Share personal medical information easily.**