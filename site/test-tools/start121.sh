SP=$SP
cd /home/user/AiCoder-/mc-1.21.11/combat-macros; export JAVA_HOME=/opt/jdk/jdk-25.0.5+8 XDG_RUNTIME_DIR=/tmp/xdg-rt LIBGL_ALWAYS_SOFTWARE=1 DISPLAY=:77 LC_ALL=C.UTF-8
pkill -f "net.fabricmc.devlaunchinjector"; sleep 3
pgrep -f "Xvfb :77" >/dev/null || { (Xvfb :77 -screen 0 1280x720x24 +extension GLX +render -noreset >/dev/null 2>&1 &); sleep 1; }
nohup ./gradlew runClient --console=plain --args="--width 1280 --height 720 --quickPlaySingleplayer \"New World\"" > $SP/run-combat121.log 2>&1 &
for i in $(seq 1 120); do sleep 2; grep -qE "logged in with entity|BUILD FAILED|error:" $SP/run-combat121.log && break; done; sleep 25
grep -E "logged in|BUILD FAILED|error:" $SP/run-combat121.log | tail -3
