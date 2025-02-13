#!/usr/bin/env bash
./gradlew build
./gradlew packageDebugAndroidTest
mv app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/com.github.tikmatrix.apk
mv app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk app/build/outputs/apk/com.github.tikmatrix.test.apk
