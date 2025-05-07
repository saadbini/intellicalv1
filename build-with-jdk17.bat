@echo off
echo Setting up environment for building with JDK 17...

REM Save current JAVA_HOME
set ORIGINAL_JAVA_HOME=%JAVA_HOME%

REM Check if JDK 17 is installed in common locations
if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot" (
    set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot
    goto :found_java
)

if exist "C:\Program Files\Java\jdk-17" (
    set JAVA_HOME=C:\Program Files\Java\jdk-17
    goto :found_java
)

if exist "C:\Program Files\Eclipse Adoptium\jdk-17" (
    set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17
    goto :found_java
)

REM If we get here, we didn't find JDK 17
echo ERROR: Could not find JDK 17 installation.
echo Please install JDK 17 or edit this script to point to your JDK 17 installation.
exit /b 1

:found_java
echo Using JDK at: %JAVA_HOME%
echo.

REM Run the build
call gradlew.bat %*

REM Restore original JAVA_HOME
set JAVA_HOME=%ORIGINAL_JAVA_HOME%

echo.
echo Build completed. JAVA_HOME restored to: %JAVA_HOME%
