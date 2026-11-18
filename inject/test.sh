#!/bin/bash
# PAYLOAD=work/AiCoderAssist.exe ./test.sh ... injects with the Windows file instead of the jar.
# Starts an unmodified Minecraft 1.21.11 (official jar, no mod loader) on a virtual display, waits for it to
# load, then injects work/AiCoderAssist.jar.   ./test.sh [extra game arguments, e.g. --quickPlaySingleplayer "New World"]
cd "$(dirname "$0")"; unset JAVA_TOOL_OPTIONS; export LC_ALL=C.UTF-8
JD=${JAVA_HOME:-/opt/jdk/jdk-25.0.5+8}/bin
export XDG_RUNTIME_DIR=/tmp/xdg-rt LIBGL_ALWAYS_SOFTWARE=1 DISPLAY=:77
# V=1.21.4 ./test.sh tests the build for another version (files from tools/fetch.py in work-$V/)
V=${V:-1.21.11}
if [ "$V" = 1.21.11 ]; then W=$PWD/work; JAR=AiCoderAssist.jar; INDEX=$(cat $W/asset-index.txt 2>/dev/null || echo 1.21.11-29); else W=$PWD/work-$V; JAR=AiCoderAssist-$V.jar; INDEX=$(cat $W/asset-index.txt); fi
mkdir -p $XDG_RUNTIME_DIR $W/game; chmod 700 $XDG_RUNTIME_DIR
[ -f $W/game/options.txt ] || printf 'onboardAccessibility:false\ntutorialStep:none\nskipMultiplayerWarning:true\nrenderDistance:3\nmaxFps:60\npauseOnLostFocus:false\n' > $W/game/options.txt
pkill -f "net.minecraft.client.main.[M]ain"; pkill -f "knot.[K]notClient"; sleep 2
pgrep -f "Xvfb :77" >/dev/null || { (Xvfb :77 -screen 0 1280x720x24 +extension GLX +render -noreset >/dev/null 2>&1 &); sleep 1; }
# FABRIC=1 starts the same game through Fabric Loader (files from tools/fetch_fabric.py)
CP=$W/cp.txt; MAIN=net.minecraft.client.main.Main
if [ -n "$FABRIC" ]; then CP=$W/fabric-cp.txt; MAIN=$(cat $W/fabric-main.txt); fi
(cd $W/game && nohup setsid $JD/java -Xmx2G $GAME_JVM_ARGS -cp "$(cat $CP)" $MAIN --version $V --accessToken 0 --username Tester --gameDir . \
  --assetsDir $HOME/.gradle/caches/fabric-loom/assets --assetIndex $INDEX --width ${GW:-1280} --height ${GH:-720} "$@" > $W/game.log 2>&1 < /dev/null &)
for i in $(seq 1 90); do sleep 2; grep -qE "OpenAL|Sound engine|Exception in thread|logged in with entity" $W/game.log && break; done
sleep ${SETTLE:-14}
$JD/java -jar ${PAYLOAD:-$W/$JAR} $INJECT_ARGS 2>&1 | head -6
sleep 3; grep -n "AiCoder Assist\]\|VerifyError\|at aicoder" $W/game.log | tail -8 | cut -c1-200
