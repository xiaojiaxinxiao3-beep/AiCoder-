#!/bin/sh
# Linux/macOS equivalent of build.ps1.
set -e
cd "$(dirname "$0")"
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"; JAR="${JAVA_HOME:+$JAVA_HOME/bin/}jar"
rm -rf build && mkdir -p build/classes
"$JAVAC" --release 25 -encoding UTF-8 -Xlint:all -classpath sdk/zorynth-plugin-api-1.jar -d build/classes $(find src/main/java -name '*.java')
"$JAR" --create --file build/aicoder-themes-1.0.0.jar -C build/classes . -C src/main/resources .
echo "Built build/aicoder-themes-1.0.0.jar"
