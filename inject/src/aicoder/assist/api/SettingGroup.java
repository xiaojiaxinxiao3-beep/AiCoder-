package aicoder.assist.api;

import java.util.ArrayList;
import java.util.List;

public class SettingGroup {
    public final String name;
    public final List<Setting<?>> settings = new ArrayList<>();

    SettingGroup(String name) {
        this.name = name;
    }

    public <T> Setting<T> add(Setting<T> setting) {
        settings.add(setting);
        return setting;
    }
}
