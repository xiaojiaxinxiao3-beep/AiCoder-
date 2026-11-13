package aicoder.auroracolumns.gui.widgets;

import aicoder.auroracolumns.gui.Chrome;
import aicoder.auroracolumns.gui.Fx;
import aicoder.auroracolumns.gui.AuroraGuiTheme;
import aicoder.auroracolumns.gui.Sounds;
import aicoder.auroracolumns.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/**
 * One line in a column. Left click toggles the module, right click opens its settings. Off is plain dim
 * text; on is a row lit by the aurora with a green bead at the right; hovering lifts a clear pane under the
 * row and shows a detail card beside it.
 */
public class WAuroraModule extends WPressable implements MeteorWidget {
    private static final double HEIGHT = 34;
    private static final double FONT = 14;
    private static final double TIP_WIDTH = 300;
    private static final double TIP_DELAY = 0.25;

    private final Module module;
    private final String title;
    private final String id;
    private final double seed;

    private double active;
    private double hover;
    private double hoverTime;
    private boolean wasHovered;
    private List<String> wrapped;
    private double wrappedUnit;

    public WAuroraModule(Module module, String title) {
        this.module = module;
        this.title = title;

        int hash = module.name.hashCode();
        this.id = String.format("MOD-0x%02X", hash & 0xFF);
        this.seed = ((hash >>> 8) & 0xFF) / 255.0;
        this.active = module.isActive() ? 1 : 0;
    }

    private AuroraGuiTheme aurora() {
        return (AuroraGuiTheme) theme;
    }

    @Override
    protected void onCalculateSize() {
        double u = aurora().unit();
        width = (21 + 44) * u + Fx.textWidth(theme, title, FONT, 0, u);
        height = Math.round(HEIGHT * u);
    }

    @Override
    protected void onPressed(int button) {
        if (button == MOUSE_BUTTON_LEFT) {
            module.toggle();
            Sounds.toggle(aurora(), module.isActive());
        } else if (button == MOUSE_BUTTON_RIGHT) {
            mc.gui.setScreen(theme.moduleScreen(module));
        }
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        Style s = aurora().style;
        double u = aurora().unit();

        // A row scrolled under the column's edge still reports the mouse over it
        WView view = getView();
        boolean hovered = mouseOver && (view == null || view.mouseOver);

        active = Fx.step(active, module.isActive(), delta, 6.25);
        hover = Fx.step(hover, hovered, delta, 8.3);
        hoverTime = hovered ? hoverTime + delta : 0;

        if (hovered && !wasHovered) Sounds.hover(aurora(), seed);
        wasHovered = hovered;

        // Switched-on rows breathe between 0.7 and 1 every 2.4 seconds, each on its own beat
        double pulse = 0.85 + 0.15 * Math.sin(Math.PI * 2 * (Fx.phase(2.4) + seed));

        // The pane, inset from the column's edges with a little air between rows
        double px = x + 8 * u, pw = width - 16 * u;
        double py = y + 1.5 * u, ph = height - 3 * u;
        double corner = 11 * u;

        Chrome.lit(renderer, px, py, pw, ph, u, corner, 0.30, 0.14, 0.20, Fx.a(s.mint, 0.42), 0.18, active * pulse);
        if (hover > 0) {
            Fx.box(renderer, px, py, pw, ph, corner, Fx.rgba(255, 255, 255, 0.11 * hover));
            Fx.boxOutline(renderer, px, py, pw, ph, corner, Fx.hair(u), Fx.rgba(255, 255, 255, 0.4 * hover));
        }

        Color text = Fx.mix(s.dim, s.title, Math.max(active, hover));
        Fx.text(renderer, u).put(title, x + 21 * u, Fx.centerY(theme, y + height / 2.0, FONT, u), FONT, text, 0, 0);

        if (active > 0) Chrome.bead(renderer, s, x + width - 24.5 * u, y + height / 2.0, 3.5 * u * active, active, pulse);

        if (hoverTime >= TIP_DELAY || (!Fx.animate && hovered)) renderTip(renderer, s, u);
    }

    /** Detail card beside the row: name, description and state. Drawn after everything else so no column covers it. */
    private void renderTip(GuiRenderer renderer, Style s, double u) {
        double padX = 18 * u, padY = 16 * u;
        double w = TIP_WIDTH * u;
        double inner = w - padX * 2;

        if (wrapped == null || wrappedUnit != u) {
            wrapped = wrap(module.description, inner, u);
            wrappedUnit = u;
        }

        double nameH = Fx.lineHeight(theme, 15, u);
        double lineH = Fx.lineHeight(theme, 13, u) * 1.25;
        double metaH = Fx.lineHeight(theme, 11, u);
        double h = padY + nameH + 6 * u + wrapped.size() * lineH + 8 * u + metaH + padY;

        // To the right of the row when it fits, otherwise to the left; kept on screen
        double tx = x + width + 6 * u;
        if (tx + w > getWindowWidth()) tx = x - 6 * u - w;
        tx = Math.max(0, tx);
        double ty = Mth.clamp(y - 8 * u, 0, Math.max(0, getWindowHeight() - h));

        double fx = Math.round(tx), fy = Math.round(ty);
        boolean on = module.isActive();
        List<String> lines = wrapped;
        String name = title.toUpperCase(Locale.ROOT);

        renderer.absolutePost(() -> {
            renderer.scissorStart(0, 0, getWindowWidth(), getWindowHeight());

            Chrome.panel(renderer, fx, fy, w, h, u, 18 * u, true);

            Fx.TextBatch text = Fx.text(renderer, u);
            double cy = fy + padY;
            text.put(name, fx + padX, cy, 15, s.title, 2.4, Fx.BOLD);
            cy += nameH + 6 * u;
            for (String line : lines) {
                text.put(line, fx + padX, cy, 13, s.dim);
                cy += lineH;
            }
            text.put(id + " \u00B7 " + (on ? "ENABLED" : "DISABLED"), fx + padX, cy + 8 * u, 11, s.green, 1.4, 0);

            renderer.scissorEnd();
        });
    }

    private List<String> wrap(String text, double maxWidth, double u) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && Fx.textWidth(theme, candidate, 13, 0, u) > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }
}
