# Android Project Checklist

Use this checklist to verify that the application demonstrates the required Android concepts, code locations, and actual runtime behavior.

## 1. Supporting Multiple Screens

- [ ] Observe the actual behavior of the application on multiple emulators or devices.
- [ ] Dealing with android market fragmentation
	- [ ] Screen resolutions & densities (`mdpi`, `hdpi`, `xhdpi`, etc., Android OS versions)
- [ ] Creating drawable resources for multiple screens
	- [ ] PNG/JPG, Icons, Vector graphics, Shapes, Backgrounds
- [ ] Creating stretchable 9-path graphics
	- [ ] 9-Patch Image; file name `.9.png`
- [ ] Creating a custom launcher icon
	- [ ] app icon display on home screen

## 2. Managing the User Interface

- [ ] Defining and using styles
	- [ ] shown in `res - values - styles`
- [ ] Applying application themes
	- [ ] shown in `res - values - themes`
- [ ] Creating a scrollable text display
	- [ ] shown inside Fragment layout
- [ ] Laying out a screen with fragments
	- [ ] a placeholder for fragments to be visible, can be static fragment, dynamic, fragment-based UI for navigation

## 3. Working with Events

- [ ] Handling user events with Java code
	- [ ] such as Text input event, `setOnClickListener()`, validation check
- [ ] Creating a Broadcast Receiver to handle system events
	- [ ] Should create inside in the Java package folder and must be Register in Manifest- shown in `app/src/main/AndroidManifest.xml` / Handles system events
- [ ] Handling orientation and other configuration changes
	- [ ] Orientation changes such as rotating the mobille from portrait to landscape - usually handled inside the Activity class; shown control configuration changes in the manifest file; shown separate layouts for different orientations thru alternative layouts

## 4. Working with Menus and the Action Bar

- [ ] Adding items to the options menu
	- [ ] Shown in `app/src/main/res/menu/`
- [ ] Displaying menu items in the action bar
	- [ ] Shown in the Java Activity file loads the menu into the Action Bar and for showing icons in the action bar see Inside the menu XML file, and check if it is added `showAsAction`.
- [ ] Managing the action bar and menus at runtime
	- [ ] Menu behavior is controlled in the Activity Java file; Added show/hide menu items dynamically; shown in `MainActivity.java` and go to `onCreateOptionsMenu()` and `MainActivity.java` go to `onOptionsItemSelected()` during runtime

## 5. Working with Data

- [ ] Passing data to an activity with intent extras
	- [ ] Shown in the java activity file folder- can send data using an Intent
- [ ] Receiving data in a new activity
	- [ ] Shown the usage when one screen sends data to another screen
- [ ] Returning data to a calling activity
	- [ ] Shown on how to send data back to the previous screen
- [ ] Displaying data in a list
	- [ ] Shown multiple data items
- [ ] Handling list items click events
	- [ ] Shown on how to detect which item the user clicked.
- [ ] Customizing the list item
	- [ ] Shown on how to create custom design instead of default list layout
- [ ] Exploring other uses of data
	- [ ] Shown on how data passed between activities can be used for

## 6. Working with Dynamic Data Using SQLite

- [ ] Fetching data from database
- [ ] Understanding different SQL queries.
- [ ] Creating CRUD application using SQLite Database

## Suggested Evidence to Collect

- [ ] Screenshots from multiple screen sizes and orientations.
- [ ] Code references for activities, fragments, adapters, receivers, and database classes.
- [ ] Manifest evidence for theme, icon, and receiver registration.
- [ ] Menu XML, drawable assets, and SQLite helper or repository code.
