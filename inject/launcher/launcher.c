/*
 * AiCoderAssist.exe: finds the running Minecraft and starts the injector with the game's own Java.
 *
 * The file is this small program with AiCoderAssist.jar appended to it. Java reads a jar from the end of
 * a file, so the same .exe can be handed to "java -jar" as it is: no second file and nothing to unpack.
 * Minecraft's launcher ships a full Java runtime, including what is needed to attach to a running JVM, so
 * no Java has to be installed separately.
 */
#ifndef UNICODE
#define UNICODE
#endif
#ifndef _UNICODE
#define _UNICODE
#endif
#include <windows.h>
#include <tlhelp32.h>
#include <wchar.h>
#include <stdio.h>

#define MAX_DIRS 16

static void wait_for_key(void) {
    wprintf(L"\nPress Enter to close this window.");
    fflush(stdout);
    getwchar();
}

int wmain(void) {
    wchar_t self[MAX_PATH * 4];
    if (!GetModuleFileNameW(NULL, self, MAX_PATH * 4)) {
        wprintf(L"Could not find this program's own path.\n");
        wait_for_key();
        return 1;
    }

    wprintf(L"AiCoder Assist for Minecraft 1.21.11\n\n");

    /* Folders of every running java.exe / javaw.exe: one of them is the game's */
    wchar_t dirs[MAX_DIRS][MAX_PATH * 2];
    int count = 0;
    /* Their process ids, handed to the injector so it does not have to find the game by itself */
    wchar_t ids[1024] = L"";

    HANDLE snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (snapshot != INVALID_HANDLE_VALUE) {
        PROCESSENTRY32W entry;
        entry.dwSize = sizeof(entry);
        if (Process32FirstW(snapshot, &entry)) {
            do {
                if (_wcsicmp(entry.szExeFile, L"javaw.exe") != 0 && _wcsicmp(entry.szExeFile, L"java.exe") != 0) continue;

                wchar_t id[32];
                _snwprintf(id, 32, L" %lu", (unsigned long) entry.th32ProcessID);
                if (wcslen(ids) + wcslen(id) < 1000) wcscat(ids, id);

                HANDLE process = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, FALSE, entry.th32ProcessID);
                if (!process) continue;

                wchar_t path[MAX_PATH * 2];
                DWORD size = MAX_PATH * 2;
                if (QueryFullProcessImageNameW(process, 0, path, &size)) {
                    wchar_t *slash = wcsrchr(path, L'\\');
                    if (slash) {
                        *slash = 0;
                        int known = 0;
                        for (int i = 0; i < count; i++) known |= _wcsicmp(dirs[i], path) == 0;
                        if (!known && count < MAX_DIRS) wcscpy(dirs[count++], path);
                    }
                }
                CloseHandle(process);
            } while (Process32NextW(snapshot, &entry));
        }
        CloseHandle(snapshot);
    }

    if (count == 0) {
        wprintf(L"Minecraft is not running.\nStart Minecraft 1.21.11 first, wait for the title screen, then run this again.\n");
        wait_for_key();
        return 2;
    }

    /* 3 = no Java could be started at all; the injector itself answers 0 (in), 1 (refused) or 2 (no game) */
    DWORD result = 3;
    /* Several Java installations may be running (a launcher often is one itself); try each until one gets in */
    for (int i = 0; i < count && result != 0; i++) {
        wchar_t java[MAX_PATH * 2];
        _snwprintf(java, MAX_PATH * 2, L"%ls\\java.exe", dirs[i]);
        if (GetFileAttributesW(java) == INVALID_FILE_ATTRIBUTES) continue;

        wchar_t command[MAX_PATH * 8];
        _snwprintf(command, MAX_PATH * 8, L"\"%ls\" -jar \"%ls\"%ls", java, self, ids);

        STARTUPINFOW startup;
        PROCESS_INFORMATION info;
        ZeroMemory(&startup, sizeof(startup));
        startup.cb = sizeof(startup);
        if (!CreateProcessW(NULL, command, NULL, NULL, TRUE, 0, NULL, NULL, &startup, &info)) continue;

        WaitForSingleObject(info.hProcess, INFINITE);
        GetExitCodeProcess(info.hProcess, &result);
        CloseHandle(info.hProcess);
        CloseHandle(info.hThread);
    }

    if (result == 3) wprintf(L"Found a running Java program, but could not start its java.exe.\n");
    wait_for_key();
    return (int) result;
}
