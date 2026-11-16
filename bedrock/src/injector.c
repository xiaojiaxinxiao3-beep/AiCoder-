// AiCoderAssist-Bedrock.exe: loads AiCoderAssist-Bedrock.dll (next to this file) into the running game.
// Plain LoadLibrary through a remote thread. Nothing is hidden: the DLL shows up in the game's module list
// under its own name.
#define UNICODE
#define _UNICODE
#include <windows.h>
#include <tlhelp32.h>
#include <stdio.h>
#include <wchar.h>

static DWORD findProcess(const wchar_t* name) {
    HANDLE snap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    PROCESSENTRY32W entry; entry.dwSize = sizeof(entry);
    DWORD pid = 0;
    if (Process32FirstW(snap, &entry)) do {
        if (!_wcsicmp(entry.szExeFile, name)) { pid = entry.th32ProcessID; break; }
    } while (Process32NextW(snap, &entry));
    CloseHandle(snap);
    return pid;
}

static int alreadyLoaded(DWORD pid, const wchar_t* dllName) {
    HANDLE snap = CreateToolhelp32Snapshot(TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, pid);
    if (snap == INVALID_HANDLE_VALUE) return 0;
    MODULEENTRY32W entry; entry.dwSize = sizeof(entry);
    int found = 0;
    if (Module32FirstW(snap, &entry)) do { if (!_wcsicmp(entry.szModule, dllName)) { found = 1; break; } } while (Module32NextW(snap, &entry));
    CloseHandle(snap);
    return found;
}

static int finish(int code) {
    wprintf(L"\nPress Enter to close.");
    getwchar();
    return code;
}

int wmain(int argc, wchar_t** argv) {
    const wchar_t* dllName = L"AiCoderAssist-Bedrock.dll";
    const wchar_t* target = argc > 1 ? argv[1] : L"Minecraft.Windows.exe";
    wchar_t dll[MAX_PATH];
    GetModuleFileNameW(NULL, dll, MAX_PATH);
    wchar_t* slash = wcsrchr(dll, L'\\');
    if (slash) slash[1] = 0;
    wcscat(dll, dllName);
    wprintf(L"AiCoder Assist for Bedrock\n\n");
    if (GetFileAttributesW(dll) == INVALID_FILE_ATTRIBUTES) { wprintf(L"Cannot find %ls\nKeep the .dll in the same folder as this .exe.\n", dll); return finish(1); }

    DWORD pid = findProcess(target);
    if (!pid) { wprintf(L"%ls is not running.\nStart Minecraft (Bedrock) first, enter a world, then run this again.\n", target); return finish(1); }
    if (alreadyLoaded(pid, dllName)) { wprintf(L"Already loaded. Press Right Shift (or the menu key you chose) in the game.\n"); return finish(0); }

    // Store builds of the game run in an app container, which may only read files that grant
    // "ALL APPLICATION PACKAGES" access. Harmless on the newer desktop builds.
    wchar_t command[MAX_PATH + 96];
    swprintf(command, MAX_PATH + 96, L"icacls \"%ls\" /grant *S-1-15-2-1:(RX) >nul 2>nul", dll);
    _wsystem(command);

    HANDLE process = OpenProcess(PROCESS_CREATE_THREAD | PROCESS_QUERY_INFORMATION | PROCESS_VM_OPERATION | PROCESS_VM_WRITE | PROCESS_VM_READ, FALSE, pid);
    if (!process) { wprintf(L"Cannot open the game process (error %lu).\nTry: right click this file, Run as administrator.\n", GetLastError()); return finish(1); }
    SIZE_T bytes = (wcslen(dll) + 1) * sizeof(wchar_t);
    void* remote = VirtualAllocEx(process, NULL, bytes, MEM_COMMIT | MEM_RESERVE, PAGE_READWRITE);
    if (!remote || !WriteProcessMemory(process, remote, dll, bytes, NULL)) { wprintf(L"Cannot write to the game process (error %lu).\n", GetLastError()); return finish(1); }
    HANDLE thread = CreateRemoteThread(process, NULL, 0, (LPTHREAD_START_ROUTINE) GetProcAddress(GetModuleHandleW(L"kernel32.dll"), "LoadLibraryW"), remote, 0, NULL);
    if (!thread) { wprintf(L"Cannot start a thread in the game (error %lu).\n", GetLastError()); return finish(1); }
    WaitForSingleObject(thread, 10000);
    DWORD result = 0; GetExitCodeThread(thread, &result);
    VirtualFreeEx(process, remote, 0, MEM_RELEASE);
    CloseHandle(thread); CloseHandle(process);
    if (!result && !alreadyLoaded(pid, dllName)) { wprintf(L"The game refused to load the DLL.\n"); return finish(1); }
    wprintf(L"Loaded into %ls (pid %lu).\nGo back to the game and press Right Shift (or the menu key you chose).\n", target, pid);
    return finish(0);
}
