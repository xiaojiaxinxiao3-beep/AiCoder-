package aicoder.client;

import aicoder.client.gui.AiCoderGuiTheme;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiThemes;
import org.slf4j.Logger;

public class AiCoderAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing AiCoder client");

        // Same name as the stock theme, so this replaces it (Meteor logs an error about the duplicate name; that is expected)
        GuiThemes.add(new AiCoderGuiTheme());
    }

    @Override
    public String getPackage() {
        return "aicoder.client";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("xiaojiaxinxiao3-beep", "AiCoder-");
    }
}
