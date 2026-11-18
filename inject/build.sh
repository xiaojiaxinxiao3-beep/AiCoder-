#!/bin/bash
# Builds work/AiCoderAssist.jar: the injector and everything it injects, in one file.
#
# Needs in work/: client.txt (Mojang's mappings for 1.21.11), cp.txt (the game's library classpath) and
# remapper-cp.txt (tiny-remapper and its libraries). The game jar itself comes from Loom's cache.
set -e
cd "$(dirname "$0")"; unset JAVA_TOOL_OPTIONS; export LC_ALL=C.UTF-8
JD=${JAVA_HOME:-/opt/jdk/jdk-25.0.5+8}/bin
# V=1.21.4 ./build.sh builds for another game version: its files are in work-$V/ (see tools/fetch.py), its
# sources are made from src/ by tools/port_version.py, and the result is work-$V/AiCoderAssist-$V.jar.
V=${V:-1.21.11}
if [ "$V" = 1.21.11 ]; then W=work; SRC=src; OUT=AiCoderAssist; GAME=${GAME_JAR:-$HOME/.gradle/caches/fabric-loom/1.21.11/minecraft-client.jar}; [ -f "$GAME" ] || GAME=$W/minecraft-client.jar
else W=work-$V; SRC=$W/src; OUT=AiCoderAssist-$V; GAME=$W/minecraft-client.jar; [ -f $W/remapper-cp.txt ] || cp work/remapper-cp.txt $W/; fi
ASM=$(tr ':' '\n' < $W/remapper-cp.txt | grep '/asm-9' | head -1)
REMAP="$JD/java -cp $(cat $W/remapper-cp.txt) net.fabricmc.tinyremapper.Main"

# 1. Readable copy of the game to compile against, and ASM moved into our own package so it cannot clash
[ -f $W/mojang.tiny ] || python3 tools/tiny.py $W/client.txt $W/mojang.tiny
[ -f $W/minecraft-named.jar ] || $REMAP "$GAME" $W/minecraft-named.jar $W/mojang.tiny official named > /dev/null
if [ ! -f $W/asm-moved.jar ]; then
  { printf 'v1\tfrom\tto\n'; unzip -Z1 "$ASM" | grep '\.class$' | grep -v module-info | sed 's/\.class$//' | awk '{t=$0; sub("org/objectweb/asm","aicoder/assist/asm",t); print "CLASS\t"$0"\t"t}'; } > $W/asm.tiny
  $REMAP "$ASM" $W/asm-moved.jar $W/asm.tiny from to > /dev/null
fi

# 2. Modules come from the Meteor addon
python3 tools/port_modules.py > /dev/null
[ "$V" = 1.21.11 ] || python3 tools/port_version.py $V $SRC > /dev/null

# 3. Compile against readable names
rm -rf $W/classes $W/named.jar && mkdir -p $W/classes
LIBS=$(tr ':' '\n' < $W/cp.txt | grep -v minecraft-client.jar | tr '\n' ':')
$JD/javac -encoding UTF-8 --release 21 -Xlint:-options -cp "$W/minecraft-named.jar:$W/asm-moved.jar:$LIBS" -d $W/classes $(find $SRC -name '*.java')
$JD/jar cf $W/named.jar -C $W/classes .

# 4. Convert our classes to the game's real (obfuscated) names
rm -f $W/official.jar
$REMAP $W/named.jar $W/official.jar $W/mojang.tiny named official $W/minecraft-named.jar > /dev/null

# 5. One jar: our classes, ASM, the patch targets, and a manifest that makes it both injector and payload
rm -rf $W/pack && mkdir -p $W/pack && (cd $W/pack && unzip -qo ../official.jar && unzip -qo ../asm-moved.jar -x 'META-INF/*')
python3 tools/gen_names.py $W/client.txt $W/pack/aicoder/assist/names.properties $V
cp -r res/. $W/pack/
# A licensed build: LICENSE_URL and LICENSE_PUBKEY (the server's Ed25519 public key, base64 of the X.509
# form; tools/LicenseServer.java prints it) turn the licence check on. Without them there is none.
rm -f $W/pack/aicoder/assist/license.properties
if [ -n "$LICENSE_URL" ] && [ -n "$LICENSE_PUBKEY" ]; then
  printf 'url=%s\npublic-key=%s\n' "$LICENSE_URL" "$LICENSE_PUBKEY" > $W/pack/aicoder/assist/license.properties
  echo "licensed build: $LICENSE_URL"
fi

# 5b. Under Fabric Loader the game's classes carry intermediary names and live in Fabric's own class loader.
#     When the intermediary mappings are there (tools/fetch_fabric.py), a second copy of the client converted
#     to those names goes inside the jar; the agent hands it to Fabric's loader when it finds itself in one.
if [ -f $W/intermediary.tiny ]; then
  python3 tools/fabric_names.py $W/mojang.tiny $W/intermediary.tiny $W/pack/aicoder/assist/names.properties $W/fabric.tiny $W/pack/aicoder/assist/names-intermediary.properties
  rm -f $W/fabric-payload.jar
  $REMAP $W/named.jar $W/fabric-payload.jar $W/fabric.tiny named intermediary $W/minecraft-named.jar > /dev/null
  cp $W/fabric-payload.jar $W/pack/aicoder/assist/fabric-payload.jar
fi
printf 'Main-Class: aicoder.assist.Injector\nAgent-Class: aicoder.assist.Agent\nCan-Retransform-Classes: true\n' > $W/manifest.txt
$JD/jar cfm $W/$OUT.jar $W/manifest.txt -C $W/pack .

# 6. The Windows program: a small launcher with the jar appended (Java reads a jar from the end of a file)
if command -v x86_64-w64-mingw32-gcc > /dev/null; then
  x86_64-w64-mingw32-gcc -O2 -s -municode -DGAME_VERSION=L\"$V\" -o $W/launcher.exe launcher/launcher.c
  cat $W/launcher.exe $W/$OUT.jar > $W/$OUT.exe
fi
ls -la $W/$OUT.jar $W/$OUT.exe
