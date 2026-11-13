package aicoder.cruise;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AiCoderCruiseAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing AiCoder elytra cruise");

        // Lives in Meteor's own Movement category. The "AiCoder" category belongs to the columns addon;
        // registering one with the same name here would show two columns when both are installed.
        Modules.get().add(new ElytraCruise());
    }

    @Override
    public String getPackage() {
        return "aicoder.cruise";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("xiaojiaxinxiao3-beep", "AiCoder-");
    }
}
