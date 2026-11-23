$ErrorActionPreference = 'Stop'
$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$tools = Join-Path $sdk 'build-tools\36.0.0'
$jar = Join-Path $sdk 'platforms\android-37.0\android.jar'
$java = 'C:\Program Files\Java\jdk-21\bin'
$out = Join-Path $PSScriptRoot 'build\local'
New-Item -ItemType Directory -Force "$out\classes", "$out\dex" | Out-Null
$manifest = (Get-Content 'app\src\main\AndroidManifest.xml' -Raw).Replace('<manifest ', '<manifest package="ru.devops.interview" android:versionCode="2" android:versionName="1.1" ').Replace('<application ', '<uses-sdk android:minSdkVersion="23" android:targetSdkVersion="34"/><application ')
[IO.File]::WriteAllText("$out\AndroidManifest.xml", $manifest, [Text.UTF8Encoding]::new($false))
& "$tools\aapt.exe" package -f -M "$out\AndroidManifest.xml" -I $jar -A 'app\src\main\assets' -F "$out\unsigned.apk"
if ($LASTEXITCODE) { throw 'aapt failed' }
& "$java\javac.exe" -encoding UTF-8 --release 8 -classpath $jar -d "$out\classes" 'app\src\main\java\ru\devops\interview\MainActivity.java' 'app\src\main\java\ru\devops\interview\AnswerAnalyzer.java'
if ($LASTEXITCODE) { throw 'javac failed' }
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$classes = Get-ChildItem "$out\classes" -Filter '*.class' -Recurse | ForEach-Object FullName
& "$tools\d8.bat" --min-api 23 --lib $jar --output "$out\dex" $classes
if ($LASTEXITCODE) { throw 'd8 failed' }
Push-Location "$out\dex"
try { & "$tools\aapt.exe" add "$out\unsigned.apk" classes.dex; if ($LASTEXITCODE) { throw 'aapt add failed' } } finally { Pop-Location }
& "$tools\zipalign.exe" -f 4 "$out\unsigned.apk" "$out\aligned.apk"
if ($LASTEXITCODE) { throw 'zipalign failed' }
if (!(Test-Path "$out\debug.keystore")) { & "$java\keytool.exe" -genkeypair -keystore "$out\debug.keystore" -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug,O=Android,C=US' -keyalg RSA -validity 10000; if ($LASTEXITCODE) { throw 'keytool failed' } }
& "$tools\apksigner.bat" sign --ks "$out\debug.keystore" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$out\devops-interview-debug.apk" "$out\aligned.apk"
if ($LASTEXITCODE) { throw 'signing failed' }
& "$tools\apksigner.bat" verify "$out\devops-interview-debug.apk"
if ($LASTEXITCODE) { throw 'signature verification failed' }
Write-Host "APK: $out\devops-interview-debug.apk"
