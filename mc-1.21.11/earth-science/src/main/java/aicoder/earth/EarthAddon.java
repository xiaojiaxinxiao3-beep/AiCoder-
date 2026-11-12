package aicoder.earth;

import aicoder.earth.gui.EarthGuiTheme;
import aicoder.earth.gui.Style;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.utils.PostInit;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EarthAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    /** The ten themes, in the order the in-window switcher steps through them. */
    public static final List<EarthGuiTheme> THEMES = new ArrayList<>();

    @Override
    public void onInitialize() {
        LOG.info("Initializing AiCoder Earth Science");

        for (Style style : Style.ALL) {
            EarthGuiTheme theme = new EarthGuiTheme(style);
            THEMES.add(theme);
            GuiThemes.add(theme);
        }
    }

    /** Meteor starts on its own theme; the first time this addon runs, switch to the first of ours instead. */
    @PostInit(dependencies = GuiThemes.class)
    public static void selectOnFirstRun() {
        File marker = new File(MeteorClient.FOLDER, "aicoder-earth.installed");
        if (marker.exists() || THEMES.isEmpty()) return;

        GuiThemes.select(THEMES.getFirst().name);

        try {
            marker.getParentFile().mkdirs();
            marker.createNewFile();
        } catch (IOException e) {
            LOG.warn("Could not write {}", marker, e);
        }
    }

    @Override
    public String getPackage() {
        return "aicoder.earth";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("xiaojiaxinxiao3-beep", "AiCoder-");
    }
}
