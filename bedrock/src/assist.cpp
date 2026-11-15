// AiCoder Assist for Minecraft Bedrock (Windows).
//
// A DLL loaded into the game process. It does not read or patch anything inside the game:
// no signatures, no hooks on game functions, nothing that depends on the game version.
//   - The menu is its own layered window placed over the game window.
//   - Modules only press mouse buttons and number keys through SendInput, the same input a
//     person makes. They cannot see what you hold or what you look at.
// Nothing here hides the DLL, the window or the process from anything.

#define NOMINMAX
#define UNICODE
#define _UNICODE
#include <windows.h>
#include <shlobj.h>
#include <mmsystem.h>
#include <algorithm>
#include <cmath>
#include <cstdio>
#include <functional>
#include <string>
#include <vector>
namespace Gdiplus { using std::max; using std::min; }
#include <gdiplus.h>

using namespace Gdiplus;

// ---------------------------------------------------------------- modules

struct Setting {
    const wchar_t* name; const wchar_t* id;
    double value, min, max, step;
    const wchar_t* unit;
};

struct Module {
    const wchar_t* name; const wchar_t* id; const wchar_t* category; const wchar_t* description;
    std::vector<Setting> settings;
    bool on = false; int bind = 0; bool hold = false;
    bool keyWasDown = false;
    double get(int i) const { return settings[i].value; }
};

static std::vector<Module> g_modules = {
    {L"Left Clicker", L"left_clicker", L"Clicker", L"Hold the left mouse button: it keeps clicking for you.",
        {{L"CPS", L"cps", 12, 1, 100, 1, L""}, {L"Variation", L"variation", 15, 0, 50, 1, L"%"}, {L"Start After", L"start_after", 150, 0, 1000, 10, L"ms"}}},
    {L"Right Clicker", L"right_clicker", L"Clicker", L"Hold the right mouse button: it keeps clicking for you.",
        {{L"CPS", L"cps", 14, 1, 100, 1, L""}, {L"Variation", L"variation", 15, 0, 50, 1, L"%"}, {L"Start After", L"start_after", 150, 0, 1000, 10, L"ms"}}},
    {L"Crystal Clicker", L"crystal_clicker", L"Explosives", L"Hold right click with crystals: place, hit, repeat.",
        {{L"Hit Delay", L"hit_delay", 50, 0, 500, 5, L"ms"}, {L"Place Delay", L"place_delay", 50, 0, 500, 5, L"ms"}}},
    {L"Anchor Macro", L"anchor_macro", L"Explosives", L"Place an anchor by hand: it charges and detonates it.",
        {{L"Glowstone Slot", L"glowstone_slot", 2, 1, 9, 1, L""}, {L"Return Slot", L"return_slot", 1, 1, 9, 1, L""}, {L"Step Delay", L"step_delay", 40, 0, 500, 5, L"ms"}}},
};
enum { LEFT, RIGHT, CRYSTAL, ANCHOR };

// ---------------------------------------------------------------- shared state

static HMODULE g_self;
static HWND g_game, g_overlay;
static volatile bool g_running = true, g_menu = false;
static volatile bool g_physLeft = false, g_physRight = false;
static volatile LONG g_rightPresses = 0;
static volatile ULONGLONG g_leftSince = 0, g_rightSince = 0;
static HHOOK g_mouseHook;
static DWORD g_hookThread;
static CRITICAL_SECTION g_lock;
static wchar_t g_configPath[MAX_PATH];

static ULONGLONG nowMs() {
    static LARGE_INTEGER frequency = [] { LARGE_INTEGER f; QueryPerformanceFrequency(&f); return f; }();
    LARGE_INTEGER c; QueryPerformanceCounter(&c);
    return (ULONGLONG) (c.QuadPart * 1000 / frequency.QuadPart);
}

// ---------------------------------------------------------------- config

static void saveConfig() {
    wchar_t buffer[64];
    for (auto& m : g_modules) {
        WritePrivateProfileStringW(m.id, L"enabled", m.on && !m.hold ? L"1" : L"0", g_configPath);
        swprintf(buffer, 64, L"%d", m.bind); WritePrivateProfileStringW(m.id, L"bind", buffer, g_configPath);
        WritePrivateProfileStringW(m.id, L"bind_mode", m.hold ? L"hold" : L"toggle", g_configPath);
        for (auto& s : m.settings) { swprintf(buffer, 64, L"%g", s.value); WritePrivateProfileStringW(m.id, s.id, buffer, g_configPath); }
    }
}

static int g_layout = 0;      // 0 columns, 1 window
static bool g_motion = true;

static void saveUi() {
    WritePrivateProfileStringW(L"ui", L"layout", g_layout ? L"window" : L"columns", g_configPath);
    WritePrivateProfileStringW(L"ui", L"motion", g_motion ? L"1" : L"0", g_configPath);
}

static void loadConfig() {
    wchar_t dir[MAX_PATH], buffer[64];
    if (FAILED(SHGetFolderPathW(nullptr, CSIDL_APPDATA, nullptr, 0, dir))) GetTempPathW(MAX_PATH, dir);
    wcscat(dir, L"\\AiCoderAssist");
    CreateDirectoryW(dir, nullptr);
    swprintf(g_configPath, MAX_PATH, L"%ls\\bedrock.ini", dir);
    for (auto& m : g_modules) {
        m.hold = GetPrivateProfileStringW(m.id, L"bind_mode", L"toggle", buffer, 64, g_configPath) && !wcscmp(buffer, L"hold");
        m.bind = GetPrivateProfileIntW(m.id, L"bind", 0, g_configPath);
        m.on = !m.hold && GetPrivateProfileIntW(m.id, L"enabled", 0, g_configPath) != 0;
        for (auto& s : m.settings) {
            if (!GetPrivateProfileStringW(m.id, s.id, L"", buffer, 64, g_configPath) || !buffer[0]) continue;
            double v = _wtof(buffer);
            s.value = std::max(s.min, std::min(s.max, v));
        }
    }
    GetPrivateProfileStringW(L"ui", L"layout", L"columns", buffer, 64, g_configPath);
    g_layout = !wcscmp(buffer, L"window");
    g_motion = GetPrivateProfileIntW(L"ui", L"motion", 1, g_configPath) != 0;
}

