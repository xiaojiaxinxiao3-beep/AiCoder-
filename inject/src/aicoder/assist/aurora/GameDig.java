package aicoder.assist.aurora;

import aicoder.assist.Client;
import aicoder.assist.gfx.Color;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A small side-view sandbox: a generated strip of land with caves and ore, a figure that walks and
 * jumps, and tiles you can dig out and put back. The world is kept in one file in the game folder.
 */
final class GameDig extends Arcade.Game {
    private static final int W = 360, H = 128;
    private static final byte AIR = 0, MOSS = 1, SOIL = 2, STONE = 3, SAND = 4, TRUNK = 5, LEAF = 6, GLOW = 7, CRYSTAL = 8, EMBER = 9, LAMP = 10, CORE = 11;
    private static final String[] NAMES = {"", "Moss", "Soil", "Stone", "Sand", "Trunk", "Leaf", "Glow ore", "Crystal", "Ember", "Lamp", "Core"};
    /** Seconds to dig each tile. */
    private static final double[] HARD = {0, 0.22, 0.22, 0.6, 0.18, 0.45, 0.08, 0.9, 1.0, 0.9, 0.2, 1e9};
    /** What the nine hotbar slots hold. */
    private static final byte[] SLOTS = {SOIL, STONE, SAND, TRUNK, LEAF, GLOW, CRYSTAL, EMBER, LAMP};
    private static final double PW = 0.62, PH = 1.7, REACH = 5.5;

    private final byte[] tiles = new byte[W * H];
    private final int[] surface = new int[W];
    private final int[] have = new int[12];
    private double px, py, vx, vy;
    private boolean grounded, loaded, facingLeft;
    private int slot, digX = -1, digY = -1;
    private double digProgress, walk, newWorldArmed, saveTimer;
    private long seed;

    GameDig() {
        super("dig", "Dig", "A D walk    W or Space jump    Hold left click dig    Right click place    1-9 or wheel pick    N twice: new world");
    }

    @Override boolean ranked() { return false; }
    @Override String status() { return ""; }
    @Override void reset() { }

    // ---- world ----

    private byte get(int x, int y) {
        return x < 0 || x >= W || y >= H ? CORE : y < 0 ? AIR : tiles[y * W + x];
    }

    private void set(int x, int y, byte t) {
        if (x >= 0 && x < W && y >= 0 && y < H) tiles[y * W + x] = t;
    }

    private static boolean solid(byte t) {
        return t != AIR && t != LEAF && t != LAMP && t != TRUNK;
    }

    private double noise(double x, int salt) {
        int i = (int) Math.floor(x);
        double f = x - i, s = f * f * (3 - 2 * f);
        return hash(i, salt) * (1 - s) + hash(i + 1, salt) * s;
    }

    private double noise2(double x, double y, int salt) {
        int i = (int) Math.floor(x), j = (int) Math.floor(y);
        double fx = x - i, fy = y - j, sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
        double a = hash(i + j * 7919, salt), b = hash(i + 1 + j * 7919, salt), c = hash(i + (j + 1) * 7919, salt), d = hash(i + 1 + (j + 1) * 7919, salt);
        return (a * (1 - sx) + b * sx) * (1 - sy) + (c * (1 - sx) + d * sx) * sy;
    }

    private double hash(int i, int salt) {
        long h = i * 374761393L + salt * 668265263L + seed * 2246822519L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return ((h ^ (h >>> 16)) & 0xFFFFFF) / (double) 0x1000000;
    }

