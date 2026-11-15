package aicoder.assist.aurora;

import aicoder.assist.gfx.Color;

/** Keep the ball in play with the paddle and knock out every brick. The mouse or the arrows move the paddle. */
final class GameBricks extends Arcade.Game {
    private static final int COLS = 12, ROWS = 6;
    /** The field in its own units; scaled to the pane when drawn. */
    private static final double W = 120, H = 72, PADDLE_W = 18, PADDLE_Y = 66, BALL = 1.3;
    private final boolean[][] bricks = new boolean[ROWS][COLS];
    private double paddle, ballX, ballY, vx, vy, lastMouse = -1;
    private boolean stuck;
    private int lives, wave;

    GameBricks() {
        super("bricks", "Bounce", "Mouse or arrows move    Space or click launches    P pause    R restart");
        reset();
    }

    @Override void reset() {
        lives = 3; wave = 0; paddle = W / 2;
        fill();
    }

    private void fill() {
        for (boolean[] row : bricks) java.util.Arrays.fill(row, true);
        stuck = true;
    }

    @Override String status() {
        return over ? "OUT OF BALLS  -  R TO RESTART" : paused ? "PAUSED  -  P TO CONTINUE" : "";
    }

    @Override void key(int key) {
        if (key == 32) launch();
    }

    @Override void click(int button) {
        launch();
    }

    private void launch() {
        if (!stuck) return;
        stuck = false;
        double speed = 62 + wave * 6;
        vx = speed * 0.35; vy = -speed * 0.94;
    }

    @Override void update(double dt) {
        double scale = Arcade.aw / W;
        if (Arcade.inArea() && Arcade.mx != lastMouse) { paddle = (Arcade.mx - Arcade.ax) / scale; lastMouse = Arcade.mx; }
        if (Arcade.held.contains(263) || Arcade.held.contains(65)) paddle -= 90 * dt;
        if (Arcade.held.contains(262) || Arcade.held.contains(68)) paddle += 90 * dt;
        paddle = Math.max(PADDLE_W / 2, Math.min(W - PADDLE_W / 2, paddle));
        if (stuck) { ballX = paddle; ballY = PADDLE_Y - BALL - 0.2; return; }

        // Small steps so the ball cannot pass through a brick in one frame
        int steps = 4;
        for (int s = 0; s < steps; s++) {
            ballX += vx * dt / steps; ballY += vy * dt / steps;
            if (ballX < BALL) { ballX = BALL; vx = Math.abs(vx); }
            if (ballX > W - BALL) { ballX = W - BALL; vx = -Math.abs(vx); }
            if (ballY < BALL) { ballY = BALL; vy = Math.abs(vy); }
            if (vy > 0 && ballY + BALL >= PADDLE_Y && ballY < PADDLE_Y + 2 && Math.abs(ballX - paddle) <= PADDLE_W / 2 + BALL) {
                double speed = Math.hypot(vx, vy), off = (ballX - paddle) / (PADDLE_W / 2);
                double angle = off * 1.05;
                vx = Math.sin(angle) * speed; vy = -Math.cos(angle) * speed;
                ballY = PADDLE_Y - BALL;
            }
            if (ballY > H + 4) {
                if (--lives <= 0) over = true;
                stuck = true;
                return;
            }
            double bw = W / COLS, bh = 3.4, top = 8;
            int col = (int) Math.floor(ballX / bw), row = (int) Math.floor((ballY - top) / bh);
            if (row >= 0 && row < ROWS && col >= 0 && col < COLS && bricks[row][col]) {
                bricks[row][col] = false;
                score += 10 * (ROWS - row);
                double cx = (col + 0.5) * bw, cy = top + (row + 0.5) * bh;
                if (Math.abs(ballX - cx) / bw > Math.abs(ballY - cy) / bh) vx = -vx; else vy = -vy;
                boolean any = false;
                for (boolean[] r : bricks) for (boolean b : r) any |= b;
                if (!any) { wave++; fill(); return; }
            }
        }
    }

    @Override void draw() {
        double scale = Math.min(Arcade.aw / W, Arcade.ah / H), ox = Arcade.ax + (Arcade.aw - W * scale) / 2, oy = Arcade.ay + (Arcade.ah - H * scale) / 2;
        Arcade.well(ox, oy, W * scale, H * scale);
        Color[] pal = Arcade.palette();
        double bw = W / COLS * scale, bh = 3.4 * scale;
        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) {
            if (!bricks[r][c]) continue;
            double x = ox + c * bw, y = oy + 8 * scale + r * bh;
            Fx.box(Arcade.R, x + 2, y + 2, bw - 4, bh - 4, bh * 0.3, Fx.mix(pal[r % pal.length], new Color(255, 255, 255, 255), 0.2), pal[r % pal.length]);
        }
        double px = ox + (paddle - PADDLE_W / 2) * scale, py = oy + PADDLE_Y * scale;
        Fx.glowRect(Arcade.R, px + 6, py + 4, PADDLE_W * scale - 12, 2, 10 * Arcade.u, Fx.a(Arcade.S.green, 0.4));
        Fx.box(Arcade.R, px, py, PADDLE_W * scale, 2 * scale, scale, Arcade.S.mint, Arcade.S.green);
        Fx.glowDot(Arcade.R, ox + ballX * scale, oy + ballY * scale, BALL * scale, BALL * scale * 1.6, Arcade.S.title, 0.8);
        Fx.circle(Arcade.R, ox + ballX * scale, oy + ballY * scale, BALL * scale, Arcade.S.title);
        for (int i = 0; i < lives; i++) Fx.circle(Arcade.R, ox + 14 * Arcade.u + i * 16 * Arcade.u, oy + H * scale - 12 * Arcade.u, 4.5 * Arcade.u, Arcade.S.pink);
    }
}