// ---------------------------------------------------------------- input

static bool cursorHidden() {
    CURSORINFO info{sizeof(CURSORINFO)};
    return GetCursorInfo(&info) && !(info.flags & CURSOR_SHOWING);
}

/** In the world with the game focused: the only time the modules act. A visible cursor means a menu or chat. */
static bool playing() {
    return !g_menu && g_game && GetForegroundWindow() == g_game && cursorHidden();
}

static void mouse(DWORD flags) {
    INPUT in{}; in.type = INPUT_MOUSE; in.mi.dwFlags = flags;
    SendInput(1, &in, sizeof(INPUT));
}

/** One more press of a button. If the player is holding it, release first so the game sees a new press. */
static void click(bool right) {
    DWORD down = right ? MOUSEEVENTF_RIGHTDOWN : MOUSEEVENTF_LEFTDOWN, up = right ? MOUSEEVENTF_RIGHTUP : MOUSEEVENTF_LEFTUP;
    bool held = right ? g_physRight : g_physLeft;
    if (held) { mouse(up); mouse(down); }
    else { mouse(down); mouse(up); }
}

/** Hotbar key 1..9, with both the virtual key and the scan code filled in. */
static void slotKey(int slot) {
    INPUT in[2]{};
    in[0].type = in[1].type = INPUT_KEYBOARD;
    in[0].ki.wVk = in[1].ki.wVk = (WORD) ('0' + slot);
    in[0].ki.wScan = in[1].ki.wScan = (WORD) (0x01 + slot);
    in[1].ki.dwFlags = KEYEVENTF_KEYUP;
    SendInput(2, in, sizeof(INPUT));
}

static LRESULT CALLBACK mouseProc(int code, WPARAM wp, LPARAM lp) {
    if (code == HC_ACTION) {
        auto* info = (MSLLHOOKSTRUCT*) lp;
        if (!(info->flags & LLMHF_INJECTED)) {
            switch (wp) {
                case WM_LBUTTONDOWN: g_physLeft = true; g_leftSince = nowMs(); break;
                case WM_LBUTTONUP: g_physLeft = false; break;
                case WM_RBUTTONDOWN: g_physRight = true; g_rightSince = nowMs(); InterlockedIncrement(&g_rightPresses); break;
                case WM_RBUTTONUP: g_physRight = false; break;
            }
        }
    }
    return CallNextHookEx(g_mouseHook, code, wp, lp);
}

/** The hook gets its own thread that does nothing else, so the mouse never waits on drawing. */
static DWORD WINAPI hookThread(LPVOID) {
    g_mouseHook = SetWindowsHookExW(WH_MOUSE_LL, mouseProc, g_self, 0);
    MSG msg;
    while (GetMessageW(&msg, nullptr, 0, 0) > 0) {}
    if (g_mouseHook) UnhookWindowsHookEx(g_mouseHook);
    return 0;
}

static double jitter(double interval, double percent) {
    double r = rand() / (double) RAND_MAX * 2 - 1;
    return interval * (1 + r * percent / 100);
}

static DWORD WINAPI macroThread(LPVOID) {
    timeBeginPeriod(1);
    ULONGLONG nextLeft = 0, nextRight = 0, crystalAt = 0, anchorAt = 0;
    int crystalStep = 0, anchorStep = -1;
    LONG seenPresses = g_rightPresses;
    while (g_running) {
        Sleep(1);
        ULONGLONG now = nowMs();
        bool active = playing();

        // Binds. A press only counts while playing; a release always counts, so a held module cannot stick on.
        EnterCriticalSection(&g_lock);
        for (auto& m : g_modules) {
            bool down = m.bind && (GetAsyncKeyState(m.bind) & 0x8000);
            if (down && !m.keyWasDown && active) m.on = m.hold ? true : !m.on;
            if (!down && m.keyWasDown && m.hold) m.on = false;
            m.keyWasDown = down;
        }
        Module left = g_modules[LEFT], right = g_modules[RIGHT], crystal = g_modules[CRYSTAL], anchor = g_modules[ANCHOR];
        LeaveCriticalSection(&g_lock);

        LONG presses = g_rightPresses;
        bool rightPressed = presses != seenPresses;
        seenPresses = presses;
        if (!active) { crystalStep = 0; anchorStep = -1; continue; }

        // Anchor: runs once after each right click made by hand.
        if (anchor.on && anchorStep < 0 && rightPressed) { anchorStep = 0; anchorAt = now + (ULONGLONG) anchor.get(2); }
        if (anchorStep >= 0 && now >= anchorAt) {
            switch (anchorStep) {
                case 0: slotKey((int) anchor.get(0)); break;
                case 1: click(true); break;
                case 2: slotKey((int) anchor.get(1)); break;
                case 3: click(true); break;
            }
            anchorAt = now + (ULONGLONG) anchor.get(2);
            if (++anchorStep > 3) anchorStep = -1;
            continue;
        }
        if (anchorStep >= 0) continue;

        // Crystal: while right is held, alternate hit and place.
        if (crystal.on && g_physRight) {
            if (crystalStep == 0) { crystalStep = 1; crystalAt = now + (ULONGLONG) crystal.get(0); }
            else if (now >= crystalAt) {
                if (crystalStep == 1) { click(false); crystalStep = 2; crystalAt = now + (ULONGLONG) crystal.get(1); }
                else { click(true); crystalStep = 1; crystalAt = now + (ULONGLONG) crystal.get(0); }
            }
        } else crystalStep = 0;

        if (left.on && g_physLeft && now - g_leftSince >= (ULONGLONG) left.get(2)) {
            if (now >= nextLeft) { click(false); nextLeft = now + (ULONGLONG) std::max(1.0, jitter(1000.0 / left.get(0), left.get(1))); }
        }
        if (right.on && !crystal.on && g_physRight && now - g_rightSince >= (ULONGLONG) right.get(2)) {
            if (now >= nextRight) { click(true); nextRight = now + (ULONGLONG) std::max(1.0, jitter(1000.0 / right.get(0), right.get(1))); }
        }
    }
    timeEndPeriod(1);
    return 0;
}

