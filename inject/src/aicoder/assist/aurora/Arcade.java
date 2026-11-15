package aicoder.assist.aurora;

import aicoder.assist.Client;
import aicoder.assist.gfx.Color;
import aicoder.assist.gfx.GuiRenderer;
import aicoder.assist.gfx.GuiTheme;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * The GAMES pane of the menu: a list of small games on the left, the one being played on the right.
 * They are drawn with the same shapes and palette as the rest of the menu, so they follow the skin.
 * Minecraft keeps running behind the menu while you play.
 */
final class Arcade {
    private Arcade() {
    }

    interface Hits { void add(double x, double y, double w, double h, Runnable action); }

    /** What a game needs to draw one frame. Filled in before each call, so games keep no reference to the screen. */
    static GuiRenderer R;
    static GuiTheme theme;
    static Fx.TextBatch text;
    static Style S;
    static double u, ax, ay, aw, ah, mx, my;
    static final Set<Integer> held = new HashSet<>();
    static boolean leftDown, rightDown;

    static boolean open;
    private static final Game[] GAMES = {new GameBlocks(), new GameSnake(), new GameMines(), new GameMerge(), new GameBricks(), new GameDig()};
    private static int current = 0;
    private static long last;
    private static Properties best;

    static abstract class Game {
        final String id, title, help;
        int score;
        boolean over, paused;

        Game(String id, String title, String help) {
            this.id = id; this.title = title; this.help = help;
        }

        abstract void reset();
        abstract void update(double dt);
        abstract void draw();
        void key(int key) {}
        void click(int button) {}
        void scroll(double amount) {}
        /** Leaving the game or closing the menu. */
        void leave() {}
        boolean ranked() { return true; }
        String status() { return over ? "GAME OVER  -  R TO RESTART" : paused ? "PAUSED  -  P TO CONTINUE" : ""; }
    }

    // ---- shared drawing ----

    static Color[] palette() {
        return new Color[]{S.green, S.teal, S.violet, S.pink, S.mint, Fx.mix(S.green, S.violet, 0.5), Fx.mix(S.pink, S.teal, 0.45)};
    }

    /** One rounded cell with a lighter top, used by every grid game. */
    static void cell(double x, double y, double size, Color c, double alpha) {
        double gap = Math.max(1, size * 0.06), r = size * 0.22;
        Fx.box(R, x + gap, y + gap, size - gap * 2, size - gap * 2, r, Fx.a(Fx.mix(c, new Color(255, 255, 255, 255), 0.25), alpha), Fx.a(c, alpha));
        Fx.box(R, x + gap * 2, y + gap * 2, size - gap * 4, (size - gap * 4) * 0.35, r * 0.7, Fx.rgba(255, 255, 255, 0.28 * alpha), Fx.rgba(255, 255, 255, 0));
    }

    static void well(double x, double y, double w, double h) {
        Fx.box(R, x, y, w, h, 10 * u, Fx.rgba(3, 8, 22, 0.55));
        Fx.boxOutline(R, x, y, w, h, 10 * u, Fx.hair(u), Fx.rgba(255, 255, 255, 0.16));
    }

    static void label(String s, double cx, double y, double size, Color c, boolean bold) {
        text.put(s, cx - Fx.textWidth(theme, s, size, 0, u) / 2, y, size, c, 0, bold ? Fx.BOLD : 0);
    }

    // ---- pane ----

