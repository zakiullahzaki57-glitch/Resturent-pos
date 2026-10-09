#!/bin/bash
set -e
cp MainActivity.java android/app/src/main/java/com/myrestaurant/pos/MainActivity.java
M=android/app/src/main/AndroidManifest.xml
sed -i 's|<application|<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />\n    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />\n    <application|' $M
grep -c BLUETOOTH_CONNECT $M