// ---------------------------------------------------------------- drawing

struct Hit { RectF rect; std::function<void(int)> click; std::function<void(float)> drag; };

static std::vector<Hit> g_hits;
static std::function<void(float)> g_dragging;
static PointF g_mouse(-1, -1);
static Module* g_selected = nullptr;   // window layout
static Module* g_popup = nullptr;      // columns layout
static Module* g_binding = nullptr;
static const wchar_t* g_category = L"Clicker";
static PrivateFontCollection* g_fonts;
static FontFamily* g_family;
static double g_time;
static ULONGLONG g_toastUntil;

static Color rgba(int r, int g, int b, int a = 255) { return Color((BYTE) a, (BYTE) r, (BYTE) g, (BYTE) b); }
static const Color GREEN = rgba(77, 255, 184), TEAL = rgba(47, 230, 224), VIOLET = rgba(143, 123, 255), PINK = rgba(255, 111, 216);
static const Color TEXT = rgba(238, 250, 255), DIM = rgba(226, 244, 255, 158), FAINT = rgba(226, 244, 255, 92);

static Color withAlpha(Color c, int a) { return Color((BYTE) std::max(0, std::min(255, a)), c.GetR(), c.GetG(), c.GetB()); }
static Color mixColor(Color a, Color b, double f) {
    f = std::max(0.0, std::min(1.0, f));
    return Color((BYTE) (a.GetA() + (b.GetA() - a.GetA()) * f), (BYTE) (a.GetR() + (b.GetR() - a.GetR()) * f),
        (BYTE) (a.GetG() + (b.GetG() - a.GetG()) * f), (BYTE) (a.GetB() + (b.GetB() - a.GetB()) * f));
}

static void rounded(GraphicsPath& path, RectF r, float radius) {
    float d = std::min(radius * 2, std::min(r.Width, r.Height));
    if (d <= 0) { path.AddRectangle(r); return; }
    path.AddArc(r.X, r.Y, d, d, 180, 90);
    path.AddArc(r.X + r.Width - d, r.Y, d, d, 270, 90);
    path.AddArc(r.X + r.Width - d, r.Y + r.Height - d, d, d, 0, 90);
    path.AddArc(r.X, r.Y + r.Height - d, d, d, 90, 90);
    path.CloseFigure();
}

static void fillRound(Graphics& g, RectF r, float radius, Color c) {
    GraphicsPath path; rounded(path, r, radius);
    SolidBrush brush(c); g.FillPath(&brush, &path);
}

static float textWidth(Graphics& g, const std::wstring& s, float size, bool bold = false) {
    Font font(g_family, size, bold ? FontStyleBold : FontStyleRegular, UnitPixel);
    RectF box; g.MeasureString(s.c_str(), -1, &font, PointF(0, 0), StringFormat::GenericTypographic(), &box);
    return box.Width;
}

static void text(Graphics& g, const std::wstring& s, float x, float y, float size, Color c, bool bold = false) {
    Font font(g_family, size, bold ? FontStyleBold : FontStyleRegular, UnitPixel);
    SolidBrush brush(c);
    g.DrawString(s.c_str(), -1, &font, PointF(x, y), StringFormat::GenericTypographic(), &brush);
}

/** Night sky colour at (u, v) in 0..1: dark gradient plus three ribbons, screen blended. */
static void sky(double u, double v, double t, double wide, double& r, double& gr, double& b) {
    r = 6 + (2 - 6) * v; gr = 18 + (4 - 18) * v; b = 38 + (14 - 38) * v;
    const Color ribbon[4] = {GREEN, TEAL, VIOLET, PINK};
    for (int k = 0; k < 3; k++) {
        double c = .26 + .15 * k + .07 * sin(u * (3.2 + k) + t * (.20 + .05 * k) + k * 2.1) + .03 * sin(u * 9 - t * .33 + k);
        double width = (.09 + .03 * sin(u * 5 + t * .15 + k)) * wide;
        double d = (v - c) / width;
        double f = exp(-d * d) * (.55 + .45 * sin(u * 14 + t * .6 + k * 1.3) * sin(u * 3.1 - t * .2));
        if (v > c) f *= exp(-(v - c) * 3 / wide);
        if (f < .01) continue;
        Color col = mixColor(ribbon[k], ribbon[k + 1], .5 + .5 * sin(u * 4 + t * .1 + k));
        r = 255 - (255 - r) * (255 - std::min(255.0, col.GetR() * f)) / 255;
        gr = 255 - (255 - gr) * (255 - std::min(255.0, col.GetG() * f)) / 255;
        b = 255 - (255 - b) * (255 - std::min(255.0, col.GetB() * f)) / 255;
    }
}

/** The sky is computed on a small grid and stretched with bilinear filtering, which also blurs it. */
static void backdrop(Graphics& g, float w, float h) {
    const int W = 96, H = 54;
    static Bitmap* small = new Bitmap(W, H, PixelFormat32bppARGB);
    BitmapData data; Rect all(0, 0, W, H);
    small->LockBits(&all, ImageLockModeWrite, PixelFormat32bppARGB, &data);
    for (int y = 0; y < H; y++) {
        auto* row = (DWORD*) ((BYTE*) data.Scan0 + y * data.Stride);
        for (int x = 0; x < W; x++) {
            double u = (x + .5) / W, v = (y + .5) / H, r, gr, b;
            sky(u, v, g_time, 1, r, gr, b);
            double vignette = 1 - .45 * ((u - .5) * (u - .5) + (v - .5) * (v - .5)) * 2;
            row[x] = 0xE6000000u | (DWORD) (r * vignette) << 16 | (DWORD) (gr * vignette) << 8 | (DWORD) (b * vignette);
        }
    }
    small->UnlockBits(&data);
    g.SetInterpolationMode(InterpolationModeBilinear);
    g.SetPixelOffsetMode(PixelOffsetModeHalf);
    ImageAttributes attributes; attributes.SetWrapMode(WrapModeTileFlipXY);
    g.DrawImage(small, RectF(0, 0, w, h), 0, 0, (REAL) W, (REAL) H, UnitPixel, &attributes);
    g.SetPixelOffsetMode(PixelOffsetModeDefault);
    // Stars in the upper part.
    for (int i = 0; i < 110; i++) {
        unsigned s = i * 2654435761u;
        float x = (s >> 8 & 0xFFFF) / 65535.f * w, y = (s * 40503u >> 8 & 0xFFFF) / 65535.f * h * .62f;
        double tw = .5 + .5 * sin(g_time * (.6 + (s >> 3 & 7) * .3) + i);
        SolidBrush brush(rgba(255, 255, 255, (int) (40 + 150 * tw * tw)));
        float d = (s >> 5 & 3) == 0 ? 2.2f : 1.4f;
        g.FillEllipse(&brush, x, y, d, d);
    }
}

