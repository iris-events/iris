package id.global.plugin.model.generator.models;

import com.fasterxml.jackson.databind.JsonNode;

public class ChannelDetails {

    String channelName;
    String subscribeEventName;
    String publishEventName;
    ChannelSectionTypes containedSections;
    JsonNode node;

    public ChannelDetails(String channelName, String subscribeEventName, String publishEventName,
            ChannelSectionTypes containedSections, JsonNode node) {
        this.channelName = channelName;
        this.subscribeEventName = subscribeEventName;
        this.publishEventName = publishEventName;
        this.containedSections = containedSections;
        this.node = node;
    }

    public ChannelDetails() {
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String subscribeEventName() {
        return subscribeEventName;
    }

    public void setSubscribeEventName(String subscribeEventName) {
        this.subscribeEventName = subscribeEventName;
    }

    public String publishEventName() {
        return publishEventName;
    }

    public void setPublishEventName(String publishEventName) {
        this.publishEventName = publishEventName;
    }

    public JsonNode node() {
        return node;
    }

    public void setNode(JsonNode node) {
        this.node = node;
    }

    public ChannelSectionTypes getContainedSections() {
        return containedSections;
    }

    public void setContainedSections(ChannelSectionTypes containedSections) {
        this.containedSections = containedSections;
    }
}
