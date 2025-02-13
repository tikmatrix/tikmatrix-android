# Purpose

Support Android 5.0+
[UIAutomator](http://developer.android.com/tools/testing/testing_ui.html) is a
great tool to perform Android UI testing, but to do it, you have to write java
code, compile it, install the jar, and run. It's a complex steps for all
testers...

This project is to build a light weight jsonrpc server in Android device, so
that we can just write PC side script to write UIAutomator tests.

# Github Actions

## R2_ACCOUNT_ID

Your Cloudflare account ID.

## R2_ACCESS_KEY_ID

Your Cloudflare R2 bucket access key ID.

## R2_SECRET_ACCESS_KEY

Your Cloudflare R2 bucket secret access key.

# Build

- Run command:

```bash
./gradlew build
./gradlew packageDebugAndroidTest
mv app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/com.github.tikmatrix.apk
mv app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk app/build/outputs/apk/com.github.tikmatrix.test.apk
```

- Run the jsonrpc server on Android device

```bash
./gradlew cC
adb forward tcp:9008 tcp:9008 # tcp forward
```

If debug apk already installed, There is no need to use gradle.

simply run the following command

```
adb forward tcp:9008 tcp:9008
adb shell am instrument -w -r -e debug false -e class com.github.tikmatrix.stub.Stub com.github.tikmatrix.test/androidx.test.runner.AndroidJUnitRunner
adb shell am startservice com.github.tikmatrix/.Service

```

# Run

```bash
$ curl -X POST -d '{"jsonrpc": "2.0", "id": "1f0f2655716023254ed2b57ba4198815", "method": "deviceInfo", "params": {}}' 'http://127.0.0.1:9008/jsonrpc/0'
{'currentPackageName': 'com.smartisanos.launcher',
 'displayHeight': 1920,
 'displayRotation': 0,
 'displaySizeDpX': 360,
 'displaySizeDpY': 640,
 'displayWidth': 1080,
 'productName': 'surabaya',
 'screenOn': True,
 'sdkInt': 23,
 'naturalOrientation': True}
```

# The buildin input method

**Fast input method**

Encode the text into UTF-8 and then Base64

For example:

    "Hello 你好" -> (UTF-8 && Base64) = SGVsbG8g5L2g5aW9

Send to FastInputIME with broadcast

```bash
# Append text to input field
$ adb shell am broadcast -a ADB_INPUT_TEXT --es text SGVsbG8g5L2g5aW9

# Clear text
$ adb shell am broadcast -a ADB_CLEAR_TEXT

# Clear text before append text
$ adb shell am broadcast -a ADB_SET_TEXT --es text SGVsbG8g5L2g5aW9

# Send keycode, eg: ENTER
$ adb shell am broadcast -a ADB_INPUT_KEYCODE --ei code 66

# Send Editor code
$ adb shell am broadcast -a ADB_EDITOR_CODE --ei code 2 # IME_ACTION_GO

# Get clipboard (without data)
$ adb shell am broadcast -a ADB_GET_CLIPBOARD
Broadcasting: Intent { act=ADB_GET_CLIPBOARD flg=0x400000 }
Broadcast completed: result=0

# Get clipboard (with data, base64 encoded)
$ adb shell am broadcast -a ADB_GET_CLIPBOARD
Broadcasting: Intent { act=ADB_GET_CLIPBOARD flg=0x400000 }
Broadcast completed: result=-1, data="5LqG6Kej5Lyg57uf5paH5YyW"
```

- [Editor Code](https://developer.android.com/reference/android/view/inputmethod/EditorInfo)
- [Key Event](https://developer.android.com/reference/android/view/KeyEvent)

# Change GPS mock location

You can change mock location from terminal using adb in order to test GPS on real devices.

```
adb [-s <specific device>] shell am broadcast -a send.mock [-e lat <latitude>] [-e lon <longitude>]
        [-e alt <altitude>] [-e accurate <accurate>]
```

For example:

```
adb  shell am broadcast -a send.mock -e lat 15.3 -e lon 99
```

## Show toast

```
adb shell am start -n com.github.tikmatrix/.ToastActivity -e message hello
```

## Float window

```
adb shell am start -n com.github.tikmatrix/.ToastActivity -e showFloatWindow true # show
adb shell am start -n com.github.tikmatrix/.ToastActivity -e showFloatWindow false # hide
```

## Change language and timezone

```
adb shell am start -n com.github.tikmatrix/.ToastActivity --es language en --es timezone Europe/London
```

# How to generate changelog

[conventional-changelog](https://github.com/conventional-changelog/conventional-changelog/tree/master/packages/conventional-changelog-cli)

```bash
npm install -g conventional-changelog-cli
conventional-changelog -p grunt -i CHANGELOG.md -s -r 0
```

# Dependencies

- [nanohttpd](https://github.com/NanoHttpd/nanohttpd)
- [jsonrpc4j](https://github.com/briandilley/jsonrpc4j)
- [jackson](https://github.com/FasterXML/jackson)
- [androidx.test.uiautomator](https://mvnrepository.com/artifact/androidx.test.uiautomator/uiautomator-v18)

# Added features

- [x] support unicode input

# Resources

- [Google UiAutomator Tutorial](https://developer.android.com/training/testing/ui-testing/uiautomator-testing?hl=zh-cn)
- [Google UiAutomator API](https://developer.android.com/reference/android/support/test/uiautomator/package-summary?hl=zh-cn)
- [Maven repository of uiautomator-v18](https://mvnrepository.com/artifact/androidx.test.uiautomator/uiautomator-v18)