/** Glass panel: aurora-tinted fill, top sheen, and a rim whose gradient slowly turns. */
static void panel(Graphics& g, RectF r, float radius, float screenW, float screenH) {
    GraphicsPath path; rounded(path, r, radius);
    SolidBrush shadow(rgba(0, 0, 0, 60));
    { GraphicsPath sp; rounded(sp, RectF(r.X + 2, r.Y + 8, r.Width, r.Height), radius); g.FillPath(&shadow, &sp); }
    // Fill follows the sky behind it, with wider and more saturated ribbons: reads as blurred glass.
    double cr, cg, cb, cr2, cg2, cb2;
    sky((r.X + r.Width * .3) / screenW, (r.Y + r.Height * .2) / screenH, g_time, 2.4, cr, cg, cb);
    sky((r.X + r.Width * .7) / screenW, (r.Y + r.Height * .8) / screenH, g_time, 2.4, cr2, cg2, cb2);
    LinearGradientBrush fill(PointF(r.X, r.Y), PointF(r.X + r.Width, r.Y + r.Height),
        rgba((int) (cr * .55 + 10), (int) (cg * .55 + 22), (int) (cb * .55 + 44), 214),
        rgba((int) (cr2 * .55 + 8), (int) (cg2 * .55 + 14), (int) (cb2 * .55 + 40), 224));
    g.FillPath(&fill, &path);
    LinearGradientBrush sheen(PointF(0, r.Y - 1), PointF(0, r.Y + std::min(90.f, r.Height * .4f)), rgba(255, 255, 255, 30), rgba(255, 255, 255, 0));
    g.SetClip(&path);
    g.FillRectangle(&sheen, r.X, r.Y, r.Width, std::min(90.f, r.Height * .4f));
    g.ResetClip();
    double a = g_time * 6.2831853 / 12;
    PointF c(r.X + r.Width / 2, r.Y + r.Height / 2);
    float rx = r.Width / 2, ry = r.Height / 2;
    LinearGradientBrush rim(PointF(c.X - (float) cos(a) * rx, c.Y - (float) sin(a) * ry), PointF(c.X + (float) cos(a) * rx, c.Y + (float) sin(a) * ry), rgba(255, 255, 255, 150), rgba(255, 255, 255, 150));
    Color colors[4] = {rgba(255, 255, 255, 150), withAlpha(GREEN, 170), withAlpha(VIOLET, 170), rgba(255, 255, 255, 110)};
    REAL stops[4] = {0, .35f, .7f, 1};
    rim.SetInterpolationColors(colors, stops, 4);
    Pen pen(&rim, 1.2f);
    g.DrawPath(&pen, &path);
}

/** Round bead switch. */
static void bead(Graphics& g, float cx, float cy, bool on) {
    if (on) {
        double breathe = .6 + .4 * sin(g_time * 2.2 + cx * .01);
        for (int i = 3; i >= 1; i--) { SolidBrush glow(withAlpha(GREEN, (int) (22 * breathe * (4 - i)))); g.FillEllipse(&glow, cx - 8 - i * 3, cy - 8 - i * 3, 16 + i * 6.f, 16 + i * 6.f); }
        GraphicsPath p; p.AddEllipse(cx - 8, cy - 8, 16.f, 16.f);
        PathGradientBrush brush(&p);
        brush.SetCenterPoint(PointF(cx - 3, cy - 3));
        brush.SetCenterColor(rgba(214, 255, 240));
        Color edge = rgba(40, 214, 150); int n = 1; brush.SetSurroundColors(&edge, &n);
        g.FillEllipse(&brush, cx - 8, cy - 8, 16.f, 16.f);
    } else {
        SolidBrush fill(rgba(3, 10, 26, 120)); g.FillEllipse(&fill, cx - 8, cy - 8, 16.f, 16.f);
        Pen pen(rgba(226, 244, 255, 90), 1.2f); g.DrawEllipse(&pen, cx - 8, cy - 8, 16.f, 16.f);
    }
}

static bool hovered(RectF r) { return r.Contains(g_mouse); }

static void hit(RectF r, std::function<void(int)> click, std::function<void(float)> drag = nullptr) {
    g_hits.push_back({r, std::move(click), std::move(drag)});
}

static std::wstring keyName(int vk) {
    if (!vk) return L"NONE";
    if ((vk >= 'A' && vk <= 'Z') || (vk >= '0' && vk <= '9')) return std::wstring(1, (wchar_t) vk);
    if (vk >= VK_F1 && vk <= VK_F24) return L"F" + std::to_wstring(vk - VK_F1 + 1);
    switch (vk) {
        case VK_XBUTTON1: return L"MOUSE 4"; case VK_XBUTTON2: return L"MOUSE 5"; case VK_MBUTTON: return L"MOUSE 3";
        case VK_LSHIFT: case VK_SHIFT: return L"SHIFT"; case VK_LCONTROL: case VK_CONTROL: return L"CTRL"; case VK_RCONTROL: return L"R CTRL";
        case VK_LMENU: case VK_MENU: return L"ALT"; case VK_RMENU: return L"R ALT"; case VK_SPACE: return L"SPACE"; case VK_TAB: return L"TAB"; case VK_CAPITAL: return L"CAPS";
    }
    wchar_t name[32]; UINT scan = MapVirtualKeyW(vk, MAPVK_VK_TO_VSC);
    if (GetKeyNameTextW(scan << 16, name, 32) > 0) { std::wstring s(name); for (auto& ch : s) ch = towupper(ch); return s; }
    return L"KEY " + std::to_wstring(vk);
}

