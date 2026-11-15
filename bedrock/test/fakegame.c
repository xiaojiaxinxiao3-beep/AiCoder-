// Stand-in for the game, for testing under Wine: a window that hides the cursor and logs the input it receives.
#define UNICODE
#include <windows.h>
#include <stdio.h>
static int L, R, K[10]; static int hide = 1;
static void dump(void) { FILE* f = fopen("fakegame.log", "w"); if (!f) return; fprintf(f, "left=%d right=%d", L, R); for (int i = 1; i < 10; i++) fprintf(f, " k%d=%d", i, K[i]); fprintf(f, " hidden=%d\n", hide); fclose(f); }
static LRESULT CALLBACK proc(HWND h, UINT m, WPARAM w, LPARAM l) {
    switch (m) {
        case WM_LBUTTONDOWN: L++; dump(); return 0;
        case WM_RBUTTONDOWN: R++; dump(); return 0;
        case WM_KEYDOWN: if (w >= '1' && w <= '9') K[w - '0']++; if (w == 'C') { hide = !hide; ShowCursor(!hide); } if (w == 'Z') { L = R = 0; for (int i = 0; i < 10; i++) K[i] = 0; } dump(); return 0;
        case WM_PAINT: { PAINTSTRUCT ps; HDC dc = BeginPaint(h, &ps); RECT r; GetClientRect(h, &r); HBRUSH b = CreateSolidBrush(RGB(90, 140, 90)); FillRect(dc, &r, b); DeleteObject(b); EndPaint(h, &ps); return 0; }
        case WM_DESTROY: PostQuitMessage(0); return 0;
    }
    return DefWindowProcW(h, m, w, l);
}
int WINAPI wWinMain(HINSTANCE i, HINSTANCE p, LPWSTR c, int s) {
    WNDCLASSW wc = {0}; wc.lpfnWndProc = proc; wc.hInstance = i; wc.lpszClassName = L"FakeGame"; wc.hCursor = LoadCursorW(NULL, IDC_ARROW);
    RegisterClassW(&wc);
    HWND h = CreateWindowW(L"FakeGame", L"Minecraft", WS_POPUP | WS_VISIBLE, 0, 0, 1280, 720, NULL, NULL, i, NULL);
    ShowCursor(FALSE); dump();
    MSG m; while (GetMessageW(&m, NULL, 0, 0) > 0) { TranslateMessage(&m); DispatchMessageW(&m); }
    return 0;
}
