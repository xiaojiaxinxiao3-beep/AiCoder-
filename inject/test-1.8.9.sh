#!/bin/bash
# Starts an unmodified Minecraft 1.8.9 on a virtual display, joined to a local 1.8.9 server with an empty
# world (the old game has no way to go straight into a single-player world), then injects
# work-1.8.9/AiCoderAssist-1.8.9.jar. Commands for the world go to the server:  srv "tp Tester 0 101 0"
# The game runs on Java 8 (JAVA8), the injector on a newer Java, as on a player's computer.
cd "$(dirname "$0")"; unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS; export LC_ALL=C.UTF-8
JD=${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}/bin
J8=${JAVA8:-/usr/lib/jvm/java-8-openjdk-amd64}/bin
export XDG_RUNTIME_DIR=/tmp/xdg-rt LIBGL_ALWAYS_SOFTWARE=1 DISPLAY=:77
W=$PWD/work-1.8.9
mkdir -p $XDG_RUNTIME_DIR $W/game $W/server; chmod 700 $XDG_RUNTIME_DIR
[ -f $W/game/options.txt ] || printf 'renderDistance:4\nmaxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\n' > $W/game/options.txt
pkill -f "net.minecraft.client.main.[M]ain"; pkill -f "launchwrapper.[L]aunch"; pkill -f "[s]erver.jar nogui"; sleep 2
pgrep -f "Xvfb :77" >/dev/null || { (Xvfb :77 -screen 0 1280x720x24 +extension GLX +render -noreset >/dev/null 2>&1 &); sleep 1; }
rm -f $W/server/in; mkfifo $W/server/in
(cd $W/server && nohup setsid bash -c "tail -f in | $J8/java -Xmx1G -jar ../server.jar nogui" > $W/server.log 2>&1 < /dev/null &)
srv() { echo "$1" > $W/server/in; sleep ${2:-0.4}; }
for i in $(seq 1 60); do sleep 1; grep -q "Done (" $W/server.log && break; done
srv "gamerule doDaylightCycle false"; srv "time set 6000"; srv "weather clear 999999"
CP=$W/cp.txt; MAIN=net.minecraft.client.main.Main
# OPTIFINE=1 starts the same game the way OptiFine's launcher profile does (files from tools/fetch_optifine.sh)
if [ -n "$OPTIFINE" ]; then CP=$W/optifine-cp.txt; MAIN="net.minecraft.launchwrapper.Launch"; set -- --tweakClass optifine.OptiFineTweaker "$@"; fi
(cd $W/game && nohup setsid $J8/java -Xmx1G -Djava.library.path=$W/natives -Djavax.accessibility.assistive_technologies=" " $GAME_JVM_ARGS -cp "$(cat $CP)" $MAIN --version 1.8.9 --accessToken 0 --username Tester --gameDir . \
  --assetsDir $HOME/.gradle/caches/fabric-loom/assets --assetIndex $(cat $W/asset-index.txt) --userProperties '{}' --width ${GW:-1280} --height ${GH:-720} \
  --server 127.0.0.1 --port 25589 "$@" > $W/game.log 2>&1 < /dev/null &)
for i in $(seq 1 90); do sleep 2; grep -q "Tester joined the game" $W/server.log && break; grep -q "Exception in thread" $W/game.log && break; done
sleep ${SETTLE:-8}
$JD/java -jar ${PAYLOAD:-$W/AiCoderAssist-1.8.9.jar} $INJECT_ARGS 2>&1 | head -8
sleep 3; grep -n "AiCoder Assist\]\|VerifyError\|at aicoder\|Error" $W/game.log | tail -8 | cut -c1-220
SP=/tmp/claude-0/-home-user/8fd84529-c46a-5acd-8a37-bac870386892/scratchpad
click() { xdotool mousemove $(($1-3)) $(($2-2)); sleep 0.15; xdotool mousemove $1 $2; sleep 0.3; xdotool mousedown ${3:-1}; sleep 0.2; xdotool mouseup ${3:-1}; sleep ${4:-0.8}; }
