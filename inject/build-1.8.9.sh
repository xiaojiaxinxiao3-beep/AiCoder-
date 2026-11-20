#!/bin/bash
# Builds work-1.8.9/AiCoderAssist-1.8.9.jar (and .exe): the client for Minecraft 1.8.9, plain or with OptiFine.
#
# First, once:  python3 tools/fetch.py 1.8.9 work-1.8.9   (the game, its libraries, its assets)
# The name tables (MCP) and the Java 8 downgrader are fetched here if they are missing.
#
# The sources are src/ with src-1.8.9/ laid over it (tools/port_version.py). They are compiled with the
# same compiler as the other versions and then turned into Java 8 class files, which is what a 1.8.9 game
# runs on; the few newer library calls they make are answered by classes the downgrader adds to the jar.
set -e
cd "$(dirname "$0")"; unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS; export LC_ALL=C.UTF-8
JD=${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}/bin
V=1.8.9; W=work-$V; SRC=$W/src; OUT=AiCoderAssist-$V; GAME=$W/minecraft-client.jar
[ -f $W/remapper-cp.txt ] || cp work/remapper-cp.txt $W/
ASM=$(tr ':' '\n' < $W/remapper-cp.txt | grep '/asm-9' | head -1)
REMAP="$JD/java -cp $(cat $W/remapper-cp.txt) net.fabricmc.tinyremapper.Main"
FORGE=https://maven.minecraftforge.net/de/oceanlabs/mcp

# 1. Names: MCP's tables, the type of every field from the game jar, and a readable copy of the game
[ -f $W/joined.srg ] || { curl -sfL -o $W/mcp-srg.zip $FORGE/mcp/$V/mcp-$V-srg.zip && unzip -qo $W/mcp-srg.zip joined.srg -d $W; }
[ -f $W/methods.csv ] || { curl -sfL -o $W/mcp-stable.zip $FORGE/mcp_stable/22-$V/mcp_stable-22-$V.zip && unzip -qo $W/mcp-stable.zip -d $W; }
if [ ! -f $W/mcp.tiny ]; then
  mkdir -p $W/tools && $JD/javac -cp "$ASM" -d $W/tools tools/java/FieldDump.java
  $JD/java -cp "$ASM:$W/tools" FieldDump "$GAME" $W/fields.txt
  python3 tools/mcp_names.py $W
fi
[ -f $W/minecraft-named.jar ] || $REMAP "$GAME" $W/minecraft-named.jar $W/mcp.tiny official named > /dev/null
if [ ! -f $W/asm-moved.jar ]; then
  { printf 'v1\tfrom\tto\n'; unzip -Z1 "$ASM" | grep '\.class$' | grep -v module-info | sed 's/\.class$//' | awk '{t=$0; sub("org/objectweb/asm","aicoder/assist/asm",t); print "CLASS\t"$0"\t"t}'; } > $W/asm.tiny
  $REMAP "$ASM" $W/asm-moved.jar $W/asm.tiny from to > /dev/null
fi
[ -f $W/jvmdg.jar ] || curl -sfL -o $W/jvmdg.jar https://repo1.maven.org/maven2/xyz/wagyourtail/jvmdowngrader/jvmdowngrader/1.3.6/jvmdowngrader-1.3.6-all.jar

# 2. Sources: the modules from the Meteor addon, then the 1.8.9 tree
python3 tools/port_modules.py > /dev/null
python3 tools/port_version.py $V $SRC > /dev/null

# 3. Compile against readable names
rm -rf $W/classes $W/named.jar && mkdir -p $W/classes
LIBS=$(tr ':' '\n' < $W/cp.txt | grep -v minecraft-client.jar | tr '\n' ':')
$JD/javac -encoding UTF-8 --release 21 -Xlint:-options -cp "$W/minecraft-named.jar:$W/asm-moved.jar:$LIBS" -d $W/classes $(find $SRC -name '*.java')
$JD/jar cf $W/named.jar -C $W/classes .

# 4. To the game's real (obfuscated) names, then to Java 8 class files
rm -f $W/official.jar $W/java8.jar
$REMAP $W/named.jar $W/official.jar $W/mcp.tiny named official $W/minecraft-named.jar > /dev/null
$JD/java -jar $W/jvmdg.jar -c 52 downgrade --classpath "$GAME:$W/asm-moved.jar:$LIBS" --target $W/official.jar - shade --prefix aicoder/assist/jvmdg --target - $W/java8.jar

# 5. One jar: our classes, ASM, the patch targets, and a manifest that makes it both injector and payload
rm -rf $W/pack && mkdir -p $W/pack && (cd $W/pack && unzip -qo ../java8.jar && unzip -qo ../asm-moved.jar -x 'META-INF/*')
python3 tools/gen_names_189.py $W/mcp.tiny $W/pack/aicoder/assist/names.properties
cp -r res/. $W/pack/
rm -f $W/pack/aicoder/assist/license.properties
printf 'Main-Class: aicoder.assist.Injector\nAgent-Class: aicoder.assist.Agent\nCan-Retransform-Classes: true\n' > $W/manifest.txt
$JD/jar cfm $W/$OUT.jar $W/manifest.txt -C $W/pack .

# 6. The Windows program: a small launcher with the jar appended
if command -v x86_64-w64-mingw32-gcc > /dev/null; then
  x86_64-w64-mingw32-gcc -O2 -s -municode -DGAME_VERSION=L\"$V\" -o $W/launcher.exe launcher/launcher.c
  cat $W/launcher.exe $W/$OUT.jar > $W/$OUT.exe
fi
ls -la $W/$OUT.jar $W/$OUT.exe
