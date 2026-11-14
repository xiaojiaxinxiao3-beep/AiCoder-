#!/bin/bash
# Builds aicoder-payload.jar (the code that runs inside the game, plus the injector's main class).
cd "$(dirname "$0")"; unset JAVA_TOOL_OPTIONS; JD=/opt/jdk/jdk-25.0.5+8/bin
ASM=/root/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.10.1/ada2141c0cc52ee8f5c48cd5fa4ce0e794f22236/asm-9.10.1.jar
$JD/javac --release 21 -cp $ASM -d build/classes $(find src -name '*.java') || exit 1
(cd build/classes && unzip -qo $ASM -x 'META-INF/*' module-info.class)
$JD/jar cfm build/aicoder-payload.jar build/manifest.txt -C build/classes .
