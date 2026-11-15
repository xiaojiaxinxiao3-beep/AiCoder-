// The GAMES pane: six small games drawn with the menu's own shapes and palette.
// Included by assist.cpp after its drawing helpers. Same games as the Java injected client.

namespace games {

static Graphics* G;
static float ax, ay, aw, ah;         // play area, in the menu's design units
static bool held[256];
static bool leftDown;

static const Color MINT = rgba(160, 255, 225);
static const Color WHITE = rgba(255, 255, 255);

static Color pal(int i) {
    const Color colors[7] = {GREEN, TEAL, VIOLET, PINK, MINT, mixColor(GREEN, VIOLET, .5), mixColor(PINK, TEAL, .45)};
    return colors[((i % 7) + 7) % 7];
}

static void quad(float x, float y, float w, float h, Color c) {
    SolidBrush brush(c);
    G->FillRectangle(&brush, x, y, w, h);
}

static void circle(float cx, float cy, float r, Color c) {
    SolidBrush brush(c);
    G->FillEllipse(&brush, cx - r, cy - r, r * 2, r * 2);
}

static void glow(float cx, float cy, float r, Color c) {
    for (int i = 3; i >= 1; i--) circle(cx, cy, r * (1 + i * .35f), withAlpha(c, 26));
}

/** One rounded cell with a lighter top, used by every grid game. */
static void cell(float x, float y, float size, Color c, double alpha) {
    float gap = std::max(1.f, size * .06f), r = size * .22f;
    RectF box(x + gap, y + gap, size - gap * 2, size - gap * 2);
    GraphicsPath path; rounded(path, box, r);
    LinearGradientBrush brush(PointF(0, box.Y - 1), PointF(0, box.Y + box.Height + 1),
        withAlpha(mixColor(c, WHITE, .3), (int) (255 * alpha)), withAlpha(c, (int) (255 * alpha)));
    G->FillPath(&brush, &path);
}

static void well(float x, float y, float w, float h) {
    fillRound(*G, RectF(x, y, w, h), 10, rgba(3, 8, 22, 140));
    GraphicsPath path; rounded(path, RectF(x, y, w, h), 10);
    Pen pen(rgba(255, 255, 255, 40), 1); G->DrawPath(&pen, &path);
}

static void label(const std::wstring& s, float cx, float cy, float size, Color c) {
    text(*G, s, cx - textWidth(*G, s, size, true) / 2, cy - size * .62f, size, c, true);
}

static bool inArea() {
    return g_mouse.X >= ax && g_mouse.X < ax + aw && g_mouse.Y >= ay && g_mouse.Y < ay + ah;
}

struct Game {
    const wchar_t* id; const wchar_t* title; const wchar_t* help;
    int score = 0; bool over = false, paused = false;
    Game(const wchar_t* i, const wchar_t* t, const wchar_t* h) : id(i), title(t), help(h) {}
    virtual ~Game() {}
    virtual void reset() = 0;
    virtual void update(double dt) = 0;
    virtual void draw() = 0;
    virtual void key(int) {}
    virtual void click(int) {}
    virtual void scroll(int) {}
    virtual void leave() {}
    virtual bool ranked() { return true; }
    virtual std::wstring status() { return over ? L"GAME OVER  -  R TO RESTART" : paused ? L"PAUSED  -  P TO CONTINUE" : L""; }
};

// ---------------------------------------------------------------- Drop

struct Blocks : Game {
    static const int W = 10, H = 20;
    int board[H][W]; std::vector<int> bag;
    int piece = 0, next = 0, hold = -1, px = 4, py = 1, lines = 0; int cells[4][2]; bool heldOnce = false; double fall = 0;
    Blocks() : Game(L"blocks", L"Drop", L"Arrows move and turn    Down soft drop    Space drop    C hold    P pause    R restart") { reset(); }

