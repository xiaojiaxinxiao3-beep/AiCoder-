package aicoder.auroracolumns;

import aicoder.auroracolumns.gui.AuroraGuiTheme;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.utils.PostInit;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

public class AuroraAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing AiCoder Aurora Glass Columns");
        GuiThemes.add(new AuroraGuiTheme());
    }

    /** Meteor starts on its own theme; the first time this addon runs, switch to the aurora theme instead. */
    @PostInit(dependencies = GuiThemes.class)
    public static void selectOnFirstRun() {
        File marker = new File(MeteorClient.FOLDER, "aicoder-aurora-columns.installed");
        if (marker.exists()) return;

        GuiThemes.select(AuroraGuiTheme.NAME);

        try {
            marker.getParentFile().mkdirs();
            marker.createNewFile();
        } catch (IOException e) {
            LOG.warn("Could not write {}", marker, e);
        }
    }

    @Override
    public String getPackage() {
        return "aicoder.auroracolumns";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("xiaojiaxinxiao3-beep", "AiCoder-");
    }
}
