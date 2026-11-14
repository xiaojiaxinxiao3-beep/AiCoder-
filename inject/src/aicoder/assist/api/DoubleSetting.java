package aicoder.assist.api;

public class DoubleSetting extends Setting<Double> {
    public final double min, max;

    private DoubleSetting(Builder b) {
        super(b);
        min = b.min;
        max = b.max;
    }

    @Override
    protected Double clamp(Double value) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void load(String text) {
        set(Double.parseDouble(text.trim()));
    }

    public static class Builder extends Setting.Builder<Builder, Double> {
        double min = 0, max = 10;

        public Builder() {
            defaultValue = 0.0;
        }

        public Builder defaultValue(int value) {
            return defaultValue((double) value);
        }

        public Builder range(double min, double max) {
            this.min = min;
            this.max = max;
            return this;
        }

        public Builder sliderRange(double min, double max) {
            return this;
        }

        @Override
        public Setting<Double> build() {
            return new DoubleSetting(this);
        }
    }
}