    const int (*shape(int k))[2] {
        static const int S[7][4][2] = {{{-1, 0}, {0, 0}, {1, 0}, {2, 0}}, {{0, 0}, {1, 0}, {0, 1}, {1, 1}}, {{-1, 0}, {0, 0}, {1, 0}, {0, -1}},
            {{-1, 0}, {0, 0}, {0, -1}, {1, -1}}, {{-1, -1}, {0, -1}, {0, 0}, {1, 0}}, {{-1, -1}, {-1, 0}, {0, 0}, {1, 0}}, {{-1, 0}, {0, 0}, {1, 0}, {1, -1}}};
        return S[k];
    }
    int deal() {
        if (bag.empty()) { for (int i = 0; i < 7; i++) bag.push_back(i); for (int i = 6; i > 0; i--) std::swap(bag[i], bag[rand() % (i + 1)]); }
        int k = bag.back(); bag.pop_back(); return k;
    }
    bool fits(int c[4][2], int x, int y) {
        for (int i = 0; i < 4; i++) {
            int cx = x + c[i][0], cy = y + c[i][1];
            if (cx < 0 || cx >= W || cy >= H) return false;
            if (cy >= 0 && board[cy][cx]) return false;
        }
        return true;
    }
    void spawn(int kind) {
        piece = kind; px = 4; py = 1; fall = 0;
        for (int i = 0; i < 4; i++) { cells[i][0] = shape(kind)[i][0]; cells[i][1] = shape(kind)[i][1]; }
        if (!fits(cells, px, py)) over = true;
    }
    void reset() override {
        memset(board, 0, sizeof(board)); bag.clear(); lines = 0; hold = -1; heldOnce = false;
        next = deal(); spawn(deal());
    }
    int level() { return lines / 10 + 1; }
    void step() {
        if (fits(cells, px, py + 1)) { py++; return; }
        for (int i = 0; i < 4; i++) if (py + cells[i][1] >= 0) board[py + cells[i][1]][px + cells[i][0]] = piece + 1;
        int cleared = 0;
        for (int y = H - 1; y >= 0; y--) {
            bool full = true;
            for (int x = 0; x < W; x++) full = full && board[y][x];
            if (!full) continue;
            for (int k = y; k > 0; k--) memcpy(board[k], board[k - 1], sizeof(board[k]));
            memset(board[0], 0, sizeof(board[0]));
            cleared++; y++;
        }
        static const int points[5] = {0, 100, 300, 500, 800};
        lines += cleared; score += points[cleared] * level(); heldOnce = false;
        int n = next; next = deal(); spawn(n);
    }
    void update(double dt) override {
        fall += dt;
        double interval = std::max(.06, .8 - (level() - 1) * .07);
        while (fall >= interval && !over) { fall -= interval; step(); }
    }
    void key(int k) override {
        if (k == VK_LEFT && fits(cells, px - 1, py)) px--;
        else if (k == VK_RIGHT && fits(cells, px + 1, py)) px++;
        else if (k == VK_DOWN && fits(cells, px, py + 1)) { py++; score++; fall = 0; }
        else if (k == VK_UP || k == 'X') {
            if (piece == 1) return;
            int t[4][2];
            for (int i = 0; i < 4; i++) { t[i][0] = -cells[i][1]; t[i][1] = cells[i][0]; }
            for (int kick : {0, -1, 1, -2, 2}) if (fits(t, px + kick, py)) { memcpy(cells, t, sizeof(t)); px += kick; return; }
        } else if (k == VK_SPACE) { while (fits(cells, px, py + 1)) { py++; score += 2; } step(); }
        else if (k == 'C' && !heldOnce) {
            int old = hold; hold = piece; heldOnce = true;
            if (old < 0) { int n = next; next = deal(); spawn(n); } else spawn(old);
        }
    }
    void side(const wchar_t* name, int kind, float x, float y, float size) {
        text(*G, name, x, y, 12, GREEN, true);
        well(x, y + 22, size * 4.4f, size * 3.4f);
        if (kind < 0) return;
        float s = size * .8f;
        for (int i = 0; i < 4; i++) cell(x + size * 1.5f + shape(kind)[i][0] * s, y + 22 + size * 1.6f + shape(kind)[i][1] * s, s, pal(kind), 1);
    }
    void draw() override {
        float size = std::floor(std::min(ah / H, aw / 22)), bx = ax + (aw - size * W) / 2, by = ay + (ah - size * H) / 2;
        well(bx - 6, by - 6, size * W + 12, size * H + 12);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) if (board[y][x]) cell(bx + x * size, by + y * size, size, pal(board[y][x] - 1), 1);
        if (!over) {
            int gy = py;
            while (fits(cells, px, gy + 1)) gy++;
            for (int i = 0; i < 4; i++) if (gy + cells[i][1] >= 0) cell(bx + (px + cells[i][0]) * size, by + (gy + cells[i][1]) * size, size, pal(piece), .22);
            for (int i = 0; i < 4; i++) if (py + cells[i][1] >= 0) cell(bx + (px + cells[i][0]) * size, by + (py + cells[i][1]) * size, size, pal(piece), 1);
        }
        side(L"NEXT", next, bx + size * W + 30, by, size);
        side(L"HOLD", hold, bx - 30 - size * 4.4f, by, size);
        text(*G, L"LEVEL " + std::to_wstring(level()), bx + size * W + 30, by + size * 6.2f, 13, DIM);
        text(*G, L"LINES " + std::to_wstring(lines), bx + size * W + 30, by + size * 6.2f + 22, 13, DIM);
    }
};

// ---------------------------------------------------------------- Trail

struct Snake : Game {
    static const int W = 28, H = 17;
    std::vector<POINT> body; int dx = 1, dy = 0, ndx = 1, ndy = 0, fx = 0, fy = 0; double timer = 0;
    Snake() : Game(L"snake", L"Trail", L"Arrows or WASD steer    P pause    R restart") { reset(); }
    bool on(int x, int y) { for (auto& p : body) if (p.x == x && p.y == y) return true; return false; }
    void food() { do { fx = rand() % W; fy = rand() % H; } while (on(fx, fy)); }
    void reset() override {
        body.clear();
        for (int i = 0; i < 4; i++) body.push_back({W / 2 - i, H / 2});
        dx = ndx = 1; dy = ndy = 0; timer = 0; food();
    }
    void key(int k) override {
        int x = k == VK_LEFT || k == 'A' ? -1 : k == VK_RIGHT || k == 'D' ? 1 : 0, y = k == VK_UP || k == 'W' ? -1 : k == VK_DOWN || k == 'S' ? 1 : 0;
        if ((x || y) && (x != -dx || y != -dy)) { ndx = x; ndy = y; }
    }
    void update(double dt) override {
        timer += dt;
        double interval = std::max(.055, .13 - body.size() * .0012);
        while (timer >= interval && !over) {
            timer -= interval; dx = ndx; dy = ndy;
            int x = body.front().x + dx, y = body.front().y + dy;
            bool eat = x == fx && y == fy;
            if (!eat) body.pop_back();
            if (x < 0 || x >= W || y < 0 || y >= H || on(x, y)) { over = true; return; }
            body.insert(body.begin(), {x, y});
            if (eat) { score += 10; food(); }
        }
    }
    void draw() override {
        float size = std::floor(std::min(aw / W, ah / H)), bx = ax + (aw - size * W) / 2, by = ay + (ah - size * H) / 2;
        well(bx - 6, by - 6, size * W + 12, size * H + 12);
        glow(bx + (fx + .5f) * size, by + (fy + .5f) * size, size * .34f, PINK);
        circle(bx + (fx + .5f) * size, by + (fy + .5f) * size, size * .32f, PINK);
        for (size_t i = 0; i < body.size(); i++)
            cell(bx + body[i].x * size, by + body[i].y * size, size, mixColor(GREEN, VIOLET, (double) i / std::max<size_t>(1, body.size() - 1)), 1);
    }
};