static std::wstring number(const Setting& s) {
    wchar_t buffer[32]; swprintf(buffer, 32, L"%g%ls%ls", s.value, s.unit[0] && s.unit[0] != L'%' ? L" " : L"", s.unit);
    return buffer;
}

static void pill(Graphics& g, RectF r, const std::wstring& label, bool active, std::function<void(int)> action) {
    bool hot = hovered(r);
    fillRound(g, r, r.Height / 2, active ? withAlpha(GREEN, 70) : rgba(3, 10, 26, hot ? 150 : 107));
    GraphicsPath path; rounded(path, r, r.Height / 2);
    Pen pen(active ? withAlpha(GREEN, 220) : rgba(226, 244, 255, hot ? 150 : 80), 1.2f); g.DrawPath(&pen, &path);
    text(g, label, r.X + (r.Width - textWidth(g, label, 13, true)) / 2, r.Y + (r.Height - 17) / 2, 13, active || hot ? TEXT : DIM, true);
    hit(r, std::move(action));
}

/** Settings of one module inside the given area. Used by the window layout and by the popup. */
static void settings(Graphics& g, Module& m, RectF area) {
    float x = area.X, y = area.Y, w = area.Width;
    text(g, m.name, x, y, 22, TEXT, true);
    text(g, m.description, x, y + 32, 13, DIM);
    y += 66;
    text(g, L"SETTINGS", x, y, 12, GREEN, true);
    { Pen pen(withAlpha(GREEN, 70), 1); g.DrawLine(&pen, x + 84, y + 8, x + w, y + 8); }
    y += 26;
    for (auto& s : m.settings) {
        Setting* ps = &s;
        text(g, s.name, x, y + 5, 14, TEXT);
        std::wstring value = number(s);
        text(g, value, x + w - textWidth(g, value, 14, true), y + 5, 14, GREEN, true);
        float sx = x + w * .36f, sw = w * .42f, f = (float) ((s.value - s.min) / (s.max - s.min));
        fillRound(g, RectF(sx, y + 12, sw, 4), 2, rgba(226, 244, 255, 46));
        { GraphicsPath p; rounded(p, RectF(sx, y + 12, std::max(4.f, sw * f), 4), 2);
          LinearGradientBrush b(PointF(sx - 1, 0), PointF(sx + sw + 1, 0), TEAL, VIOLET); g.FillPath(&b, &p); }
        { SolidBrush glow(withAlpha(GREEN, 60)); g.FillEllipse(&glow, sx + sw * f - 10, y + 4, 20.f, 20.f);
          SolidBrush knob(rgba(255, 255, 255)); g.FillEllipse(&knob, sx + sw * f - 6, y + 8, 12.f, 12.f); }
        hit(RectF(sx - 8, y, sw + 16, 28), nullptr, [ps, sx, sw](float px) {
            double f = std::max(0.0, std::min(1.0, (double) (px - sx) / sw)), v = ps->min + f * (ps->max - ps->min);
            v = ps->min + floor((v - ps->min) / ps->step + .5) * ps->step;
            EnterCriticalSection(&g_lock); ps->value = std::max(ps->min, std::min(ps->max, v)); LeaveCriticalSection(&g_lock);
        });
        y += 34;
    }
    y += 6;
    text(g, L"BIND", x, y, 12, GREEN, true);
    { Pen pen(withAlpha(GREEN, 70), 1); g.DrawLine(&pen, x + 48, y + 8, x + w, y + 8); }
    y += 26;
    Module* pm = &m;
    text(g, L"Key", x, y + 6, 14, TEXT);
    pill(g, RectF(x + w - 150, y, 150, 28), g_binding == pm ? L"PRESS A KEY" : keyName(m.bind), g_binding == pm, [pm](int button) {
        if (button == 1) { EnterCriticalSection(&g_lock); pm->bind = 0; LeaveCriticalSection(&g_lock); g_binding = nullptr; saveConfig(); }
        else g_binding = pm;
    });
    y += 36;
    text(g, L"Mode", x, y + 6, 14, TEXT);
    pill(g, RectF(x + w - 150, y, 150, 28), m.hold ? L"HOLD" : L"TOGGLE", false, [pm](int) {
        EnterCriticalSection(&g_lock); pm->hold = !pm->hold; if (pm->hold) pm->on = false; LeaveCriticalSection(&g_lock); saveConfig();
    });
}

static void moduleRow(Graphics& g, Module& m, RectF r, bool selected, bool clickSelects) {
    bool hot = hovered(r) && !g_popup;
    fillRound(g, r, 10, m.on ? withAlpha(GREEN, selected ? 60 : 40) : rgba(226, 244, 255, selected ? 44 : hot ? 30 : 14));
    if (selected) { GraphicsPath p; rounded(p, r, 10); Pen pen(withAlpha(GREEN, 150), 1.2f); g.DrawPath(&pen, &p); }
    text(g, m.name, r.X + 14, r.Y + (r.Height - 19) / 2, 15, m.on || hot ? TEXT : DIM, m.on);
    if (m.bind) { std::wstring k = keyName(m.bind); text(g, k, r.X + r.Width - 44 - textWidth(g, k, 11), r.Y + (r.Height - 14) / 2, 11, FAINT); }
    bead(g, r.X + r.Width - 22, r.Y + r.Height / 2, m.on);
    Module* pm = &m;
    auto toggle = [pm] { EnterCriticalSection(&g_lock); pm->on = !pm->on; LeaveCriticalSection(&g_lock); saveConfig(); };
    if (clickSelects) {
        hit(r, [pm, toggle](int button) { if (button == 1) toggle(); else { g_selected = pm; g_binding = nullptr; } });
        hit(RectF(r.X + r.Width - 40, r.Y, 40, r.Height), [toggle](int) { toggle(); });
    } else hit(r, [pm, toggle](int button) { if (button == 1) { g_popup = pm; g_binding = nullptr; } else toggle(); });
}

