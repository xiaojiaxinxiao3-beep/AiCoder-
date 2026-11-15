package aicoder.assist.aurora;

import aicoder.assist.gfx.Color;

import java.util.Random;

/** Slide all tiles one way; two equal tiles that meet become one of twice the value. */
final class GameMerge extends Arcade.Game {
    private final int[][] grid = new int[4][4];
    private final Random random = new Random();

    GameMerge() {
        super("merge", "Merge", "Arrows or WASD slide    R restart");
        reset();
    }

    @Override void reset() {
        for (int[] row : grid) java.util.Arrays.fill(row, 0);
        add(); add();
    }

    private void add() {
        int free = 0;
        for (int[] row : grid) for (int v : row) if (v == 0) free++;
        if (free == 0) return;
        int pick = random.nextInt(free);
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) {
            if (grid[y][x] == 0 && pick-- == 0) grid[y][x] = random.nextInt(10) == 0 ? 4 : 2;
        }
    }

    @Override void key(int key) {
        int dx = key == 263 || key == 65 ? -1 : key == 262 || key == 68 ? 1 : 0;
        int dy = key == 265 || key == 87 ? -1 : key == 264 || key == 83 ? 1 : 0;
        if (dx == 0 && dy == 0) return;
        boolean moved = false;
        for (int line = 0; line < 4; line++) {
            int[] values = new int[4];
            for (int i = 0; i < 4; i++) {
                int k = dx > 0 || dy > 0 ? 3 - i : i;
                values[i] = dx != 0 ? grid[line][k] : grid[k][line];
            }
            int[] out = new int[4];
            int n = 0, last = 0;
            for (int v : values) {
                if (v == 0) continue;
                if (last == v) { out[n - 1] = v * 2; score += v * 2; last = 0; }
                else { out[n++] = v; last = v; }
            }
            for (int i = 0; i < 4; i++) {
                int k = dx > 0 || dy > 0 ? 3 - i : i;
                if (values[i] != out[i]) moved = true;
                if (dx != 0) grid[line][k] = out[i]; else grid[k][line] = out[i];
            }
        }
        if (!moved) return;
        add();
        over = stuck();
    }

    private boolean stuck() {
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) {
            if (grid[y][x] == 0) return false;
            if (x < 3 && grid[y][x] == grid[y][x + 1]) return false;
            if (y < 3 && grid[y][x] == grid[y + 1][x]) return false;
        }
        return true;
    }

    @Override void update(double dt) {
    }

    @Override void draw() {
        double size = Math.floor(Math.min(Arcade.aw, Arcade.ah) / 4.3);
        double bx = Arcade.ax + (Arcade.aw - size * 4) / 2, by = Arcade.ay + (Arcade.ah - size * 4) / 2;
        Arcade.well(bx - 8, by - 8, size * 4 + 16, size * 4 + 16);
        Color[] pal = Arcade.palette();
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) {
            double cx = bx + x * size, cy = by + y * size;
            int v = grid[y][x];
            if (v == 0) { Fx.box(Arcade.R, cx + 5, cy + 5, size - 10, size - 10, size * 0.16, Fx.rgba(255, 255, 255, 0.05)); continue; }
            int power = 31 - Integer.numberOfLeadingZeros(v);
            Arcade.cell(cx, cy, size, pal[(power - 1) % pal.length], Math.min(1, 0.5 + power * 0.06));
            double font = v < 100 ? 30 : v < 1000 ? 25 : 20;
            Arcade.label(String.valueOf(v), cx + size / 2, Fx.centerY(Arcade.theme, cy + size / 2, font, Arcade.u), font, power > 2 ? Arcade.S.dark : Arcade.S.title, true);
        }
    }
}