// ---------------------------------------------------------------- Sweep

struct Mines : Game {
    static const int W = 18, H = 12, COUNT = 34;
    bool mine[H][W], open[H][W], flag[H][W]; bool placed = false, won = false; int opened = 0; float bx = 0, by = 0, size = 0;
    Mines() : Game(L"mines", L"Sweep", L"Left click opens    Right click marks    R new field") { reset(); }
    void reset() override { memset(mine, 0, sizeof(mine)); memset(open, 0, sizeof(open)); memset(flag, 0, sizeof(flag)); placed = won = false; opened = 0; }
    std::wstring status() override { return won ? L"CLEARED  -  R FOR A NEW FIELD" : over ? L"BOOM  -  R TO RETRY" : L""; }
    int around(int x, int y) {
        int n = 0;
        for (int j = -1; j <= 1; j++) for (int i = -1; i <= 1; i++) {
            int cx = x + i, cy = y + j;
            if (cx >= 0 && cx < W && cy >= 0 && cy < H && mine[cy][cx]) n++;
        }
        return n;
    }
    void reveal(int x, int y) {
        if (x < 0 || x >= W || y < 0 || y >= H || open[y][x] || flag[y][x]) return;
        open[y][x] = true; opened++;
        if (mine[y][x]) { over = true; return; }
        score += 5;
        if (!around(x, y)) for (int j = -1; j <= 1; j++) for (int i = -1; i <= 1; i++) reveal(x + i, y + j);
    }
    void click(int button) override {
        if (size <= 0) return;
        int x = (int) std::floor((g_mouse.X - bx) / size), y = (int) std::floor((g_mouse.Y - by) / size);
        if (x < 0 || x >= W || y < 0 || y >= H || open[y][x]) return;
        if (button == 1) { flag[y][x] = !flag[y][x]; return; }
        if (flag[y][x]) return;
        if (!placed) {
            for (int n = 0; n < COUNT;) {
                int mx = rand() % W, my = rand() % H;
                if (mine[my][mx] || (abs(mx - x) <= 1 && abs(my - y) <= 1)) continue;
                mine[my][mx] = true; n++;
            }
            placed = true;
        }
        reveal(x, y);
        if (!over && opened == W * H - COUNT) { won = over = true; score += 500; }
    }
    void update(double) override {}
    void draw() override {
        size = std::floor(std::min(aw / W, ah / H)); bx = ax + (aw - size * W) / 2; by = ay + (ah - size * H) / 2;
        well(bx - 6, by - 6, size * W + 12, size * H + 12);
        const Color numbers[8] = {TEAL, GREEN, PINK, VIOLET, MINT, PINK, TEXT, TEXT};
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            float cx = bx + x * size, cy = by + y * size;
            if (!open[y][x] && !(over && mine[y][x])) {
                bool hover = !over && g_mouse.X >= cx && g_mouse.X < cx + size && g_mouse.Y >= cy && g_mouse.Y < cy + size;
                cell(cx, cy, size, mixColor(VIOLET, TEAL, (x + y) / (double) (W + H)), hover ? .78 : .5);
                if (flag[y][x]) circle(cx + size / 2, cy + size / 2, size * .2f, PINK);
            } else if (mine[y][x]) {
                glow(cx + size / 2, cy + size / 2, size * .26f, PINK);
                circle(cx + size / 2, cy + size / 2, size * .24f, won ? GREEN : PINK);
            } else {
                fillRound(*G, RectF(cx + 1, cy + 1, size - 2, size - 2), size * .18f, rgba(255, 255, 255, 13));
                int n = around(x, y);
                if (n) label(std::to_wstring(n), cx + size / 2, cy + size / 2, std::min(16.f, size * .5f), numbers[n - 1]);
            }
        }
    }
};

// ---------------------------------------------------------------- Merge

struct Merge : Game {
    int grid[4][4];
    Merge() : Game(L"merge", L"Merge", L"Arrows or WASD slide    R restart") { reset(); }
    void add() {
        int free = 0;
        for (auto& row : grid) for (int v : row) if (!v) free++;
        if (!free) return;
        int pick = rand() % free;
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) if (!grid[y][x] && pick-- == 0) grid[y][x] = rand() % 10 == 0 ? 4 : 2;
    }
    void reset() override { memset(grid, 0, sizeof(grid)); add(); add(); }
    void key(int k) override {
        int dx = k == VK_LEFT || k == 'A' ? -1 : k == VK_RIGHT || k == 'D' ? 1 : 0, dy = k == VK_UP || k == 'W' ? -1 : k == VK_DOWN || k == 'S' ? 1 : 0;
        if (!dx && !dy) return;
        bool moved = false;
        for (int line = 0; line < 4; line++) {
            int values[4], out[4] = {0, 0, 0, 0}, n = 0, last = 0;
            for (int i = 0; i < 4; i++) { int j = dx > 0 || dy > 0 ? 3 - i : i; values[i] = dx ? grid[line][j] : grid[j][line]; }
            for (int v : values) {
                if (!v) continue;
                if (last == v) { out[n - 1] = v * 2; score += v * 2; last = 0; } else { out[n++] = v; last = v; }
            }
            for (int i = 0; i < 4; i++) {
                int j = dx > 0 || dy > 0 ? 3 - i : i;
                if (values[i] != out[i]) moved = true;
                if (dx) grid[line][j] = out[i]; else grid[j][line] = out[i];
            }
        }
        if (!moved) return;
        add();
        over = true;
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++)
            if (!grid[y][x] || (x < 3 && grid[y][x] == grid[y][x + 1]) || (y < 3 && grid[y][x] == grid[y + 1][x])) over = false;
    }
    void update(double) override {}
    void draw() override {
        float size = std::floor(std::min(aw, ah) / 4.3f), bx = ax + (aw - size * 4) / 2, by = ay + (ah - size * 4) / 2;
        well(bx - 8, by - 8, size * 4 + 16, size * 4 + 16);
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) {
            float cx = bx + x * size, cy = by + y * size; int v = grid[y][x];
            if (!v) { fillRound(*G, RectF(cx + 5, cy + 5, size - 10, size - 10), size * .16f, rgba(255, 255, 255, 13)); continue; }
            int power = 0; for (int t = v; t > 1; t >>= 1) power++;
            cell(cx, cy, size, pal(power - 1), std::min(1.0, .5 + power * .06));
            label(std::to_wstring(v), cx + size / 2, cy + size / 2, v < 100 ? 30.f : v < 1000 ? 25.f : 20.f, power > 2 ? rgba(4, 35, 28) : TEXT);
        }
    }
};

