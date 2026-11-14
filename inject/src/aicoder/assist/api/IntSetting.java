package aicoder.assist.api;

public class IntSetting extends Setting<Integer> {
    public final int min, max;

    private IntSetting(Builder b) {
        super(b);
        min = b.min;
        max = b.max;
    }

    @Override
    protected Integer clamp(Integer value) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void load(String text) {
        set(Integer.parseInt(text.trim()));
    }

    public static class Builder extends Setting.Builder<Builder, Integer> {
        int min = 0, max = 100;

        public Builder() {
            defaultValue = 0;
        }

        public Builder range(int min, int max) {
            this.min = min;
            this.max = max;
            return this;
        }

        /** Meteor shows a narrower slider than the allowed range; here the slider covers the whole range. */
        public Builder sliderRange(int min, int max) {
            return this;
        }

        @Override
        public Setting<Integer> build() {
            return new IntSetting(this);
        }
    }
}
