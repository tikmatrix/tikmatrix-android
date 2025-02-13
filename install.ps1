# adb push app\build\libs\app-test.jar /data/local/tmp
# adb push app\build\compile_app
# adb shell am instrument -w -r -e debug true -e class com.github.tikmatrix.stub.Stub com.github.tikmatrix.test/androidx.test.runner.AndroidJUnitRunner CLASSPATH=/data/local/tmp/app-test.jar / com.github.tikmatrix.stub.Stub
adb install -r -t app\build\outputs\apk\debug\app-debug.apk
adb install -r -t app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w -r -e debug false -e class com.github.tikmatrix.stub.Stub com.github.tikmatrix.test/androidx.test.runner.AndroidJUnitRunner