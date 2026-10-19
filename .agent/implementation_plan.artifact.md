# Transport Mode Selection and Location Override Fix

This plan addresses the two requests: adding a transport mode selector and fixing the location resolution bug.

## Open Questions
- None.

## Proposed Changes

### 1. Fix Event Location Override (The "South Wentworthville" bug)
The reason "71 dunmore st" wasn't working is that your work calendar event already had a location field defined (`"98344 - WENTWORTHVILLE"`). Our previous logic only applied your custom Work Location if the calendar event's location was completely blank. When the system tried to find `"98344 - WENTWORTHVILLE"`, the TfNSW API fuzzy-matched it to "South Wentworthville".
- **Fix:** We will update `ScheduleRepositoryImpl.kt` so that if you have explicitly set a Work Location or College Location in your settings, it will *always* forcefully override the calendar's default location.

### 2. Add Transport Mode Selection
We will add a new setting that lets you choose your preferred mode of transport, similar to Google Maps.
- **PreferencesManager:** Add a new key for `transport_mode` (e.g., All, Train, Bus, Walk).
- **SettingsViewModel & SettingsScreen:** Add a dropdown menu to select the preferred transport mode and save it to preferences.
- **TfNSWApiService & CommuteRepositoryImpl:** Pass the selected transport mode down to the API. TfNSW allows filtering by modes (e.g., `inclMOT_1` for Train, `inclMOT_5` for Bus, etc.).
- **DashboardViewModel:** Read the transport mode from preferences and supply it to the `CalculateCommuteUseCase`.

### [MODIFY] ScheduleRepositoryImpl.kt
- Change the `rawWorkEvents.map` and `rawCollegeEvents.map` logic to unconditionally use the user's custom location if it is set.

### [MODIFY] PreferencesManager.kt
- Add `TRANSPORT_MODE` preference key and flow.
- Add `saveTransportMode()` function.

### [MODIFY] SettingsViewModel.kt / SettingsScreen.kt
- Add state and dropdown UI for "Preferred Transport Mode" with options: "All Public Transport", "Train Only", "Bus Only".

### [MODIFY] TfNSWApiService.kt & CommuteRepositoryImpl.kt
- Add mode filtering parameters (`inclMOT_1`, `inclMOT_5`, etc.) to the `getTripPlan` API call based on the selected mode.

### [MODIFY] DashboardViewModel.kt
- Retrieve the `transportMode` from `PreferencesManager`.
- Pass it into `CalculateCommuteUseCase`.

## Verification Plan
- Verify that setting the Work Location to "71 dunmore st" correctly routes to that exact address on the timeline.
- Verify that changing the transport mode to "Bus Only" filters out train journeys and updates the commute times accordingly.