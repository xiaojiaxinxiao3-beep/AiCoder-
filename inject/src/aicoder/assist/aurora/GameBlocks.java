package aicoder.assist.aurora;

import aicoder.assist.gfx.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Falling pieces of four cells; a full row clears. Seven pieces dealt from a shuffled bag. */
final class GameBlocks extends Arcade.Game {
    private static final int W = 10, H = 20;
    /** Cells of each piece around its turning point. */
    private static final int[][][] SHAPES = {
        {{-1, 0}, {0, 0}, {1, 0}, {2, 0}}, {{0, 0}, {1, 0}, {0, 1}, {1, 1}}, {{-1, 0}, {0, 0}, {1, 0}, {0, -1}},
        {{-1, 0}, {0, 0}, {0, -1}, {1, -1}}, {{-1, -1}, {0, -1}, {0, 0}, {1, 0}}, {{-1, -1}, {-1, 0}, {0, 0}, {1, 0}}, {{-1, 0}, {0, 0}, {1, 0}, {1, -1}}};

    private final int[][] board = new int[H][W];
    private final List<Integer> bag = new ArrayList<>();
    private int piece, next, hold = -1, px, py, lines;
    private int[][] cells;
    private boolean held;
    private double fall;

    GameBlocks() {
        super("blocks", "Drop", "Arrows move and turn    Down soft drop    Space drop    C hold    P pause    R restart");
        reset();
    }

    @Override void reset() {
        for (int[] row : board) java.util.Arrays.fill(row, 0);
        bag.clear(); lines = 0; hold = -1; held = false;
        next = draw7();
        spawn(draw7());
    }

    private int draw7() {
        if (bag.isEmpty()) { for (int i = 0; i < 7; i++) bag.add(i); Collections.shuffle(bag); }
        return bag.remove(bag.size() - 1);
    }

    private void spawn(int kind) {
        piece = kind; px = 4; py = 1; fall = 0;
        cells = new int[4][2];
        for (int i = 0; i < 4; i++) { cells[i][0] = SHAPES[kind][i][0]; cells[i][1] = SHAPES[kind][i][1]; }
        if (!fits(cells, px, py)) over = true;
    }

    private boolean fits(int[][] c, int x, int y) {
        for (int[] p : c) {
            int cx = x + p[0], cy = y + p[1];
            if (cx < 0 || cx >= W || cy >= H) return false;
            if (cy >= 0 && board[cy][cx] != 0) return false;
        }
        return true;
    }

    private int level() { return lines / 10 + 1; }

    @Override void update(double dt) {
        fall += dt;
        double interval = Math.max(0.06, 0.8 - (level() - 1) * 0.07);
        while (fall >= interval && !over) { fall -= interval; step(); }
    }

    private void step() {
        if (fits(cells, px, py + 1)) { py++; return; }
        for (int[] p : cells) if (py + p[1] >= 0) board[py + p[1]][px + p[0]] = piece + 1;
        int cleared = 0;
        for (int y = H - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < W; x++) full &= board[y][x] != 0;
            if (!full) continue;
            for (int k = y; k > 0; k--) board[k] = board[k - 1].clone();
            board[0] = new int[W];
            cleared++; y++;
        }
        lines += cleared;
        score += new int[]{0, 100, 300, 500, 800}[cleared] * level();
        held = false;
        int n = next;
        next = draw7();
        spawn(n);
    }

    @Override void key(int key) {
        switch (key) {
            case 263 -> { if (fits(cells, px - 1, py)) px--; }
            case 262 -> { if (fits(cells, px + 1, py)) px++; }
            case 264 -> { if (fits(cells, px, py + 1)) { py++; score++; fall = 0; } }
            case 265, 88 -> turn();
            case 32 -> { while (fits(cells, px, py + 1)) { py++; score += 2; } step(); }
            case 67 -> {
                if (held) return;
                int old = hold;
                hold = piece; held = true;
                if (old < 0) { int n = next; next = draw7(); spawn(n); } else spawn(old);
            }
            default -> { }
        }
    }

    private void turn() {
        if (piece == 1) return;
        int[][] t = new int[4][2];
        for (int i = 0; i < 4; i++) { t[i][0] = -cells[i][1]; t[i][1] = cells[i][0]; }
        for (int kick : new int[]{0, -1, 1, -2, 2}) {
            if (fits(t, px + kick, py)) { cells = t; px += kick; return; }
        }
    }

    @Override void draw() {
        Color[] pal = Arcade.palette();
        double size = Math.floor(Math.min(Arcade.ah / H, Arcade.aw / 22));
        double bx = Arcade.ax + (Arcade.aw - size * W) / 2, by = Arcade.ay + (Arcade.ah - size * H) / 2;
        Arcade.well(bx - 6, by - 6, size * W + 12, size * H + 12);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            if (board[y][x] != 0) Arcade.cell(bx + x * size, by + y * size, size, pal[board[y][x] - 1], 1);
        }
        if (!over) {
            int gy = py;
            while (fits(cells, px, gy + 1)) gy++;
            for (int[] p : cells) if (gy + p[1] >= 0) Arcade.cell(bx + (px + p[0]) * size, by + (gy + p[1]) * size, size, pal[piece], 0.22);
            for (int[] p : cells) if (py + p[1] >= 0) Arcade.cell(bx + (px + p[0]) * size, by + (py + p[1]) * size, size, pal[piece], 1);
        }
        side("NEXT", next, bx + size * W + 30 * Arcade.u, by, size, pal);
        side("HOLD", hold, bx - 30 * Arcade.u - size * 4.4, by, size, pal);
        double tx = bx + size * W + 30 * Arcade.u, ty = by + size * 6.2;
        Arcade.text.put("LEVEL " + level(), tx, ty, 13, Arcade.S.dim, 1.6, 0);
        Arcade.text.put("LINES " + lines, tx, ty + 24 * Arcade.u, 13, Arcade.S.dim, 1.6, 0);
    }

    private void side(String name, int kind, double x, double y, double size, Color[] pal) {
        Arcade.text.put(name, x, y, 12, Arcade.S.green, 3, Fx.BOLD);
        Arcade.well(x, y + 24 * Arcade.u, size * 4.4, size * 3.4);
        if (kind < 0) return;
        double s = size * 0.8;
        for (int[] p : SHAPES[kind]) Arcade.cell(x + size * 1.5 + p[0] * s, y + 24 * Arcade.u + size * 1.6 + p[1] * s, s, pal[kind], 1);
    }
}
