#!/bin/sh
# Linux/macOS equivalent of build.ps1. Uses sdk/zorynth-plugin-api-1.jar when present,
# otherwise compiles the compile-only stand-in in sdk-stub/ (never packaged).
set -e
cd "$(dirname "$0")"
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"; JAR="${JAVA_HOME:+$JAVA_HOME/bin/}jar"
rm -rf build && mkdir -p build/classes build/sdk
SDK=sdk/zorynth-plugin-api-1.jar
if [ ! -f "$SDK" ]; then
  "$JAVAC" --release 25 -encoding UTF-8 -d build/sdk sdk-stub/dev/zorynth/api/PluginApi.java
  SDK=build/sdk
fi
"$JAVAC" --release 25 -encoding UTF-8 -classpath "$SDK" -d build/classes $(find src/main/java -name '*.java')
"$JAR" --create --file build/aicoder-themes-1.0.0.jar -C build/classes . -C src/main/resources .
echo "Built build/aicoder-themes-1.0.0.jar"