// ---------------------------------------------------------------- Bounce

struct Bricks : Game {
    static const int COLS = 12, ROWS = 6;
    const double FW = 120, FH = 72, PADDLE = 18, PADDLE_Y = 66, BALL = 1.3;
    bool bricks[ROWS][COLS]; double paddle = 60, bx = 0, by = 0, vx = 0, vy = 0; float lastMouse = -1; bool stuck = true; int lives = 3, wave = 0;
    Bricks() : Game(L"bricks", L"Bounce", L"Mouse or arrows move    Space or click launches    P pause    R restart") { reset(); }
    void fill() { memset(bricks, 1, sizeof(bricks)); stuck = true; }
    void reset() override { lives = 3; wave = 0; paddle = FW / 2; fill(); }
    std::wstring status() override { return over ? L"OUT OF BALLS  -  R TO RESTART" : paused ? L"PAUSED  -  P TO CONTINUE" : L""; }
    void launch() { if (!stuck) return; stuck = false; double speed = 62 + wave * 6; vx = speed * .35; vy = -speed * .94; }
    void key(int k) override { if (k == VK_SPACE) launch(); }
    void click(int) override { launch(); }
    void update(double dt) override {
        double scale = aw / FW;
        if (inArea() && g_mouse.X != lastMouse) { paddle = (g_mouse.X - ax) / scale; lastMouse = g_mouse.X; }
        if (held[VK_LEFT] || held['A']) paddle -= 90 * dt;
        if (held[VK_RIGHT] || held['D']) paddle += 90 * dt;
        paddle = std::max(PADDLE / 2, std::min(FW - PADDLE / 2, paddle));
        if (stuck) { bx = paddle; by = PADDLE_Y - BALL - .2; return; }
        for (int s = 0; s < 4; s++) {
            bx += vx * dt / 4; by += vy * dt / 4;
            if (bx < BALL) { bx = BALL; vx = fabs(vx); }
            if (bx > FW - BALL) { bx = FW - BALL; vx = -fabs(vx); }
            if (by < BALL) { by = BALL; vy = fabs(vy); }
            if (vy > 0 && by + BALL >= PADDLE_Y && by < PADDLE_Y + 2 && fabs(bx - paddle) <= PADDLE / 2 + BALL) {
                double speed = hypot(vx, vy), angle = (bx - paddle) / (PADDLE / 2) * 1.05;
                vx = sin(angle) * speed; vy = -cos(angle) * speed; by = PADDLE_Y - BALL;
            }
            if (by > FH + 4) { if (--lives <= 0) over = true; stuck = true; return; }
            double bw = FW / COLS, bh = 3.4, top = 8;
            int col = (int) floor(bx / bw), row = (int) floor((by - top) / bh);
            if (row >= 0 && row < ROWS && col >= 0 && col < COLS && bricks[row][col]) {
                bricks[row][col] = false; score += 10 * (ROWS - row);
                double cx = (col + .5) * bw, cy = top + (row + .5) * bh;
                if (fabs(bx - cx) / bw > fabs(by - cy) / bh) vx = -vx; else vy = -vy;
                bool any = false;
                for (auto& r : bricks) for (bool b : r) any = any || b;
                if (!any) { wave++; fill(); return; }
            }
        }
    }
    void draw() override {
        float scale = (float) std::min(aw / FW, ah / FH), ox = ax + (aw - (float) FW * scale) / 2, oy = ay + (ah - (float) FH * scale) / 2;
        well(ox, oy, (float) FW * scale, (float) FH * scale);
        float bw = (float) FW / COLS * scale, bh = 3.4f * scale;
        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++)
            if (bricks[r][c]) fillRound(*G, RectF(ox + c * bw + 2, oy + 8 * scale + r * bh + 2, bw - 4, bh - 4), bh * .3f, pal(r));
        float px = ox + (float) (paddle - PADDLE / 2) * scale, py = oy + (float) PADDLE_Y * scale;
        fillRound(*G, RectF(px, py, (float) PADDLE * scale, 2 * scale), scale, MINT);
        glow(ox + (float) bx * scale, oy + (float) by * scale, (float) BALL * scale, TEXT);
        circle(ox + (float) bx * scale, oy + (float) by * scale, (float) BALL * scale, TEXT);
        for (int i = 0; i < lives; i++) circle(ox + 14 + i * 16.f, oy + (float) FH * scale - 12, 4.5f, PINK);
    }
};

