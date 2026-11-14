#!/bin/bash
# Builds work/AiCoderAssist.jar: the injector and everything it injects, in one file.
#
# Needs in work/: client.txt (Mojang's mappings for 1.21.11), cp.txt (the game's library classpath) and
# remapper-cp.txt (tiny-remapper and its libraries). The game jar itself comes from Loom's cache.
set -e
cd "$(dirname "$0")"; unset JAVA_TOOL_OPTIONS; export LC_ALL=C.UTF-8
JD=${JAVA_HOME:-/opt/jdk/jdk-25.0.5+8}/bin
GAME=${GAME_JAR:-$HOME/.gradle/caches/fabric-loom/1.21.11/minecraft-client.jar}
ASM=$(tr ':' '\n' < work/remapper-cp.txt | grep '/asm-9' | head -1)
REMAP="$JD/java -cp $(cat work/remapper-cp.txt) net.fabricmc.tinyremapper.Main"
W=work

# 1. Readable copy of the game to compile against, and ASM moved into our own package so it cannot clash
[ -f $W/mojang.tiny ] || python3 tools/tiny.py $W/client.txt $W/mojang.tiny
[ -f $W/minecraft-named.jar ] || $REMAP "$GAME" $W/minecraft-named.jar $W/mojang.tiny official named > /dev/null
if [ ! -f $W/asm-moved.jar ]; then
  { printf 'v1\tfrom\tto\n'; unzip -Z1 "$ASM" | grep '\.class$' | grep -v module-info | sed 's/\.class$//' | awk '{t=$0; sub("org/objectweb/asm","aicoder/assist/asm",t); print "CLASS\t"$0"\t"t}'; } > $W/asm.tiny
  $REMAP "$ASM" $W/asm-moved.jar $W/asm.tiny from to > /dev/null
fi

# 2. Modules come from the Meteor addon
python3 tools/port_modules.py > /dev/null

# 3. Compile against readable names
rm -rf $W/classes $W/named.jar && mkdir -p $W/classes
LIBS=$(tr ':' '\n' < $W/cp.txt | grep -v minecraft-client.jar | tr '\n' ':')
$JD/javac -encoding UTF-8 --release 21 -Xlint:-options -cp "$W/minecraft-named.jar:$W/asm-moved.jar:$LIBS" -d $W/classes $(find src -name '*.java')
$JD/jar cf $W/named.jar -C $W/classes .

# 4. Convert our classes to the game's real (obfuscated) names
rm -f $W/official.jar
$REMAP $W/named.jar $W/official.jar $W/mojang.tiny named official $W/minecraft-named.jar > /dev/null

# 5. One jar: our classes, ASM, the patch targets, and a manifest that makes it both injector and payload
rm -rf $W/pack && mkdir -p $W/pack && (cd $W/pack && unzip -qo ../official.jar && unzip -qo ../asm-moved.jar -x 'META-INF/*')
python3 tools/gen_names.py $W/client.txt $W/pack/aicoder/assist/names.properties
printf 'Main-Class: aicoder.assist.Injector\nAgent-Class: aicoder.assist.Agent\nCan-Retransform-Classes: true\n' > $W/manifest.txt
$JD/jar cfm $W/AiCoderAssist.jar $W/manifest.txt -C $W/pack .
ls -la $W/AiCoderAssist.jar
