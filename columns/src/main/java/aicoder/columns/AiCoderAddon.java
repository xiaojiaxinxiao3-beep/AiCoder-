package aicoder.columns;

import aicoder.columns.gui.AiCoderGuiTheme;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AiCoderAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    /** The addon's own column. It has no modules yet; the theme shows it anyway, in cyan. */
    public static final Category CATEGORY = new Category("AiCoder");

    @Override
    public void onInitialize() {
        LOG.info("Initializing AiCoder columns");

        // Same name as the stock theme, so this replaces it (Meteor logs an error about the duplicate name; that is expected)
        GuiThemes.add(new AiCoderGuiTheme());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "aicoder.columns";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("xiaojiaxinxiao3-beep", "AiCoder-");
    }
}
