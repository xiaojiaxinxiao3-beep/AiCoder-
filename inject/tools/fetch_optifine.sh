#!/bin/bash
# For testing the 1.8.9 build under OptiFine: fetches OptiFine 1.8.9 HD U M5 from optifine.net into
# work-1.8.9/ and writes optifine-cp.txt, the class path its launcher profile starts the game with.
# Then:  OPTIFINE=1 ./test-1.8.9.sh
set -e
cd "$(dirname "$0")/../work-1.8.9"
F=OptiFine_1.8.9_HD_U_M5.jar
if [ ! -f $F ]; then
  link=$(curl -sfL "https://optifine.net/adloadx?f=$F" | grep -o "downloadx?f=[^'\"]*" | head -1)
  curl -sfL -o $F "https://optifine.net/$link"
fi
unzip -qo $F launchwrapper-of-2.2.jar
echo "$PWD/launchwrapper-of-2.2.jar:$PWD/$F:$(cat cp.txt)" > optifine-cp.txt
