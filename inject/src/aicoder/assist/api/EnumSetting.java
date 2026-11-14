package aicoder.assist.api;

public class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private EnumSetting(Builder<E> b) {
        super(b);
    }

    public E[] values() {
        return defaultValue.getDeclaringClass().getEnumConstants();
    }

    /** The next choice, wrapping around; what a click on the setting does. */
    public void cycle(int step) {
        E[] all = values();
        set(all[Math.floorMod(get().ordinal() + step, all.length)]);
    }

    @Override
    public String save() {
        return get().name();
    }

    @Override
    public void load(String text) {
        for (E e : values()) {
            if (e.name().equals(text.trim())) set(e);
        }
    }

    public static class Builder<E extends Enum<E>> extends Setting.Builder<Builder<E>, E> {
        @Override
        public Setting<E> build() {
            return new EnumSetting<>(this);
        }
    }
}
