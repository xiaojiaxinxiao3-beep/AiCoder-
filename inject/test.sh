#!/bin/bash
# Starts an unmodified Minecraft 1.21.11 (official jar, no mod loader) on a virtual display, waits for it to
# load, then injects work/AiCoderAssist.jar.   ./test.sh [extra game arguments, e.g. --quickPlaySingleplayer "New World"]
cd "$(dirname "$0")"; unset JAVA_TOOL_OPTIONS; export LC_ALL=C.UTF-8
JD=${JAVA_HOME:-/opt/jdk/jdk-25.0.5+8}/bin
export XDG_RUNTIME_DIR=/tmp/xdg-rt LIBGL_ALWAYS_SOFTWARE=1 DISPLAY=:77
mkdir -p $XDG_RUNTIME_DIR work/game; chmod 700 $XDG_RUNTIME_DIR
[ -f work/game/options.txt ] || printf 'onboardAccessibility:false\ntutorialStep:none\nskipMultiplayerWarning:true\nrenderDistance:3\nmaxFps:60\npauseOnLostFocus:false\n' > work/game/options.txt
pkill -f "net.minecraft.client.main.Main"; sleep 2
pgrep -f "Xvfb :77" >/dev/null || { (Xvfb :77 -screen 0 1280x720x24 +extension GLX +render -noreset >/dev/null 2>&1 &); sleep 1; }
W=$PWD/work
(cd work/game && nohup setsid $JD/java -Xmx2G -cp "$(cat $W/cp.txt)" net.minecraft.client.main.Main --version 1.21.11 --accessToken 0 --username Tester --gameDir . \
  --assetsDir $HOME/.gradle/caches/fabric-loom/assets --assetIndex 1.21.11-29 --width 1280 --height 720 "$@" > $W/game.log 2>&1 < /dev/null &)
for i in $(seq 1 90); do sleep 2; grep -qE "OpenAL|Sound engine|Exception in thread|logged in with entity" work/game.log && break; done
sleep ${SETTLE:-14}
$JD/java -jar work/AiCoderAssist.jar 2>&1 | head -4
sleep 3; grep -n "AiCoder Assist\]\|VerifyError\|at aicoder" work/game.log | tail -8 | cut -c1-200