static std::vector<const wchar_t*> categories() {
    std::vector<const wchar_t*> out;
    for (auto& m : g_modules) if (std::find_if(out.begin(), out.end(), [&](const wchar_t* c) { return !wcscmp(c, m.category); }) == out.end()) out.push_back(m.category);
    return out;
}

static void render(Graphics& g, float w, float h) {
    g_hits.clear();
    backdrop(g, w, h);
    text(g, L"AICODER", 34, 26, 22, TEXT, true);
    text(g, L"ASSIST  /  BEDROCK", 34 + textWidth(g, L"AICODER", 22, true) + 14, 33, 12, withAlpha(GREEN, 230), true);
    float bx = w - 34;
    bx -= 92; pill(g, RectF(bx, 24, 92, 30), L"UNLOAD", false, [](int) { g_running = false; });
    bx -= 102; pill(g, RectF(bx, 24, 92, 30), L"MOTION", g_motion, [](int) { g_motion = !g_motion; saveUi(); });
    bx -= 112; pill(g, RectF(bx, 24, 102, 30), L"COLUMNS", g_layout == 0, [](int) { g_layout = 0; g_popup = nullptr; saveUi(); });
    bx -= 102; pill(g, RectF(bx, 24, 92, 30), L"WINDOW", g_layout == 1, [](int) { g_layout = 1; g_popup = nullptr; saveUi(); });

    auto cats = categories();
    if (g_layout == 0) {
        float cw = 270, gap = 18, x = 34, y = 84;
        const wchar_t* hint = nullptr; RectF hintAt;
        for (size_t i = 0; i < cats.size(); i++) {
            int count = 0; for (auto& m : g_modules) if (!wcscmp(m.category, cats[i])) count++;
            RectF box(x + i * (cw + gap), y, cw, 58 + count * 46.f + 8);
            panel(g, box, 22, w, h);
            wchar_t no[8]; swprintf(no, 8, L"%02d", (int) i + 1);
            text(g, no, box.X + 18, box.Y + 19, 12, withAlpha(GREEN, 230), true);
            std::wstring name(cats[i]); for (auto& ch : name) ch = towupper(ch);
            text(g, name, box.X + 44, box.Y + 16, 15, TEXT, true);
            { Pen pen(rgba(226, 244, 255, 40), 1); g.DrawLine(&pen, box.X + 16, box.Y + 48, box.X + cw - 16, box.Y + 48); }
            float ry = box.Y + 58;
            for (auto& m : g_modules) {
                if (wcscmp(m.category, cats[i])) continue;
                RectF row(box.X + 10, ry, cw - 20, 40);
                moduleRow(g, m, row, false, false);
                if (hovered(row) && !g_popup) { hint = m.description; hintAt = row; }
                ry += 46;
            }
        }
        if (hint) {
            float tw = textWidth(g, hint, 13) + 28;
            RectF card(std::min(hintAt.X + hintAt.Width + 12, w - tw - 12), hintAt.Y + 2, tw, 36);
            fillRound(g, card, 12, rgba(12, 28, 58, 244));
            GraphicsPath p; rounded(p, card, 12); Pen pen(withAlpha(GREEN, 120), 1); g.DrawPath(&pen, &p);
            text(g, hint, card.X + 14, card.Y + 10, 13, TEXT);
        }
        if (g_popup) {
            SolidBrush dim(rgba(2, 6, 16, 150)); g.FillRectangle(&dim, 0.f, 0.f, w, h);
            hit(RectF(0, 0, w, h), [](int) { g_popup = nullptr; g_binding = nullptr; });
            RectF box((w - 520) / 2, (h - 420) / 2, 520, 420);
            panel(g, box, 26, w, h);
            hit(box, [](int) {});
            settings(g, *g_popup, RectF(box.X + 30, box.Y + 28, box.Width - 60, box.Height - 56));
        }
    } else {
        float ww = std::min(1000.f, w - 68), wh = std::min(580.f, h - 150), x = (w - ww) / 2, y = 84 + (h - 84 - 50 - wh) / 2;
        panel(g, RectF(x, y, ww, wh), 28, w, h);
        float cx = x + 22, cw = 200, mx = cx + cw + 22, mw = 320, sx = mx + mw + 30;
        { Pen pen(rgba(226, 244, 255, 36), 1); g.DrawLine(&pen, mx - 11, y + 24, mx - 11, y + wh - 24); g.DrawLine(&pen, sx - 15, y + 24, sx - 15, y + wh - 24); }
        for (size_t i = 0; i < cats.size(); i++) {
            RectF row(cx, y + 24 + i * 46.f, cw, 40);
            bool active = !wcscmp(cats[i], g_category);
            fillRound(g, row, 10, active ? withAlpha(GREEN, 50) : rgba(226, 244, 255, hovered(row) ? 28 : 12));
            wchar_t no[8]; swprintf(no, 8, L"%02d", (int) i + 1);
            text(g, no, row.X + 14, row.Y + 13, 12, active ? GREEN : FAINT, true);
            std::wstring name(cats[i]); for (auto& ch : name) ch = towupper(ch);
            text(g, name, row.X + 42, row.Y + 11, 14, active ? TEXT : DIM, true);
            const wchar_t* c = cats[i];
            hit(row, [c](int) { g_category = c; });
        }
        float ry = y + 24;
        for (auto& m : g_modules) {
            if (wcscmp(m.category, g_category)) continue;
            moduleRow(g, m, RectF(mx, ry, mw, 40), g_selected == &m, true);
            ry += 46;
        }
        if (g_selected) settings(g, *g_selected, RectF(sx, y + 28, x + ww - 30 - sx, wh - 56));
        else { text(g, L"Select a module", sx, y + 30, 16, DIM); text(g, L"Click a row to select, click its switch or right click to toggle.", sx, y + 58, 13, FAINT); }
    }
    const wchar_t* help = g_layout == 0 ? L"Left click: toggle    Right click: settings    Right Shift: close" : L"Click: select    Switch or right click: toggle    Right Shift: close";
    text(g, help, 34, h - 40, 13, DIM);
    const wchar_t* note = L"Modules act only in the world (cursor hidden) with the game focused.";
    text(g, note, w - 34 - textWidth(g, note, 13), h - 40, 13, FAINT);
}

