$env:JAVA_HOME = 'C:\Users\admin\.jdks\openjdk-25.0.2'
$env:Path = $env:JAVA_HOME + '\bin;' + $env:Path
$env:ANDROID_HOME = 'C:\Users\admin\AppData\Local\Android\Sdk'
& .\gradlew.bat installDebug