// ---------------------------------------------------------------- Dig

struct Dig : Game {
    static const int W = 360, H = 128;
    enum : unsigned char { AIR, MOSS, SOIL, STONE, SAND, TRUNK, LEAF, GLOW, CRYSTAL, EMBER, LAMP, CORE };
    const double PW = .62, PH = 1.7, REACH = 5.5;
    std::vector<unsigned char> tiles; int surface[W]; int have[12];
    double px = 0, py = 0, vx = 0, vy = 0, digProgress = 0, walk = 0, armed = 0, saveTimer = 0; bool grounded = false, loaded = false, facingLeft = false;
    int slot = 0, digX = -1, digY = -1; unsigned seed = 1; float tile = 0; double camX = 0, camY = 0;
    Dig() : Game(L"dig", L"Dig", L"A D walk    W or Space jump    Hold left click dig    Right click place    1-9 or wheel pick    N twice: new world"), tiles(W * H) {}
    bool ranked() override { return false; }
    std::wstring status() override { return L""; }
    void reset() override {}

    static unsigned char slotKind(int i) { static const unsigned char S[9] = {SOIL, STONE, SAND, TRUNK, LEAF, GLOW, CRYSTAL, EMBER, LAMP}; return S[i]; }
    unsigned char get(int x, int y) { return x < 0 || x >= W || y >= H ? (unsigned char) CORE : y < 0 ? (unsigned char) AIR : tiles[y * W + x]; }
    void set(int x, int y, unsigned char t) { if (x >= 0 && x < W && y >= 0 && y < H) tiles[y * W + x] = t; }
    static bool solid(unsigned char t) { return t != AIR && t != LEAF && t != LAMP && t != TRUNK; }
    double hash(int i, int salt) {
        unsigned long long h = (unsigned long long) (long long) i * 374761393ULL + (unsigned long long) salt * 668265263ULL + seed * 2246822519ULL;
        h = (h ^ (h >> 13)) * 1274126177ULL;
        return ((h ^ (h >> 16)) & 0xFFFFFF) / (double) 0x1000000;
    }
    double noise(double x, int salt) { int i = (int) floor(x); double f = x - i, s = f * f * (3 - 2 * f); return hash(i, salt) * (1 - s) + hash(i + 1, salt) * s; }
    double noise2(double x, double y, int salt) {
        int i = (int) floor(x), j = (int) floor(y); double fx = x - i, fy = y - j, sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
        double a = hash(i + j * 7919, salt), b = hash(i + 1 + j * 7919, salt), c = hash(i + (j + 1) * 7919, salt), d = hash(i + 1 + (j + 1) * 7919, salt);
        return (a * (1 - sx) + b * sx) * (1 - sy) + (c * (1 - sx) + d * sx) * sy;
    }
    bool blocked(double x, double y) {
        int x0 = (int) floor(x - PW / 2), x1 = (int) floor(x + PW / 2 - 1e-6), y0 = (int) floor(y - PH), y1 = (int) floor(y - 1e-6);
        for (int j = y0; j <= y1; j++) for (int i = x0; i <= x1; i++) if (solid(get(i, j))) return true;
        return false;
    }
    void generate() {
        seed = (unsigned) (nowMs() & 0xFFFFFF) + 1;
        std::fill(tiles.begin(), tiles.end(), (unsigned char) AIR);
        memset(have, 0, sizeof(have)); have[LAMP] = 16;
        for (int x = 0; x < W; x++) {
            int top = (int) lround(38 + noise(x / 28.0, 1) * 16 + noise(x / 9.0, 2) * 5);
            surface[x] = top;
            bool sandy = noise(x / 22.0, 3) > .68;
            for (int y = top; y < H; y++) {
                int depth = y - top;
                unsigned char t = depth == 0 ? (sandy ? SAND : MOSS) : depth < 4 + (int) (noise(x / 5.0, 4) * 3) ? (sandy ? SAND : SOIL) : STONE;
                if (depth > 6) {
                    double cave = noise2(x / 11.0, y / 8.0, 5) * .65 + noise2(x / 5.0, y / 4.0, 6) * .35;
                    if (cave > .60) t = AIR;
                    else if (t == STONE) {
                        double r = hash(x * 131 + y * 7, 7), deep = (double) depth / (H - top);
                        if (r < .012 + deep * .012) t = GLOW;
                        else if (r < .018 + deep * .03 && deep > .3) t = CRYSTAL;
                        else if (r < .022 + deep * .045 && deep > .55) t = EMBER;
                    }
                }
                if (y >= H - 2) t = CORE;
                tiles[y * W + x] = t;
            }
        }
        for (int x = 4; x < W - 4; x++) {
            if (hash(x, 8) > .9 && get(x, surface[x]) == MOSS) {
                int height = 4 + (int) (hash(x, 9) * 4), top = surface[x] - height;
                for (int y = surface[x] - 1; y >= top; y--) set(x, y, TRUNK);
                for (int j = -3; j <= 1; j++) for (int i = -3; i <= 3; i++) if (i * i + j * j * 1.6 <= 9.5 && get(x + i, top + j) == AIR) set(x + i, top + j, LEAF);
                x += 3;
            }
        }
        px = W / 2 + .5; py = surface[W / 2] - .01; vx = vy = 0;
        while (blocked(px, py)) py -= 1;
    }
    std::wstring path() { std::wstring p(g_configPath); return p.substr(0, p.find_last_of(L'\\')) + L"\\dig.dat"; }
    void save() {
        if (!loaded) return;
        FILE* f = _wfopen(path().c_str(), L"wb");
        if (!f) return;
        int header[3] = {1, W, H};
        fwrite(header, sizeof(header), 1, f); fwrite(&seed, sizeof(seed), 1, f); fwrite(tiles.data(), 1, tiles.size(), f);
        fwrite(surface, sizeof(surface), 1, f); fwrite(have, sizeof(have), 1, f); fwrite(&px, sizeof(px), 1, f); fwrite(&py, sizeof(py), 1, f); fwrite(&slot, sizeof(slot), 1, f);
        fclose(f);
    }
    void load() {
        loaded = true;
        FILE* f = _wfopen(path().c_str(), L"rb");
        int header[3] = {0, 0, 0}; bool ok = false;
        if (f) {
            ok = fread(header, sizeof(header), 1, f) == 1 && header[0] == 1 && header[1] == W && header[2] == H && fread(&seed, sizeof(seed), 1, f) == 1
                && fread(tiles.data(), 1, tiles.size(), f) == tiles.size() && fread(surface, sizeof(surface), 1, f) == 1 && fread(have, sizeof(have), 1, f) == 1
                && fread(&px, sizeof(px), 1, f) == 1 && fread(&py, sizeof(py), 1, f) == 1 && fread(&slot, sizeof(slot), 1, f) == 1;
            fclose(f);
        }
        if (!ok) generate();
        slot = ((slot % 9) + 9) % 9;
    }
    void leave() override { save(); }