    private void generate() {
        seed = new Random().nextLong() & 0xFFFFFF;
        java.util.Arrays.fill(tiles, AIR);
        java.util.Arrays.fill(have, 0);
        have[LAMP] = 16;
        for (int x = 0; x < W; x++) {
            int top = (int) Math.round(38 + noise(x / 28.0, 1) * 16 + noise(x / 9.0, 2) * 5);
            surface[x] = top;
            boolean sandy = noise(x / 22.0, 3) > 0.68;
            for (int y = top; y < H; y++) {
                int depth = y - top;
                byte t = depth == 0 ? (sandy ? SAND : MOSS) : depth < 4 + (int) (noise(x / 5.0, 4) * 3) ? (sandy ? SAND : SOIL) : STONE;
                if (depth > 6) {
                    double cave = noise2(x / 11.0, y / 8.0, 5) * 0.65 + noise2(x / 5.0, y / 4.0, 6) * 0.35;
                    if (cave > 0.60) t = AIR;
                    else if (t == STONE) {
                        double r = hash(x * 131 + y * 7, 7), deep = (double) depth / (H - top);
                        if (r < 0.012 + deep * 0.012) t = GLOW;
                        else if (r < 0.018 + deep * 0.03 && deep > 0.3) t = CRYSTAL;
                        else if (r < 0.022 + deep * 0.045 && deep > 0.55) t = EMBER;
                    }
                }
                if (y >= H - 2) t = CORE;
                tiles[y * W + x] = t;
            }
        }
        // Trees on mossy ground
        for (int x = 4; x < W - 4; x++) {
            if (hash(x, 8) > 0.9 && get(x, surface[x]) == MOSS && get(x - 1, surface[x - 1] - 1) != TRUNK) {
                int height = 4 + (int) (hash(x, 9) * 4), top = surface[x] - height;
                for (int y = surface[x] - 1; y >= top; y--) set(x, y, TRUNK);
                for (int j = -3; j <= 1; j++) for (int i = -3; i <= 3; i++) {
                    if (i * i + j * j * 1.6 <= 9.5 && get(x + i, top + j) == AIR) set(x + i, top + j, LEAF);
                }
                x += 3;
            }
        }
        px = W / 2 + 0.5; py = surface[W / 2] - 0.01; vx = vy = 0;
        while (blocked(px, py)) py -= 1;
    }

    private File file() {
        return new File(Client.mc.gameDirectory, "aicoder-assist-dig.dat");
    }

