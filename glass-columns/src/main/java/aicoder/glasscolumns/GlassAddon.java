package aicoder.glasscolumns;

import aicoder.glasscolumns.gui.GlassGuiTheme;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.utils.PostInit;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

public class GlassAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing AiCoder Liquid Glass Columns");
        GuiThemes.add(new GlassGuiTheme());
    }

    /** Meteor starts on its own theme; the first time this addon runs, switch to the glass theme instead. */
    @PostInit(dependencies = GuiThemes.class)
    public static void selectOnFirstRun() {
        File marker = new File(MeteorClient.FOLDER, "aicoder-glass-columns.installed");
        if (marker.exists()) return;

        GuiThemes.select(GlassGuiTheme.NAME);

        try {
            marker.getParentFile().mkdirs();
            marker.createNewFile();
        } catch (IOException e) {
            LOG.warn("Could not write {}", marker, e);
        }
    }

    @Override
    public String getPackage() {
        return "aicoder.glasscolumns";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("xiaojiaxinxiao3-beep", "AiCoder-");
    }
}
