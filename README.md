# find_location


# Android Location Estimation Test App

This repository contains a test app for performing location estimation based on data collected through an admin app.

## Purpose

The purpose of this repository is to provide an algorithm for estimating the user's location using data collected through an Android admin app. The test app communicates with a Flask server to send the sensed Wi-Fi data and receive the estimated location in JSON format, which is then displayed as text.

## How It Works

The app's functionality can be described as follows:

1. **Communication with Flask Server**: The Android app uses Retrofit to communicate with the Flask server. It sends the sensed Wi-Fi data through the communication interface.

2. **Wi-Fi Sensing**: The app utilizes various components, such as PermissionSupport, WifiManager, and BroadcastReceiver, to detect Wi-Fi signals and collect the necessary data for location estimation.

3. **Data Collection**: The app collects the Wi-Fi sensing data and prepares it for communication with the Flask server.

4. **Location Estimation**: The app sends the collected data to the Flask server using the established communication interface. The server performs the location estimation algorithm and sends back the estimated location in JSON format.

5. **Displaying the Result**: The app receives the estimated location in JSON format and displays it as text on the device screen.

## Included Files

This repository includes the following files:

- `PermissionSupport.java`: A class that manages permissions and handles the necessary permissions for Wi-Fi sensing.
- Other relevant files specific to the implementation of the Android location estimation test app.

Feel free to modify and customize the above content to fit your project's specific details.
