<h1 align="center">
  Playfinity Starter Android (Football and Smartball)
</h1>

<p align="center">
    This sample application is designed to assist customers in getting started with Playfinity hardware, including Smartball and Football.
</p>

<p align="center">
    <a href="https://playfinity.com/">Playfinity.com</a>
    :heavy_check_mark:
    <a href="https://www.facebook.com/profile.php?id=100068605786408">Facebook</a>
    :heavy_check_mark:
    <a href="https://www.instagram.com/playfinity_io/">Instagram</a>
    :heavy_check_mark:
    <a href="https://www.youtube.com/channel/UCiJSGEIcw_-OHlYjTOeJ-Ag/featured/">Youtube</a>
</p>

## Setup

<details open><summary><b>Compatibility 🔄</b></summary>
<p>
  
This application targets `Android SDK 21` (Lollipop) and newer.
To successfully discover Playfinity devices make sure that your device supports Bluetooth Low Energy (BLE).

</p>
</details>

> [!TIP]
> Android SDK configuration can be found in root `build.gradle` file.

-------

### What's included :rocket:
- Support for Football
- Support for Smartball
- Listening and reacting to console events and raw data
- Coded in Kotlin

# Preview

## Screens
![starter](https://github.com/Playfinity/playfinity-recorder-android/assets/8034071/989b9534-a990-40dd-b1cf-1bd9e59359e5)

# How to use

## Setup

`AndroidManifest.xml`

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

## Console mode (Smartball)

Playfinity Console can operate in a several different modes. For example it can act like a ball or a trampoline ankle band.<br>
While initializing Playfinity SDK we can choose what mode to use:

```kotlin
PlayfinitySDKBuilder()
    .enableLogging(BuildConfig.DEBUG)
    .addCallback(this)
    .build(this, io.playfinity.sdk.device.SensorType.Ball)
```

> [!TIP]
> Please note that the console will produce different events `io.playfinity.sdk.SensorEventType` while working under specific mode.

### 1. `typeBall`
Puts the console into `Ball` mode.<br>
Events: `Throw`, `Catch`, `Miss`.

### 2. `typeTrampoline`
Puts the console into `Trampoline` mode.<br>
Events: `Jump`, `Land`.

### 3. `typeTrix`
Puts the console into `Football` mode, to provide football-like experience.<br>
Events: `Kick`, `Bounce`, `Miss`.

# License

    Copyright 2024 Playfinity.com

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
