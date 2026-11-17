package aicoder.firstcolumns.gui.widgets;

import aicoder.firstcolumns.gui.AiCoderGuiTheme;
import aicoder.firstcolumns.gui.render.Palette;
import aicoder.firstcolumns.gui.render.Sdf;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Module row of a column: a rounded card with the module's name. There is no switch on it: a module that
 * is on has its whole card lit in the two theme colours, with the drifting light of the design around it.
 * Left click switches the module, right click opens its settings.
 */
public class WAiCoderModule extends WPressable implements MeteorWidget {
    private static final Color TEXT = Palette.color(Palette.TEXT);
    private static final Color TEXT_ON = Palette.color(Palette.WHITE);

    private final Module module;
    private final String title;

    private final double seed = Math.random();
    private double titleWidth;
    private double activeProgress;

    public WAiCoderModule(Module module, String title, Consumer<Module> onSelect) {
        this.module = module;
        this.title = title;
        this.tooltip = module.description;

        activeProgress = module.isActive() ? 1 : 0;
    }

    public Module module() {
        return module;
    }

    private double u(double v) {
        return AiCoderGuiTheme.of(theme).u(v);
    }

    @Override
    protected void onCalculateSize() {
        if (titleWidth == 0) titleWidth = theme.textWidth(title);

        width = u(14) + titleWidth + u(14);
        height = u(9) * 2 + theme.textHeight();
    }

    @Override
    protected void onPressed(int button) {
        if (button == MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == MOUSE_BUTTON_RIGHT) mc.gui.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double u = u(1);
        double radius = 10 * u;
        boolean active = module.isActive();

        activeProgress += delta * 7 * (active ? 1 : -1);
        activeProgress = Mth.clamp(activeProgress, 0, 1);
        double a = activeProgress;

        Shapes.row(x, y, width, height, radius, u, mouseOver && !active);

        if (a > 0) {
            // On: the card fills with the two colours and the light drifts along its edge
            Sdf.box(x, y, width, height, radius)
                .gradient(Palette.withAlpha(Palette.ACCENT_1, 0.26 * a), Palette.withAlpha(Palette.ACCENT_2, 0.20 * a), 0)
                .flowFill(0.22 * a)
                .stroke(Math.max(1, 1.2 * u), Palette.rgba(255, 255, 255, 0.75 * a)).flowStroke()
                .glow(8 * u, Palette.rgba(255, 255, 255, 0.55 * a)).flowGlow()
                .seed(seed, true)
                .draw();
        }

        renderer.text(title, x + 14 * u, Math.round(y + (height - theme.textHeight()) / 2), a > 0.5 ? TEXT_ON : TEXT, false);
    }
}
