package aicoder.assist.aurora;

import aicoder.assist.gfx.Color;

import java.util.Random;

/** Open every safe cell. A number says how many of its neighbours are mined; right click marks a cell. */
final class GameMines extends Arcade.Game {
    private static final int W = 18, H = 12, MINES = 34;
    private final boolean[][] mine = new boolean[H][W], open = new boolean[H][W], flag = new boolean[H][W];
    private boolean placed, won;
    private int opened;
    private double bx, by, size;

    GameMines() {
        super("mines", "Sweep", "Left click opens    Right click marks    R new field");
        reset();
    }

    @Override void reset() {
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) { mine[y][x] = open[y][x] = flag[y][x] = false; }
        placed = false; won = false; opened = 0;
    }

    @Override String status() {
        return won ? "CLEARED  -  R FOR A NEW FIELD" : over ? "BOOM  -  R TO RETRY" : "";
    }

    private void place(int safeX, int safeY) {
        Random random = new Random();
        for (int n = 0; n < MINES; ) {
            int x = random.nextInt(W), y = random.nextInt(H);
            if (mine[y][x] || (Math.abs(x - safeX) <= 1 && Math.abs(y - safeY) <= 1)) continue;
            mine[y][x] = true; n++;
        }
        placed = true;
    }

    private int around(int x, int y) {
        int n = 0;
        for (int j = -1; j <= 1; j++) for (int i = -1; i <= 1; i++) {
            int cx = x + i, cy = y + j;
            if (cx >= 0 && cx < W && cy >= 0 && cy < H && mine[cy][cx]) n++;
        }
        return n;
    }

    private void reveal(int x, int y) {
        if (x < 0 || x >= W || y < 0 || y >= H || open[y][x] || flag[y][x]) return;
        open[y][x] = true; opened++;
        if (mine[y][x]) { over = true; return; }
        score += 5;
        if (around(x, y) == 0) for (int j = -1; j <= 1; j++) for (int i = -1; i <= 1; i++) reveal(x + i, y + j);
    }

    @Override void click(int button) {
        if (size <= 0) return;
        int x = (int) Math.floor((Arcade.mx - bx) / size), y = (int) Math.floor((Arcade.my - by) / size);
        if (x < 0 || x >= W || y < 0 || y >= H || open[y][x]) return;
        if (button == 1) { flag[y][x] = !flag[y][x]; return; }
        if (button != 0 || flag[y][x]) return;
        if (!placed) place(x, y);
        reveal(x, y);
        if (!over && opened == W * H - MINES) { won = true; over = true; score += 500; }
    }

    @Override void update(double dt) {
    }

    @Override void draw() {
        size = Math.floor(Math.min(Arcade.aw / W, Arcade.ah / H));
        bx = Arcade.ax + (Arcade.aw - size * W) / 2; by = Arcade.ay + (Arcade.ah - size * H) / 2;
        Arcade.well(bx - 6, by - 6, size * W + 12, size * H + 12);
        Color[] numbers = {Arcade.S.teal, Arcade.S.green, Arcade.S.pink, Arcade.S.violet, Arcade.S.mint, Arcade.S.pink, Arcade.S.title, Arcade.S.title};
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            double cx = bx + x * size, cy = by + y * size;
            if (!open[y][x] && !(over && mine[y][x])) {
                boolean hover = !over && Arcade.mx >= cx && Arcade.mx < cx + size && Arcade.my >= cy && Arcade.my < cy + size;
                Arcade.cell(cx, cy, size, Fx.mix(Arcade.S.violet, Arcade.S.teal, (x + y) / (double) (W + H)), hover ? 0.75 : 0.5);
                if (flag[y][x]) Fx.circle(Arcade.R, cx + size / 2, cy + size / 2, size * 0.2, Arcade.S.pink);
            } else if (mine[y][x]) {
                Fx.glowDot(Arcade.R, cx + size / 2, cy + size / 2, size * 0.2, size * 0.5, Arcade.S.pink, won ? 0.3 : 1);
                Fx.circle(Arcade.R, cx + size / 2, cy + size / 2, size * 0.24, won ? Arcade.S.green : Arcade.S.pink);
            } else {
                Fx.box(Arcade.R, cx + 1, cy + 1, size - 2, size - 2, size * 0.18, Fx.rgba(255, 255, 255, 0.05));
                int n = around(x, y);
                if (n > 0) Arcade.label(String.valueOf(n), cx + size / 2, Fx.centerY(Arcade.theme, cy + size / 2, 15, Arcade.u), 15, numbers[n - 1], true);
            }
        }
    }
}
