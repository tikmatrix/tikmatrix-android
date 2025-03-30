./gradlew build
./gradlew packageDebugAndroidTest
Copy-Item app\build\outputs\apk\debug\app-debug.apk C:\Users\Administrator\AppData\Roaming\com.tikmatrix\bin\com.github.tikmatrix.apk
Copy-Item app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk C:\Users\Administrator\AppData\Roaming\com.tikmatrix\bin\com.github.tikmatrix.test.apk
# igmatrix
Copy-Item app\build\outputs\apk\debug\app-debug.apk C:\Users\Administrator\AppData\Roaming\com.igmatrix\bin\com.github.tikmatrix.apk
Copy-Item app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk C:\Users\Administrator\AppData\Roaming\com.igmatrix\bin\com.github.tikmatrix.test.apk

