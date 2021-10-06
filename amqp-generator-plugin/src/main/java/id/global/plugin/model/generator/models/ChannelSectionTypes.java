package id.global.plugin.model.generator.models;

public enum ChannelSectionTypes {
    NONE("none"),
    PUBLISH("publish"),
    SUBSCRIBE("subscribe"),
    BOTH("both");

    private final String text;

    ChannelSectionTypes(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
