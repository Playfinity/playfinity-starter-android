# Playfinity API Sample 

The SDK Sample is here to help customers get up and running with Playfinity SDK.

-------

<p align="center">
    <a href="https://playfinity.io/">Playfinity.io</a>
    :heavy_check_mark:
    <a href="https://www.facebook.com/playfinity.io/">Facebook</a>
    :heavy_check_mark:
    <a href="https://www.instagram.com/playfinity_io/">Instagram</a>
    :heavy_check_mark:
    <a href="https://www.youtube.com/channel/UCiJSGEIcw_-OHlYjTOeJ-Ag/featured/">Youtube</a>
</p>

-------

### What's included :rocket:
- Playfinity SDK embedded
- Playfinity console discovering and pairing (BLE)
- Listening and reacting to console events
- Sounds
- Ball throwing sample
- Trampoline jumping sample
- Trick ball sample
- Cloud transporter
- Coded in Kotlin

# Preview

## Screenshots 
![Image](https://github.com/Playfinity/playfinity-recorder-android/blob/feature/playfinity-api-sample/DEV/screenshots/screenshots_01.png)

# How to use

## Requirements 

This application targets Android (5.0) Lollipop and newer.<br>
To successfully discover Playfinity Console make sure that your device supports Bluetooth Low Energy (BLE).

## Setup

Make sure to add this lines to your `AndroidManifest.xml`.

```xml
<meta-data
    android:name="io.playfinity.sdk.applicationKey"
    android:value="SAMPLE_TEST" />
<meta-data
    android:name="io.playfinity.sdk.developerId"
    android:value="SAMPLE_TEST" />
<meta-data
    android:name="io.playfinity.sdk.apiUrl"
    android:value="https://apps.playfinity.io" />
<meta-data
    android:name="io.playfinity.sdk.cloudApiUrl"
    android:value="https://cloud.playfinity.io" />
```

## Console mode

Playfinity Console can operate in several different modes. For example it can act like a ball or a trampoline device.<br>
While initializing Playfinity SDK we can choose what mode to use:

```kotlin
PlayfinitySDKBuilder()
    .enableLogging(BuildConfig.DEBUG)
    .addCallback(this)
    .build(this, io.playfinity.sdk.device.SensorType.Ball)
```

:information_source: Please note that the console will produce different events `io.playfinity.sdk.SensorEventType` while working under specific mode.

## Build variants

### 1. `typeBall`
Puts the console into `Ball` mode.<br>
Events: `Throw`, `Catch`, `Miss`.

### 2. `typeTrampoline`
Puts the console into `Trampoline` mode.<br>
Events: `Jump`, `Land`.

### 3. `typeTrix`
Puts the console into `Football` mode, to provide football-like experience.<br>
Events: `Kick`, `Bounce`, `Miss`.

## Cloud Transporder :cloud:
Playfinity Cloud Transporder enables an option to send console events to a specified endpoint in real time.<br>
To toggle it, you need to grand a custom permission `Playfinity Developer Mode` in your application settings.

To change endpoint url replace:

```
<meta-data
    android:name="io.playfinity.sdk.cloudApiUrl"
    android:value="https://cloud.playfinity.io" />
```

with your own URL.

# License

    Copyright 2020 Playfinity.io

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
