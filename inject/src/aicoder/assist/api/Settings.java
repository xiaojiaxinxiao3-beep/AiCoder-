package aicoder.assist.api;

import java.util.ArrayList;
import java.util.List;

public class Settings {
    public final List<SettingGroup> groups = new ArrayList<>();

    public SettingGroup getDefaultGroup() {
        return createGroup("General");
    }

    public SettingGroup createGroup(String name) {
        for (SettingGroup group : groups) {
            if (group.name.equals(name)) return group;
        }
        SettingGroup group = new SettingGroup(name);
        groups.add(group);
        return group;
    }

    public Setting<?> get(String name) {
        for (SettingGroup group : groups) {
            for (Setting<?> setting : group.settings) {
                if (setting.name.equals(name)) return setting;
            }
        }
        return null;
    }
}