static void renderToast(Graphics& g, float w) {
    const wchar_t* message = L"AiCoder Assist loaded  -  Right Shift opens the menu";
    float tw = textWidth(g, message, 15, true) + 48;
    RectF box((w - tw) / 2, 26, tw, 44);
    fillRound(g, box, 22, rgba(12, 28, 58, 236));
    GraphicsPath p; rounded(p, box, 22); Pen pen(withAlpha(GREEN, 200), 1.4f); g.DrawPath(&pen, &p);
    text(g, message, box.X + 24, box.Y + 12, 15, TEXT, true);
}

// ---------------------------------------------------------------- overlay window

static float g_scale = 1;

static void closeMenu() {
    g_menu = false; g_popup = nullptr; g_binding = nullptr; g_dragging = nullptr;
    saveConfig();
    ShowWindow(g_overlay, SW_HIDE);
    if (g_game) SetForegroundWindow(g_game);
}

static void pressAt(float x, float y, int button) {
    g_binding = nullptr;
    for (int i = (int) g_hits.size() - 1; i >= 0; i--) {
        Hit& h = g_hits[i];
        if (!h.rect.Contains(x, y)) continue;
        if (h.drag && button == 0) { g_dragging = h.drag; g_dragging(x); }
        else if (h.click) { auto action = h.click; action(button); }
        return;
    }
}

static LRESULT CALLBACK overlayProc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp) {
    float x = (short) LOWORD(lp) / g_scale, y = (short) HIWORD(lp) / g_scale;
    switch (msg) {
        case WM_MOUSEMOVE: g_mouse = PointF(x, y); if (g_dragging && (wp & MK_LBUTTON)) g_dragging(x); return 0;
        case WM_LBUTTONDOWN: SetCapture(hwnd); g_mouse = PointF(x, y); pressAt(x, y, 0); return 0;
        case WM_RBUTTONDOWN: g_mouse = PointF(x, y); pressAt(x, y, 1); return 0;
        case WM_LBUTTONUP: ReleaseCapture(); if (g_dragging) { g_dragging = nullptr; saveConfig(); } return 0;
        case WM_XBUTTONDOWN: case WM_MBUTTONDOWN: case WM_KEYDOWN: case WM_SYSKEYDOWN: {
            int vk = msg == WM_XBUTTONDOWN ? (HIWORD(wp) == XBUTTON1 ? VK_XBUTTON1 : VK_XBUTTON2) : msg == WM_MBUTTONDOWN ? VK_MBUTTON : (int) wp;
            if (vk == VK_SHIFT && (HIWORD(lp) & 0xFF) == 0x36) vk = VK_RSHIFT;
            if (vk == VK_SHIFT) vk = VK_LSHIFT;
            if (vk == VK_CONTROL) vk = (HIWORD(lp) & KF_EXTENDED) ? VK_RCONTROL : VK_LCONTROL;
            if (vk == VK_MENU) vk = (HIWORD(lp) & KF_EXTENDED) ? VK_RMENU : VK_LMENU;
            if (g_binding) {
                if (vk != VK_RSHIFT) {
                    EnterCriticalSection(&g_lock);
                    g_binding->bind = vk == VK_ESCAPE || vk == VK_BACK || vk == VK_DELETE ? 0 : vk;
                    g_binding->keyWasDown = true;
                    LeaveCriticalSection(&g_lock);
                    saveConfig();
                }
                g_binding = nullptr;
            } else if (vk == VK_ESCAPE) { if (g_popup) g_popup = nullptr; else closeMenu(); }
            return 0;
        }
        case WM_ACTIVATE:
            // Alt-tabbed away with the menu open: just put it away, do not pull focus back.
            if (LOWORD(wp) == WA_INACTIVE && g_menu) { g_menu = false; g_popup = nullptr; g_binding = nullptr; saveConfig(); ShowWindow(hwnd, SW_HIDE); }
            return 0;
        case WM_MOUSEACTIVATE: return g_menu ? MA_ACTIVATE : MA_NOACTIVATE;
    }
    return DefWindowProcW(hwnd, msg, wp, lp);
}

struct FindData { HWND best; LONG area; };

static BOOL CALLBACK findWindow(HWND hwnd, LPARAM lp) {
    DWORD pid; GetWindowThreadProcessId(hwnd, &pid);
    if (pid != GetCurrentProcessId() || hwnd == g_overlay || !IsWindowVisible(hwnd) || GetWindow(hwnd, GW_OWNER)) return TRUE;
    RECT r; GetClientRect(hwnd, &r);
    auto* data = (FindData*) lp;
    LONG area = r.right * r.bottom;
    if (area > data->area) { data->area = area; data->best = hwnd; }
    return TRUE;
}

static bool gameRect(RECT& out) {
    if (!g_game || !IsWindow(g_game)) { FindData data{nullptr, 0}; EnumWindows(findWindow, (LPARAM) &data); g_game = data.best; }
    if (!g_game || IsIconic(g_game)) return false;
    RECT client; GetClientRect(g_game, &client);
    POINT origin{0, 0}; ClientToScreen(g_game, &origin);
    out = {origin.x, origin.y, origin.x + client.right, origin.y + client.bottom};
    return client.right > 200 && client.bottom > 150;
}