    static void render(GuiRenderer renderer, GuiTheme guiTheme, Style style, double unit, double width, double height, double mouseX, double mouseY, Hits hits) {
        R = renderer; theme = guiTheme; S = style; u = unit; mx = mouseX; my = mouseY;
        long now = System.nanoTime();
        double dt = last == 0 ? 0 : Math.min(0.05, (now - last) / 1e9);
        last = now;

        double w = Math.min(1240 * u, width - 68 * u), h = Math.min(720 * u, height - 130 * u);
        double x = Math.round((width - w) / 2), y = Math.round(88 * u + (height - 88 * u - 30 * u - h) / 2);
        Ui.panel(R, x, y, w, h, u, S.corner * u, false);
        text = Fx.text(R, u);

        // Game list
        double lx = x + 22 * u, lw = 200 * u, ly = y + 24 * u;
        text.put("GAMES", lx + 6 * u, ly, 12, S.green, 3.4, Fx.BOLD);
        ly += 30 * u;
        for (int i = 0; i < GAMES.length; i++) {
            Game g = GAMES[i];
            double ry = ly + i * 48 * u;
            boolean on = i == current, hover = mouseX >= lx && mouseX < lx + lw && mouseY >= ry && mouseY < ry + 42 * u;
            Ui.plate(R, lx, ry, lw, 42 * u, u, 14 * u, hover && !on ? 1 : 0);
            if (on) Ui.lit(R, lx, ry, lw, 42 * u, u, 14 * u, 0.30, 0.20, 0.26, Fx.a(S.mint, 0.42), 0.2, 1);
            text.put(g.title.toUpperCase(Locale.ROOT), lx + 16 * u, Fx.centerY(theme, ry + 21 * u, 13.5, u), 13.5, on ? S.title : S.dim, 2, on ? Fx.BOLD : 0);
            int index = i;
            hits.add(lx, ry, lw, 42 * u, () -> select(index));
        }
        R.quad(lx + lw + 14 * u, y + 24 * u, Fx.hair(u), h - 48 * u, Fx.rgba(255, 255, 255, 0.1));

        // The game
        Game game = GAMES[current];
        double gx = lx + lw + 30 * u, gw = x + w - 24 * u - gx, top = y + 22 * u;
        text.put(game.title.toUpperCase(Locale.ROOT), gx, top, 19, S.title, 4, Fx.BOLD);
        if (game.ranked()) {
            String score = "SCORE " + game.score + "    BEST " + Math.max(game.score, bestOf(game));
            text.put(score, gx + gw - Fx.textWidth(theme, score, 13, 1.6, u), top + 4 * u, 13, S.green, 1.6, Fx.BOLD);
        }
        text.put(game.help, gx, top + 30 * u, 12, S.faint, 0.6, 0);

        ax = gx; ay = top + 58 * u; aw = gw; ah = y + h - 22 * u - ay;
        if (!game.over && !game.paused) game.update(dt);
        if (game.over && game.ranked() && game.score > bestOf(game)) setBest(game);
        R.scissorStart(ax, ay, aw, ah);
        text = Fx.text(R, u);
        game.draw();
        String status = game.status();
        if (!status.isEmpty()) {
            double sw = Fx.textWidth(theme, status, 16, 2, u) + 56 * u;
            Fx.box(R, ax + (aw - sw) / 2, ay + ah / 2 - 26 * u, sw, 52 * u, 26 * u, Fx.rgba(4, 10, 26, 0.86));
            Fx.boxOutline(R, ax + (aw - sw) / 2, ay + ah / 2 - 26 * u, sw, 52 * u, 26 * u, Fx.hair(u), Fx.a(S.green, 0.7));
            R.scissorStart(ax, ay, aw, ah);
            Fx.TextBatch top2 = Fx.text(R, u);
            top2.put(status, ax + (aw - sw) / 2 + 28 * u, Fx.centerY(theme, ay + ah / 2, 16, u), 16, S.title, 2, Fx.BOLD);
            R.scissorEnd();
        }
        R.scissorEnd();
    }

    private static void select(int index) {
        if (index == current) return;
        GAMES[current].leave();
        current = index;
        held.clear();
    }

    static boolean inArea() {
        return mx >= ax && mx < ax + aw && my >= ay && my < ay + ah;
    }

    static boolean key(int key, boolean down) {
        if (!down) { held.remove(key); return true; }
        held.add(key);
        Game game = GAMES[current];
        if (key == 82 && game.ranked()) { game.over = false; game.paused = false; game.score = 0; game.reset(); return true; }
        if (key == 80) { game.paused = !game.paused; return true; }
        if (!game.over && !game.paused) game.key(key);
        return true;
    }

    static boolean mouse(int button, boolean down) {
        if (button == 0) leftDown = down;
        if (button == 1) rightDown = down;
        Game game = GAMES[current];
        if (down && inArea() && !game.over && !game.paused) game.click(button);
        return inArea();
    }

    static void scroll(double amount) {
        GAMES[current].scroll(amount);
    }

    /** The menu is closing or showing the modules again. */
    static void leave() {
        held.clear();
        leftDown = rightDown = false;
        last = 0;
        GAMES[current].leave();
        for (Game g : GAMES) {
            if (!g.ranked()) continue;
            if (g.score > bestOf(g)) setBest(g);
            if (!g.over) g.paused = g.score > 0 || g.paused;
        }
    }

    // ---- best scores ----

    private static File file() {
        return new File(Client.mc.gameDirectory, "aicoder-assist-games.properties");
    }

    private static int bestOf(Game game) {
        if (best == null) {
            best = new Properties();
            try (FileInputStream in = new FileInputStream(file())) { best.load(in); } catch (Exception ignored) { }
        }
        try { return Integer.parseInt(best.getProperty(game.id, "0")); } catch (NumberFormatException e) { return 0; }
    }

    private static void setBest(Game game) {
        best.setProperty(game.id, String.valueOf(game.score));
        try (FileOutputStream out = new FileOutputStream(file())) { best.store(out, Client.NAME + " games"); } catch (Exception ignored) { }
    }
}
