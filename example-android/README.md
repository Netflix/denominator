# Denominator Android Example

## Setup
You will need to use either [Android Studio](http://developer.android.com/sdk/installing/studio.html) or the command-line tools to install this.  The easiest way to get started is by using homebrew to install the android sdk.
1. `brew install android`
2. `android update sdk --no-ui`
3. `export ANDROID_HOME=/usr/local/opt/android-sdk`
4. `echo sdk.dir=$ANDROID_HOME >>local.properties`

## Build
`gradle clean assemble` to build the android package.

## Install on your device
Use adb to install a new copy of the example onto your connected device.
```
adb install -r examples/denominator-example-android/build/apk/denominator-example-android-debug-unaligned.apk
```

## Running
If the configured provider has credentials, you can enter them via the menu button.  On refresh, a zone list should be emitted.  You can use `adb logcat` to view any stack traces as necessary.