static void present(const RECT& r, bool toastOnly) {
    int w = r.right - r.left, h = r.bottom - r.top;
    BITMAPINFO info{}; info.bmiHeader.biSize = sizeof(BITMAPINFOHEADER); info.bmiHeader.biWidth = w; info.bmiHeader.biHeight = -h;
    info.bmiHeader.biPlanes = 1; info.bmiHeader.biBitCount = 32;
    HDC screen = GetDC(nullptr), memory = CreateCompatibleDC(screen);
    void* bits = nullptr;
    HBITMAP dib = CreateDIBSection(screen, &info, DIB_RGB_COLORS, &bits, nullptr, 0);
    if (dib) {
        HGDIOBJ old = SelectObject(memory, dib);
        {
            Bitmap surface(w, h, w * 4, PixelFormat32bppPARGB, (BYTE*) bits);
            Graphics g(&surface);
            g.Clear(Color(0, 0, 0, 0));
            g.SetSmoothingMode(SmoothingModeAntiAlias);
            g.SetTextRenderingHint(TextRenderingHintAntiAlias);
            g_scale = std::max(.55f, std::min(1.6f, std::min(w / 1280.f, h / 720.f)));
            g.ScaleTransform(g_scale, g_scale);
            if (toastOnly) renderToast(g, w / g_scale); else render(g, w / g_scale, h / g_scale);
        }
        POINT position{r.left, r.top}, zero{0, 0}; SIZE size{w, h};
        BLENDFUNCTION blend{AC_SRC_OVER, 0, 255, AC_SRC_ALPHA};
        UpdateLayeredWindow(g_overlay, screen, &position, &size, memory, &zero, 0, &blend, ULW_ALPHA);
        SelectObject(memory, old);
        DeleteObject(dib);
    }
    DeleteDC(memory);
    ReleaseDC(nullptr, screen);
}

static void loadFont() {
    g_fonts = new PrivateFontCollection();
    HRSRC resource = FindResourceW(g_self, MAKEINTRESOURCEW(101), MAKEINTRESOURCEW(10));
    if (resource) {
        HGLOBAL handle = LoadResource(g_self, resource);
        g_fonts->AddMemoryFont(LockResource(handle), (INT) SizeofResource(g_self, resource));
    }
    int found = 0;
    static FontFamily families[2];
    if (g_fonts->GetFamilyCount() > 0) g_fonts->GetFamilies(1, families, &found);
    g_family = found > 0 ? &families[0] : new FontFamily(L"Segoe UI");
    if (!g_family->IsAvailable()) g_family = const_cast<FontFamily*>(FontFamily::GenericSansSerif());
}

static DWORD WINAPI uiThread(LPVOID) {
    InitializeCriticalSection(&g_lock);
    loadConfig();
    GdiplusStartupInput startup; ULONG_PTR token;
    GdiplusStartup(&token, &startup, nullptr);
    loadFont();

    WNDCLASSW wc{}; wc.lpfnWndProc = overlayProc; wc.hInstance = g_self; wc.lpszClassName = L"AiCoderAssistOverlay"; wc.hCursor = LoadCursorW(nullptr, IDC_ARROW);
    RegisterClassW(&wc);
    g_overlay = CreateWindowExW(WS_EX_LAYERED | WS_EX_TOPMOST | WS_EX_TOOLWINDOW, wc.lpszClassName, L"AiCoder Assist", WS_POPUP, 0, 0, 100, 100, nullptr, nullptr, g_self, nullptr);

    HANDLE hook = CreateThread(nullptr, 0, hookThread, nullptr, 0, &g_hookThread);
    HANDLE macro = CreateThread(nullptr, 0, macroThread, nullptr, 0, nullptr);

    g_toastUntil = nowMs() + 6000;
    bool shiftWasDown = false, toastShown = false;
    ULONGLONG start = nowMs();
    while (g_running) {
        MSG msg;
        while (PeekMessageW(&msg, nullptr, 0, 0, PM_REMOVE)) { TranslateMessage(&msg); DispatchMessageW(&msg); }
        RECT r;
        bool have = gameRect(r);
        HWND foreground = GetForegroundWindow();
        SHORT state = GetAsyncKeyState(VK_RSHIFT);
        bool shift = (state & 0x8000) != 0, tapped = (state & 1) && !shift && !shiftWasDown;   // a tap shorter than one frame
        if (((shift && !shiftWasDown) || tapped) && have && (foreground == g_game || foreground == g_overlay)) {
            if (g_menu) closeMenu();
            else {
                g_menu = true; toastShown = false; g_toastUntil = 0;
                SetWindowLongW(g_overlay, GWL_EXSTYLE, WS_EX_LAYERED | WS_EX_TOPMOST | WS_EX_TOOLWINDOW);
                present(r, false);
                ShowWindow(g_overlay, SW_SHOW);
                SetForegroundWindow(g_overlay);
            }
        }
        shiftWasDown = shift;
        g_time = g_motion ? (nowMs() - start) / 1000.0 : 0;
        if (g_menu && have) present(r, false);
        else if (!g_menu && have && nowMs() < g_toastUntil) {
            if (!toastShown) {
                SetWindowLongW(g_overlay, GWL_EXSTYLE, WS_EX_LAYERED | WS_EX_TOPMOST | WS_EX_TOOLWINDOW | WS_EX_TRANSPARENT | WS_EX_NOACTIVATE);
                RECT strip{r.left, r.top, r.right, r.top + 120};
                present(strip, true);
                ShowWindow(g_overlay, SW_SHOWNOACTIVATE);
                toastShown = true;
            }
        } else if (toastShown) { ShowWindow(g_overlay, SW_HIDE); toastShown = false; }
        Sleep(g_menu ? 16 : 30);
    }
    saveConfig();
    PostThreadMessageW(g_hookThread, WM_QUIT, 0, 0);
    WaitForSingleObject(hook, 2000); WaitForSingleObject(macro, 2000);
    CloseHandle(hook); CloseHandle(macro);
    DestroyWindow(g_overlay);
    UnregisterClassW(wc.lpszClassName, g_self);
    if (g_game) SetForegroundWindow(g_game);
    FreeLibraryAndExitThread(g_self, 0);
    return 0;
}

BOOL WINAPI DllMain(HINSTANCE instance, DWORD reason, LPVOID) {
    if (reason == DLL_PROCESS_ATTACH) {
        g_self = instance;
        DisableThreadLibraryCalls(instance);
        HANDLE thread = CreateThread(nullptr, 0, uiThread, nullptr, 0, nullptr);
        if (thread) CloseHandle(thread);
    }
    return TRUE;
}
