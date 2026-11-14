package aicoder.assist.api;

public class BoolSetting extends Setting<Boolean> {
    private BoolSetting(Builder b) {
        super(b);
    }

    @Override
    public void load(String text) {
        set(Boolean.parseBoolean(text));
    }

    public static class Builder extends Setting.Builder<Builder, Boolean> {
        public Builder() {
            defaultValue = false;
        }

        @Override
        public Setting<Boolean> build() {
            return new BoolSetting(this);
        }
    }
}
