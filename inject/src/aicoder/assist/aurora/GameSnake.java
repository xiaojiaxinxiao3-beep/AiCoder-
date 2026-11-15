package aicoder.assist.aurora;

import java.util.ArrayDeque;
import java.util.Random;

/** Steer the line to the dots; it grows with each one. Hitting a wall or itself ends the run. */
final class GameSnake extends Arcade.Game {
    private static final int W = 28, H = 17;
    private final ArrayDeque<int[]> body = new ArrayDeque<>();
    private final Random random = new Random();
    private int dx, dy, nextDx, nextDy, foodX, foodY;
    private double timer;

    GameSnake() {
        super("snake", "Trail", "Arrows or WASD steer    P pause    R restart");
        reset();
    }

    @Override void reset() {
        body.clear();
        for (int i = 0; i < 4; i++) body.addLast(new int[]{W / 2 - i, H / 2});
        dx = nextDx = 1; dy = nextDy = 0; timer = 0;
        food();
    }

    private void food() {
        do { foodX = random.nextInt(W); foodY = random.nextInt(H); } while (on(foodX, foodY));
    }

    private boolean on(int x, int y) {
        for (int[] p : body) if (p[0] == x && p[1] == y) return true;
        return false;
    }

    @Override void key(int key) {
        int x = key == 263 || key == 65 ? -1 : key == 262 || key == 68 ? 1 : 0;
        int y = key == 265 || key == 87 ? -1 : key == 264 || key == 83 ? 1 : 0;
        if ((x != 0 || y != 0) && (x != -dx || y != -dy)) { nextDx = x; nextDy = y; }
    }

    @Override void update(double dt) {
        timer += dt;
        double interval = Math.max(0.055, 0.13 - body.size() * 0.0012);
        while (timer >= interval && !over) {
            timer -= interval;
            dx = nextDx; dy = nextDy;
            int[] head = body.peekFirst();
            int x = head[0] + dx, y = head[1] + dy;
            boolean eat = x == foodX && y == foodY;
            if (!eat) body.removeLast();
            if (x < 0 || x >= W || y < 0 || y >= H || on(x, y)) { over = true; return; }
            body.addFirst(new int[]{x, y});
            if (eat) { score += 10; food(); }
        }
    }

    @Override void draw() {
        double size = Math.floor(Math.min(Arcade.aw / W, Arcade.ah / H));
        double bx = Arcade.ax + (Arcade.aw - size * W) / 2, by = Arcade.ay + (Arcade.ah - size * H) / 2;
        Arcade.well(bx - 6, by - 6, size * W + 12, size * H + 12);
        double pulse = 0.75 + 0.25 * Math.sin(System.nanoTime() / 2.5e8);
        Fx.glowDot(Arcade.R, bx + (foodX + 0.5) * size, by + (foodY + 0.5) * size, size * 0.3, size * 0.7 * pulse, Arcade.S.pink, 1);
        Fx.circle(Arcade.R, bx + (foodX + 0.5) * size, by + (foodY + 0.5) * size, size * 0.32, Arcade.S.pink);
        int i = 0, n = body.size();
        for (int[] p : body) {
            Arcade.cell(bx + p[0] * size, by + p[1] * size, size, Fx.mix(Arcade.S.green, Arcade.S.violet, (double) i / Math.max(1, n - 1)), 1);
            i++;
        }
    }
}
