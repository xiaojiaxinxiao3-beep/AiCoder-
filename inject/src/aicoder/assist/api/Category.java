package aicoder.assist.api;

import java.util.List;

public record Category(String name) {
    public static final Category EXPLOSIVES = new Category("Explosives");
    public static final Category MELEE = new Category("Melee");
    public static final Category UTILITY = new Category("Utility");
    public static final List<Category> ALL = List.of(EXPLOSIVES, MELEE, UTILITY);
}