    bool target(int& tx, int& ty) {
        if (tile <= 0 || !inArea()) return false;
        double wx = camX + (g_mouse.X - ax) / tile, wy = camY + (g_mouse.Y - ay) / tile;
        if (hypot(wx - px, wy - (py - PH / 2)) > REACH) return false;
        tx = (int) floor(wx); ty = (int) floor(wy);
        return true;
    }
    void key(int k) override {
        if (k >= '1' && k <= '9') slot = k - '1';
        if (k == 'N') { if (armed > 0) { generate(); armed = 0; save(); } else armed = 2.5; }
    }
    void scroll(int amount) override { slot = (((slot - (amount > 0 ? 1 : -1)) % 9) + 9) % 9; }
    void click(int button) override {
        if (button != 1) return;
        int tx, ty; unsigned char kind = slotKind(slot);
        if (!target(tx, ty) || get(tx, ty) != AIR || have[kind] <= 0) return;
        if (get(tx - 1, ty) == AIR && get(tx + 1, ty) == AIR && get(tx, ty - 1) == AIR && get(tx, ty + 1) == AIR) return;
        set(tx, ty, kind);
        if (solid(kind) && blocked(px, py)) { set(tx, ty, AIR); return; }
        have[kind]--;
    }
    void update(double dt) override {
        if (!loaded) load();
        if (armed > 0) armed -= dt;
        saveTimer += dt;
        if (saveTimer > 30) { saveTimer = 0; save(); }
        double want = ((held['D'] || held[VK_RIGHT]) ? 1 : 0) - ((held['A'] || held[VK_LEFT]) ? 1 : 0);
        vx += (want * 6.2 - vx) * std::min(1.0, dt * (grounded ? 14 : 6));
        if (want != 0) { facingLeft = want < 0; walk += dt * 9; }
        if ((held['W'] || held[VK_SPACE] || held[VK_UP]) && grounded) { vy = -11.8; grounded = false; }
        vy = std::min(30.0, vy + 34 * dt);
        int steps = (int) ceil(std::max(fabs(vx), fabs(vy)) * dt / .4) + 1;
        bool wasGrounded = grounded; grounded = false;
        for (int s = 0; s < steps; s++) {
            double nx = px + vx * dt / steps;
            if (!blocked(nx, py)) px = nx;
            else if ((wasGrounded || grounded) && !blocked(nx, py - 1.02) && blocked(px, py + .05)) { px = nx; py -= 1.02; }
            else vx = 0;
            double ny = py + vy * dt / steps;
            if (!blocked(px, ny)) py = ny;
            else { if (vy > 0) { grounded = true; py = floor(ny + 1e-6) - 1e-4; if (blocked(px, py)) py = floor(py) - 1e-4; } vy = 0; }
        }
        if (!grounded && blocked(px, py + .06)) grounded = true;
        px = std::max(PW, std::min(W - PW, px));

        static const double hard[12] = {0, .22, .22, .6, .18, .45, .08, .9, 1.0, .9, .2, 1e9};
        int tx, ty;
        if (!leftDown || !target(tx, ty) || get(tx, ty) == AIR || get(tx, ty) == CORE) { digX = -1; digProgress = 0; }
        else {
            if (tx != digX || ty != digY) { digX = tx; digY = ty; digProgress = 0; }
            digProgress += dt / hard[get(digX, digY)];
            if (digProgress >= 1) { unsigned char got = get(digX, digY); have[got == MOSS ? SOIL : got]++; set(digX, digY, AIR); digX = -1; digProgress = 0; }
        }
    }
    Color colorOf(unsigned char t, int x, int y) {
        double v = .9 + hash(x * 31 + y * 17, 20) * .12;
        auto shade = [v](int r, int g, int b) { return rgba((int) (r * v), (int) (g * v), (int) (b * v)); };
        switch (t) {
            case MOSS: return mixColor(rgba(40, 120, 96), GREEN, .35 + (v - .9));
            case SOIL: return shade(96, 74, 86);
            case STONE: return shade(62, 70, 96);
            case SAND: return shade(196, 178, 150);
            case TRUNK: return shade(120, 86, 78);
            case LEAF: return withAlpha(mixColor(rgba(30, 110, 100), TEAL, .4 + (v - .9)), 210);
            case GLOW: return shade(52, 62, 90);
            case CRYSTAL: return shade(56, 54, 96);
            case EMBER: return shade(70, 52, 78);
            case LAMP: return mixColor(MINT, WHITE, .5);
            default: return rgba(20, 22, 34);
        }
    }
    void drawTile(unsigned char t, int x, int y, float sx, float sy, float size, double light) {
        Color c = colorOf(t, x, y);
        quad(sx, sy, size + .5f, size + .5f, Color(c.GetA(), (BYTE) (c.GetR() * light), (BYTE) (c.GetG() * light), (BYTE) (c.GetB() * light)));
        if (t == GLOW || t == CRYSTAL || t == EMBER) {
            Color gem = t == GLOW ? TEAL : t == CRYSTAL ? VIOLET : PINK;
            for (int k = 0; k < 3; k++) {
                float gx = sx + size * (float) (.2 + hash(x * 3 + k, y + 30) * .55), gy = sy + size * (float) (.2 + hash(x + k * 5, y * 3 + 31) * .55), d = size * .16f;
                PointF points[4] = {{gx, gy - d}, {gx + d, gy}, {gx, gy + d}, {gx - d, gy}};
                SolidBrush brush(gem); G->FillPolygon(&brush, points, 4);
            }
        } else if (t == LAMP) glow(sx + size / 2, sy + size / 2, size * .7f, MINT);
        if (t != LEAF && t != LAMP && t != TRUNK && !solid(get(x, y - 1))) quad(sx, sy, size + .5f, std::max(1.f, size * .12f), rgba(255, 255, 255, (int) (40 * light)));
    }
    void draw() override {
        if (!loaded) load();
        tile = std::max(8.f, std::floor(ah / 24));
        double cols = aw / tile, rows = ah / tile;
        camX = std::max(0.0, std::min(W - cols, px - cols / 2)); camY = std::max(0.0, std::min(H - rows, py - PH / 2 - rows / 2));
        { LinearGradientBrush sky(PointF(0, ay - 1), PointF(0, ay + ah + 1), mixColor(rgba(8, 14, 40), VIOLET, .16), mixColor(rgba(10, 26, 50), TEAL, .2)); G->FillRectangle(&sky, ax, ay, aw, ah); }
        G->SetSmoothingMode(SmoothingModeNone);
        int x0 = (int) floor(camX), x1 = (int) ceil(camX + cols), y0 = (int) floor(camY), y1 = (int) ceil(camY + rows);
        for (int x = std::max(0, x0); x <= x1 && x < W; x++) for (int y = std::max(0, y0); y <= y1 && y < H; y++) {
            unsigned char t = tiles[y * W + x];
            float sx = ax + (float) ((x - camX) * tile), sy = ay + (float) ((y - camY) * tile);
            int depth = y - surface[x]; double light = depth <= 0 ? 1 : std::max(.42, 1 - depth * .018);
            if (t == AIR || t == LEAF || t == LAMP || t == TRUNK) {
                if (depth > 0) quad(sx, sy, tile + .5f, tile + .5f, rgba((int) (26 * light), (int) (28 * light), (int) (44 * light)));
                if (t == AIR) continue;
            }
            drawTile(t, x, y, sx, sy, tile, light);
        }
        G->SetSmoothingMode(SmoothingModeAntiAlias);
        float fx = ax + (float) ((px - camX) * tile), fy = ay + (float) ((py - camY) * tile), bw = (float) PW * tile, bh = (float) PH * tile;
        float swing = fabs(vx) > .4 && grounded ? (float) sin(walk) * bw * .22f : 0;
        Color leg = mixColor(VIOLET, rgba(20, 20, 50), .4);
        quad(fx - bw * .32f + swing, fy - bh * .3f, bw * .26f, bh * .3f, leg); quad(fx + bw * .06f - swing, fy - bh * .3f, bw * .26f, bh * .3f, leg);
        fillRound(*G, RectF(fx - bw / 2, fy - bh * .72f, bw, bh * .46f), bw * .2f, TEAL);
        fillRound(*G, RectF(fx - bw * .44f, fy - bh, bw * .88f, bh * .3f), bw * .24f, TEXT);
        quad(fx + (facingLeft ? -bw * .4f : bw * .02f), fy - bh * .9f, bw * .38f, bh * .08f, rgba(10, 20, 40));
        int tx, ty;
        if (target(tx, ty)) {
            float sx = ax + (float) ((tx - camX) * tile), sy = ay + (float) ((ty - camY) * tile);
            Pen pen(withAlpha(TEXT, 180), 1.4f); G->DrawRectangle(&pen, sx, sy, tile, tile);
            if (tx == digX && ty == digY && digProgress > 0) quad(sx, sy + tile * (float) (1 - digProgress), tile, tile * (float) digProgress, rgba(255, 255, 255, 76));
        }
        float slotSize = 46, total = slotSize * 9 + 8 * 6, hx = ax + (aw - total) / 2, hy = ay + ah - slotSize - 12;
        fillRound(*G, RectF(hx - 10, hy - 8, total + 20, slotSize + 16), 16, rgba(4, 10, 26, 184));
        for (int i = 0; i < 9; i++) {
            float x = hx + i * (slotSize + 6); unsigned char kind = slotKind(i);
            fillRound(*G, RectF(x, hy, slotSize, slotSize), 10, rgba(255, 255, 255, i == slot ? 40 : 13));
            if (i == slot) { GraphicsPath p; rounded(p, RectF(x, hy, slotSize, slotSize), 10); Pen pen(GREEN, 1.6f); G->DrawPath(&pen, &p); }
            float in = slotSize * .24f;
            drawTile(kind, i * 3, 200, x + in, hy + in * .7f, slotSize - in * 2, have[kind] > 0 ? 1 : .35);
            text(*G, std::to_wstring(have[kind]), x + 5, hy + slotSize - 16, 10.5f, have[kind] > 0 ? TEXT : FAINT, true);
        }
        static const wchar_t* names[12] = {L"", L"Moss", L"Soil", L"Stone", L"Sand", L"Trunk", L"Leaf", L"Glow ore", L"Crystal", L"Ember", L"Lamp", L"Core"};
        int column = (int) std::max(0.0, std::min(W - 1.0, px));
        std::wstring info = armed > 0 ? L"Press N again to replace this world with a new one" : std::wstring(names[slotKind(slot)]) + L"    depth " + std::to_wstring(std::max(0, (int) (py - surface[column])));
        text(*G, info, ax + 14, ay + 10, 12.5f, armed > 0 ? PINK : DIM);
    }
};

