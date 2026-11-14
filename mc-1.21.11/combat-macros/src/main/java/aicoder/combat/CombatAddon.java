package aicoder.combat;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class CombatAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing AiCoder Combat Macros");

        Modules modules = Modules.get();
        modules.add(new AnchorMacro());
        modules.add(new CrystalMacro());
        modules.add(new CrosshairDrift());
        modules.add(new WindPearl());
        modules.add(new RightClicker());
        modules.add(new LeftClicker());
    }

    @Override
    public String getPackage() {
        return "aicoder.combat";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("xiaojiaxinxiao3-beep", "AiCoder-");
    }
}