    private void save() {
        if (!loaded) return;
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file())))) {
            out.writeInt(1); out.writeInt(W); out.writeInt(H); out.writeLong(seed);
            out.write(tiles);
            for (int s : surface) out.writeInt(s);
            for (int n : have) out.writeInt(n);
            out.writeDouble(px); out.writeDouble(py); out.writeInt(slot);
        } catch (Exception e) {
            System.out.println("[" + Client.NAME + "] cannot save the Dig world: " + e);
        }
    }

    private void load() {
        loaded = true;
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(file())))) {
            if (in.readInt() != 1 || in.readInt() != W || in.readInt() != H) throw new IllegalStateException("other format");
            seed = in.readLong();
            in.readFully(tiles);
            for (int i = 0; i < W; i++) surface[i] = in.readInt();
            for (int i = 0; i < have.length; i++) have[i] = in.readInt();
            px = in.readDouble(); py = in.readDouble(); slot = Math.floorMod(in.readInt(), 9);
        } catch (Exception e) {
            generate();
        }
    }

    @Override void leave() {
        save();
    }

    // ---- play ----

    private boolean blocked(double x, double y) {
        int x0 = (int) Math.floor(x - PW / 2), x1 = (int) Math.floor(x + PW / 2 - 1e-6);
        int y0 = (int) Math.floor(y - PH), y1 = (int) Math.floor(y - 1e-6);
        for (int j = y0; j <= y1; j++) for (int i = x0; i <= x1; i++) if (solid(get(i, j))) return true;
        return false;
    }

    @Override void key(int key) {
        if (key >= 49 && key <= 57) slot = key - 49;
        if (key == 78) {
            if (newWorldArmed > 0) { generate(); newWorldArmed = 0; save(); } else newWorldArmed = 2.5;
        }
    }

    @Override void scroll(double amount) {
        slot = Math.floorMod(slot - (int) Math.signum(amount), 9);
    }

    @Override void click(int button) {
        if (button != 1) return;
        int[] t = target();
        byte kind = SLOTS[slot];
        if (t == null || get(t[0], t[1]) != AIR || have[kind] <= 0) return;
        boolean neighbour = get(t[0] - 1, t[1]) != AIR || get(t[0] + 1, t[1]) != AIR || get(t[0], t[1] - 1) != AIR || get(t[0], t[1] + 1) != AIR;
        if (!neighbour) return;
        set(t[0], t[1], kind);
        if (solid(kind) && blocked(px, py)) { set(t[0], t[1], AIR); return; }
        have[kind]--;
    }

    private double tile, camX, camY;

    /** The tile under the pointer, if it is within reach. */
    private int[] target() {
        if (tile <= 0 || !Arcade.inArea()) return null;
        double wx = camX + (Arcade.mx - Arcade.ax) / tile, wy = camY + (Arcade.my - Arcade.ay) / tile;
        if (Math.hypot(wx - px, wy - (py - PH / 2)) > REACH) return null;
        return new int[]{(int) Math.floor(wx), (int) Math.floor(wy)};
    }

    @Override void update(double dt) {
        if (!loaded) load();
        if (newWorldArmed > 0) newWorldArmed -= dt;
        saveTimer += dt;
        if (saveTimer > 30) { saveTimer = 0; save(); }

        boolean left = Arcade.held.contains(65) || Arcade.held.contains(263), right = Arcade.held.contains(68) || Arcade.held.contains(262);
        boolean jump = Arcade.held.contains(87) || Arcade.held.contains(32) || Arcade.held.contains(265);
        double want = (right ? 1 : 0) - (left ? 1 : 0);
        vx += (want * 6.2 - vx) * Math.min(1, dt * (grounded ? 14 : 6));
        if (want != 0) { facingLeft = want < 0; walk += dt * 9; }
        if (jump && grounded) { vy = -11.8; grounded = false; }
        vy = Math.min(30, vy + 34 * dt);

        // One axis at a time, in steps smaller than a tile
        int steps = (int) Math.ceil(Math.max(Math.abs(vx), Math.abs(vy)) * dt / 0.4) + 1;
        grounded = false;
        for (int s = 0; s < steps; s++) {
            double nx = px + vx * dt / steps;
            if (!blocked(nx, py)) px = nx;
            else if (grounded || vy >= 0) {
                // Walk up a single step
                if (!blocked(nx, py - 1.02) && blocked(px, py + 0.05)) { px = nx; py -= 1.02; } else vx = 0;
            } else vx = 0;
            double ny = py + vy * dt / steps;
            if (!blocked(px, ny)) py = ny;
            else {
                if (vy > 0) { grounded = true; py = Math.floor(ny + 1e-6) - 1e-4; if (blocked(px, py)) py = Math.floor(py) - 1e-4; }
                vy = 0;
            }
        }
        if (!grounded && blocked(px, py + 0.06)) grounded = true;
        px = Math.max(PW, Math.min(W - PW, px));

        // Digging: hold the button on one tile until it gives
        int[] t = Arcade.leftDown ? target() : null;
        if (t == null || get(t[0], t[1]) == AIR || get(t[0], t[1]) == CORE) { digX = -1; digProgress = 0; }
        else {
            if (t[0] != digX || t[1] != digY) { digX = t[0]; digY = t[1]; digProgress = 0; }
            digProgress += dt / HARD[get(digX, digY)];
            if (digProgress >= 1) {
                byte got = get(digX, digY);
                have[got == MOSS ? SOIL : got]++;
                set(digX, digY, AIR);
                digX = -1; digProgress = 0;
            }
        }
    }

    // ---- drawing ----

    private Color colorOf(byte t, int x, int y) {
        Style s = Arcade.S;
        double v = hash(x * 31 + y * 17, 20) * 0.12;
        return switch (t) {
            case MOSS -> Fx.mix(new Color(40, 120, 96, 255), s.green, 0.35 + v);
            case SOIL -> shade(96, 74, 86, v);
            case STONE -> shade(62, 70, 96, v);
            case SAND -> shade(196, 178, 150, v);
            case TRUNK -> shade(120, 86, 78, v);
            case LEAF -> Fx.a(Fx.mix(new Color(30, 110, 100, 255), s.teal, 0.4 + v), 0.82);
            case GLOW -> shade(52, 62, 90, v);
            case CRYSTAL -> shade(56, 54, 96, v);
            case EMBER -> shade(70, 52, 78, v);
            case LAMP -> Fx.mix(s.mint, new Color(255, 255, 255, 255), 0.5);
            default -> new Color(20, 22, 34, 255);
        };
    }

    private static Color shade(int r, int g, int b, double v) {
        double k = 0.9 + v;
        return new Color((int) (r * k), (int) (g * k), (int) (b * k), 255);
    }

    private void drawTile(byte t, int x, int y, double sx, double sy, double size, double light) {
        Style s = Arcade.S;
        Color c = colorOf(t, x, y);
        Arcade.R.quad(sx, sy, size + 0.5, size + 0.5, new Color((int) (c.r * light), (int) (c.g * light), (int) (c.b * light), c.a));
        if (t == GLOW || t == CRYSTAL || t == EMBER) {
            Color gem = t == GLOW ? s.teal : t == CRYSTAL ? s.violet : s.pink;
            double pulse = 0.7 + 0.3 * Math.sin(System.nanoTime() / 4e8 + x * 1.7 + y);
            for (int k = 0; k < 3; k++) {
                double gx = sx + size * (0.2 + hash(x * 3 + k, y + 30) * 0.55), gy = sy + size * (0.2 + hash(x + k * 5, y * 3 + 31) * 0.55), d = size * 0.16;
                Arcade.R.triangle(gx, gy - d, gx + d, gy, gx, gy + d, Fx.a(gem, pulse));
                Arcade.R.triangle(gx, gy - d, gx - d, gy, gx, gy + d, Fx.a(Fx.mix(gem, new Color(255, 255, 255, 255), 0.4), pulse));
            }
        } else if (t == LAMP) {
            Fx.glowDot(Arcade.R, sx + size / 2, sy + size / 2, size * 0.3, size * 1.8, s.mint, 0.55);
        }
        if (t != LEAF && t != LAMP && t != TRUNK && !solid(get(x, y - 1))) Arcade.R.quad(sx, sy, size + 0.5, Math.max(1, size * 0.12), Fx.rgba(255, 255, 255, 0.16 * light));
    }

    @Override void draw() {
        if (!loaded) load();
        Style s = Arcade.S;
        tile = Math.max(8, Math.floor(Arcade.ah / 24));
        double cols = Arcade.aw / tile, rows = Arcade.ah / tile;
        camX = Math.max(0, Math.min(W - cols, px - cols / 2));
        camY = Math.max(0, Math.min(H - rows, py - PH / 2 - rows / 2));

        // Sky
        Fx.box(Arcade.R, Arcade.ax, Arcade.ay, Arcade.aw, Arcade.ah, 10 * Arcade.u, Fx.mix(new Color(8, 14, 40, 255), s.violet, 0.16), Fx.mix(new Color(10, 26, 50, 255), s.teal, 0.2));
        for (int i = 0; i < 40; i++) {
            double sxp = Arcade.ax + hash(i, 40) * Arcade.aw, syp = Arcade.ay + hash(i, 41) * Arcade.ah * 0.6 - camY * tile * 0.05;
            if (syp > Arcade.ay) Arcade.R.quad(sxp, syp, 1.6 * Arcade.u, 1.6 * Arcade.u, Fx.rgba(255, 255, 255, 0.25 + 0.4 * hash(i, 42)));
        }

        int x0 = (int) Math.floor(camX), x1 = (int) Math.ceil(camX + cols), y0 = (int) Math.floor(camY), y1 = (int) Math.ceil(camY + rows);
        for (int x = x0; x <= x1 && x < W; x++) {
            if (x < 0) continue;
            for (int y = Math.max(0, y0); y <= y1 && y < H; y++) {
                byte t = tiles[y * W + x];
                double sx = Arcade.ax + (x - camX) * tile, sy = Arcade.ay + (y - camY) * tile;
                int depth = y - surface[x];
                double light = depth <= 0 ? 1 : Math.max(0.42, 1 - depth * 0.018);
                if (t == AIR || t == LEAF || t == LAMP || t == TRUNK) {
                    // Underground air shows the back wall instead of the sky
                    if (depth > 0) Arcade.R.quad(sx, sy, tile + 0.5, tile + 0.5, new Color((int) (26 * light), (int) (28 * light), (int) (44 * light), 255));
                    if (t == AIR) continue;
                }
                drawTile(t, x, y, sx, sy, tile, light);
            }
        }

        // The figure: body, head, a visor that shows which way it faces, two legs that swing
        double fx = Arcade.ax + (px - camX) * tile, fy = Arcade.ay + (py - camY) * tile, bw = PW * tile, bh = PH * tile;
        double swing = Math.abs(vx) > 0.4 && grounded ? Math.sin(walk) * bw * 0.22 : 0;
        Arcade.R.quad(fx - bw * 0.32 + swing, fy - bh * 0.3, bw * 0.26, bh * 0.3, Fx.mix(s.violet, new Color(20, 20, 50, 255), 0.4));
        Arcade.R.quad(fx + bw * 0.06 - swing, fy - bh * 0.3, bw * 0.26, bh * 0.3, Fx.mix(s.violet, new Color(20, 20, 50, 255), 0.4));
        Fx.box(Arcade.R, fx - bw / 2, fy - bh * 0.72, bw, bh * 0.46, bw * 0.2, s.teal, Fx.mix(s.teal, s.violet, 0.6));
        Fx.box(Arcade.R, fx - bw * 0.44, fy - bh, bw * 0.88, bh * 0.3, bw * 0.24, s.title, Fx.mix(s.title, s.teal, 0.3));
        Arcade.R.quad(fx + (facingLeft ? -bw * 0.4 : bw * 0.02), fy - bh * 0.9, bw * 0.38, bh * 0.08, new Color(10, 20, 40, 255));

        // Target outline and digging progress
        int[] t = target();
        if (t != null) {
            double sx = Arcade.ax + (t[0] - camX) * tile, sy = Arcade.ay + (t[1] - camY) * tile;
            Fx.boxOutline(Arcade.R, sx, sy, tile, tile, 2, Math.max(1, Arcade.u * 1.4), Fx.a(s.title, 0.7));
            if (t[0] == digX && t[1] == digY && digProgress > 0) Arcade.R.quad(sx, sy + tile * (1 - digProgress), tile, tile * digProgress, Fx.rgba(255, 255, 255, 0.3));
        }

        // Hotbar
        double slotSize = 46 * Arcade.u, total = slotSize * 9 + 8 * 6 * Arcade.u, hx = Arcade.ax + (Arcade.aw - total) / 2, hy = Arcade.ay + Arcade.ah - slotSize - 12 * Arcade.u;
        Fx.box(Arcade.R, hx - 10 * Arcade.u, hy - 8 * Arcade.u, total + 20 * Arcade.u, slotSize + 16 * Arcade.u, 16 * Arcade.u, Fx.rgba(4, 10, 26, 0.72));
        for (int i = 0; i < 9; i++) {
            double x = hx + i * (slotSize + 6 * Arcade.u);
            byte kind = SLOTS[i];
            Fx.box(Arcade.R, x, hy, slotSize, slotSize, 10 * Arcade.u, Fx.rgba(255, 255, 255, i == slot ? 0.16 : 0.05));
            if (i == slot) Fx.boxOutline(Arcade.R, x, hy, slotSize, slotSize, 10 * Arcade.u, Math.max(1, 1.6 * Arcade.u), s.green);
            double in = slotSize * 0.24;
            drawTile(kind, i * 3, 200, x + in, hy + in * 0.7, slotSize - in * 2, have[kind] > 0 ? 1 : 0.35);
            Arcade.text.put(String.valueOf(have[kind]), x + 5 * Arcade.u, hy + slotSize - 15 * Arcade.u, 10.5, have[kind] > 0 ? s.title : s.faint, 0, Fx.BOLD);
        }
        String info = NAMES[SLOTS[slot]] + "    depth " + Math.max(0, (int) (py - surface[(int) Math.max(0, Math.min(W - 1, px))]));
        if (newWorldArmed > 0) info = "Press N again to replace this world with a new one";
        Arcade.text.put(info, Arcade.ax + 14 * Arcade.u, Arcade.ay + 12 * Arcade.u, 12.5, newWorldArmed > 0 ? s.pink : s.dim, 0.8, 0);
    }
}
