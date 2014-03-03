Overview
========

This project was my first attempt at an Android application.  It is used to find bicycle racks in the greater Chicago area.  I found a public dataset one evening that the city of Chicago had put online containing a bunch of data about all of the bicycle racks maintained by the city with their street address, GPS coordinates, number of spaces, etc.  I had been looking into building an Android application at the time and this seemed like a useful set of data to use for a first app.

It has two parts, a couple php scripts I threw together to pass the app data and the application its self.  The php code is very rough and doesn't escape or validate input, which is clearly not secure or production worthy by any means.  The java code is pretty complete with error checking, but it typically throws up a toast message with error details for debugging purposes.

The basic functionality is to find the users current location and then query a php web service to find bicycle racks near that location to display on the map.  The user is able to scroll around the map and look for bicycle racks and if they select one a dialog will be shown with the street address, number of spaces and a button to have the application fetch bicycle directions from the users current position.

It also features a search function, using the Yahoo places API, that will let the user punch in an address or street and bring up a list of bicycle racks, ordered by distance, near the searched location.

I've included the main source files that have real code in them and left out some of the smaller helper source files.  This application is still a work in progress, but I haven't touched it since moving to Madison.  As such, there are some definite optimizations that I still have to make and some general cleanup, but it is a good body of code for an example.

Accomplishments
===============

* Uses the Great Circle Equation to calculate distances between GPS coordinates.
* First Android app and first time coding in Java.
* Uses GeoSpatial functionality of MySQL.
* Uses intelligent data optimizations when querying and displaying bicycle racks based on the user zoom level to avoid sending large amounts of info across the wire, which results in very slow performance or timeouts.

Details
=======

### PHP Code ###
There are two PHP scripts, racks.php and search_racks.php.  racks.php takes the users GPS coordinates as input and sends back a JSON list of bicycle racks near them.

search_racks.php takes the users GPS coordinates and a search string as input and queries Yahoo places to find a location matching the users search.  If Yahoo returns a result the the Latitude and Longitude used to query will be from the Yahoo result, otherwise we use the users current location and query the database for an address or community like the users search.

### Java Code ###
The main source file is BikeRackLocatorActivity.java.  It contains the bulk of the program logic for displaying the map and bicycle racks, and also for launching the search activity.  I have a class, BaseAvailableActivity (referred to as glob for global) that is available to the different activities.  I use this class to pass data between the activities such as the users location, current zoom level and an object for the currently selected bicycle rack, if there is one.

ListViewActivity.java is used to display a list of bicycle racks vs the map view.

MyItemizedOverlay.java is what displays the bicycle rack's on the map and handles clicking on them to bring up a dialog with their information.

MyLocation.java is what handles getting the users location and keeping it updated.  It uses logic designed to give a quick result to the user and fine-tune it after.  This is basically the same logic that Google Maps uses to display an approximate location with a giant circle of "you are somewhere in here" and then work in the background to get a finer fix.

SearchableActivity.java is used to take a search string from the user, query the php search_racks.php script and display a list of results.
