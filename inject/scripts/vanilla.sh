#!/bin/bash
# Starts an unmodified Minecraft 1.21.11 client (official obfuscated jar, no mod loader) on the virtual display.
SP=/tmp/claude-0/-home-user-AiCoder-/a88f1349-443d-5f53-938d-da47d5d5d1fb/scratchpad
cd $SP/inject/game; unset JAVA_TOOL_OPTIONS
export XDG_RUNTIME_DIR=/tmp/xdg-rt LIBGL_ALWAYS_SOFTWARE=1 DISPLAY=:77
pgrep -f "Xvfb :77" >/dev/null || { (Xvfb :77 -screen 0 1280x720x24 +extension GLX +render -noreset >/dev/null 2>&1 &); sleep 1; }
nohup /opt/jdk/jdk-25.0.5+8/bin/java -Xmx2G -cp "$(cat ../cp.txt)" net.minecraft.client.main.Main --version 1.21.11 --accessToken 0 --username Tester --gameDir . --assetsDir /root/.gradle/caches/fabric-loom/assets --assetIndex 1.21.11-29 --width 1280 --height 720 "$@" > ../vanilla.log 2>&1 &
echo $! > ../vanilla.pid
