package aicoder.assist.api;

import java.util.function.BooleanSupplier;

/** One adjustable value of a module. The builders mirror Meteor's, so module code reads the same in both clients. */
public abstract class Setting<T> {
    public final String name;
    public final String title;
    public final String description;
    public final T defaultValue;
    private final BooleanSupplier visible;
    private T value;

    protected Setting(Builder<?, T> b) {
        this.name = b.name;
        this.title = Module.titleOf(b.name);
        this.description = b.description;
        this.defaultValue = b.defaultValue;
        this.visible = b.visible;
        this.value = b.defaultValue;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = clamp(value);
    }

    public void reset() {
        value = defaultValue;
    }

    public boolean isVisible() {
        return visible == null || visible.getAsBoolean();
    }

    protected T clamp(T value) {
        return value;
    }

    /** Text form for the config file. */
    public String save() {
        return String.valueOf(value);
    }

    public abstract void load(String text);

    @SuppressWarnings("unchecked")
    public abstract static class Builder<B extends Builder<B, T>, T> {
        String name = "";
        String description = "";
        T defaultValue;
        BooleanSupplier visible;

        public B name(String name) {
            this.name = name;
            return (B) this;
        }

        public B description(String description) {
            this.description = description;
            return (B) this;
        }

        public B defaultValue(T value) {
            this.defaultValue = value;
            return (B) this;
        }

        public B visible(BooleanSupplier visible) {
            this.visible = visible;
            return (B) this;
        }

        public abstract Setting<T> build();
    }
}
