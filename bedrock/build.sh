#!/bin/sh
# Cross-compiles with mingw-w64. Output: work/AiCoderAssist-Bedrock.dll and .exe (work/ is not committed).
set -e
cd "$(dirname "$0")"
mkdir -p work
x86_64-w64-mingw32-windres src/assist.rc -O coff -o work/assist.res
x86_64-w64-mingw32-g++ -std=c++17 -O2 -Wall -shared -static -static-libgcc -static-libstdc++ -o work/AiCoderAssist-Bedrock.dll src/assist.cpp work/assist.res -lgdiplus -lgdi32 -luser32 -lshell32 -lwinmm -lole32
x86_64-w64-mingw32-gcc -O2 -Wall -municode -o work/AiCoderAssist-Bedrock.exe src/injector.c
x86_64-w64-mingw32-gcc -O2 -municode -mwindows -o work/Minecraft.Windows.exe test/fakegame.c
x86_64-w64-mingw32-strip work/*.dll work/*.exe
ls -la work
