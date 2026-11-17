package aicoder.firstcolumns;

import aicoder.firstcolumns.gui.AiCoderGuiTheme;
import aicoder.firstcolumns.gui.render.Sdf;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiThemes;
import org.slf4j.Logger;

public class AiCoderAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing AiCoder client");

        // Must happen before the first resource load, that is when Meteor compiles its pipelines
        Sdf.register();

        // The theme keeps the name "Meteor", so this replaces the stock theme. Meteor logs one
        // "has already been added" error when that happens, which is expected.
        GuiThemes.add(new AiCoderGuiTheme());
    }

    @Override
    public String getPackage() {
        return "aicoder.firstcolumns";
    }
}
