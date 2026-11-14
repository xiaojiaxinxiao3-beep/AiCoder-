#!/bin/bash
# Restarts the unmodified game, waits for the title screen, injects, and takes a screenshot named $1.
SP=/tmp/claude-0/-home-user-AiCoder-/a88f1349-443d-5f53-938d-da47d5d5d1fb/scratchpad
cd $SP/inject; unset JAVA_TOOL_OPTIONS
pkill -f "net.minecraft.client.main.Main"; sleep 2; ./vanilla.sh "${@:2}"
for i in $(seq 1 60); do sleep 2; grep -qE "OpenAL|Sound engine|Exception in thread" vanilla.log && break; done; sleep ${SETTLE:-14}
/opt/jdk/jdk-25.0.5+8/bin/java -cp build/aicoder-payload.jar aicoder.inject.Injector build/aicoder-payload.jar 2>&1 | head -5
sleep 4; grep -n "AiCoder\|VerifyError\|at aicoder" vanilla.log | tail -8 | cut -c1-220
DISPLAY=:77 import -window root $SP/shots/${1:-inj}.png
