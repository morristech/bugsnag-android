#!/bin/bash

function launch_emulator() {
  cd $ANDROID_HOME/tools
  echo "Launching Android Emulator"

  # see https://developer.android.com/studio/run/emulator-commandline.html#starting
  emulator @NexusEmulator -verbose -no-boot-anim -noaudio -no-snapshot -no-window &
  EMULATOR_PID=$!
  echo "Waiting for device..."
  adb wait-for-device
  echo "Device ready!"
}

function build_test_app() {
  echo "Running Android Tests"
  rm $APK_FILE
  cd /opt/hugsnag-android
  ./gradlew testharness:assembleRelease
  cd /opt
}

function poll_app() {
  # Detect whether app is still in the foreground (app kills its own process when completed)
  while [[ `adb shell dumpsys activity | grep "Proc # 0" | grep "com.bugsnag.android.hugsnag"` ]];
   do echo "Polling Android App"
   sleep 2
  done
}

function launch_test_app() {
  echo "Launching test app"
  adb devices
  echo "uninstalling previous app"
  adb uninstall com.bugsnag.android.hugsnag
  adb install $APK_FILE
  adb shell am start -n com.bugsnag.android.hugsnag/com.bugsnag.android.MainActivity
  echo "Started MainActivity"
  poll_app

  echo "Killing app process"
  adb shell am force-stop com.bugsnag.android.hugsnag

  # launch again, with session sending enabled
  sleep 2 # wait for app to close
  echo "Launching MainActivity again"
  adb shell am start -n com.bugsnag.android.hugsnag/com.bugsnag.android.MainActivity --ez sendSessions true
  poll_app
  echo "Android App finished"
}

function on_exit() {
  echo "Cleaning up Android Env"
  kill -9 $EMULATOR_PID
}

APK_FILE="/opt/hugsnag-android/testharness/build/outputs/apk/release/testharness-release.apk"

trap on_exit EXIT
launch_emulator
build_test_app
launch_test_app
on_exit