// ---------------------------------------------------------------- pane

static std::vector<Game*> list;
static int current = 0;
static ULONGLONG lastFrame = 0;

static void ensure() {
    if (list.empty()) list = {new Blocks(), new Snake(), new Mines(), new Merge(), new Bricks(), new Dig()};
}

static int bestOf(Game* game) { return GetPrivateProfileIntW(L"games", game->id, 0, g_configPath); }

static void keepBest(Game* game) {
    if (!game->ranked() || game->score <= bestOf(game)) return;
    WritePrivateProfileStringW(L"games", game->id, std::to_wstring(game->score).c_str(), g_configPath);
}

/** The menu is closing or going back to the modules. */
static void leave() {
    memset(held, 0, sizeof(held)); leftDown = false; lastFrame = 0;
    if (list.empty()) return;
    list[current]->leave();
    for (Game* game : list) { keepBest(game); if (game->ranked() && !game->over && game->score > 0) game->paused = true; }
}

static void render(Graphics& g, float w, float h) {
    ensure();
    G = &g;
    ULONGLONG now = nowMs();
    double dt = lastFrame == 0 ? 0 : std::min(.05, (now - lastFrame) / 1000.0);
    lastFrame = now;

    float pw = std::min(1180.f, w - 68), ph = std::min(600.f, h - 150), x = (w - pw) / 2, y = 84 + (h - 84 - 50 - ph) / 2;
    panel(g, RectF(x, y, pw, ph), 28, w, h);
    float lx = x + 22, lw = 190;
    text(g, L"GAMES", lx + 6, y + 22, 12, GREEN, true);
    for (size_t i = 0; i < list.size(); i++) {
        RectF row(lx, y + 50 + i * 46.f, lw, 40);
        bool on = (int) i == current;
        fillRound(g, row, 10, on ? withAlpha(GREEN, 50) : rgba(226, 244, 255, hovered(row) ? 28 : 12));
        std::wstring name(list[i]->title); for (auto& ch : name) ch = towupper(ch);
        text(g, name, row.X + 16, row.Y + 11, 14, on ? TEXT : DIM, on);
        int index = (int) i;
        hit(row, [index](int) { if (index != current) { list[current]->leave(); keepBest(list[current]); current = index; memset(held, 0, sizeof(held)); } });
    }
    { Pen pen(rgba(226, 244, 255, 36), 1); g.DrawLine(&pen, lx + lw + 14, y + 24, lx + lw + 14, y + ph - 24); }

    Game* game = list[current];
    float gx = lx + lw + 30, gw = x + pw - 24 - gx;
    std::wstring title(game->title); for (auto& ch : title) ch = towupper(ch);
    text(g, title, gx, y + 20, 20, TEXT, true);
    if (game->ranked()) {
        std::wstring score = L"SCORE " + std::to_wstring(game->score) + L"    BEST " + std::to_wstring(std::max(game->score, bestOf(game)));
        text(g, score, gx + gw - textWidth(g, score, 13, true), y + 24, 13, GREEN, true);
    }
    text(g, game->help, gx, y + 50, 12, FAINT);

    ax = gx; ay = y + 76; aw = gw; ah = y + ph - 22 - ay;
    if (!game->over && !game->paused) game->update(dt);
    if (game->over) keepBest(game);
    GraphicsPath clip; rounded(clip, RectF(ax, ay, aw, ah), 10);
    g.SetClip(&clip);
    game->draw();
    g.ResetClip();
    std::wstring status = game->status();
    if (!status.empty()) {
        float sw = textWidth(g, status, 16, true) + 56;
        RectF box(ax + (aw - sw) / 2, ay + ah / 2 - 26, sw, 52);
        fillRound(g, box, 26, rgba(4, 10, 26, 220));
        GraphicsPath p; rounded(p, box, 26); Pen pen(withAlpha(GREEN, 180), 1.2f); g.DrawPath(&pen, &p);
        text(g, status, box.X + 28, box.Y + 16, 16, TEXT, true);
    }
}

static void key(int vk, bool down) {
    ensure();
    if (vk < 0 || vk > 255) return;
    if (!down) { held[vk] = false; return; }
    bool repeat = held[vk];
    held[vk] = true;
    Game* game = list[current];
    if (vk == 'R' && game->ranked() && !repeat) { keepBest(game); game->over = game->paused = false; game->score = 0; game->reset(); return; }
    if (vk == 'P' && !repeat) { game->paused = !game->paused; return; }
    if (!game->over && !game->paused) game->key(vk);
}

static bool mouse(int button, bool down) {
    ensure();
    if (button == 0) leftDown = down;
    Game* game = list[current];
    if (down && inArea() && !game->over && !game->paused) game->click(button);
    return inArea();
}

static void scroll(int amount) {
    ensure();
    list[current]->scroll(amount);
}

}  // namespace games
