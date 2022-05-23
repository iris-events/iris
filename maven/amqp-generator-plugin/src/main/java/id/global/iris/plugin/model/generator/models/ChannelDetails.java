package id.global.iris.plugin.model.generator.models;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class ChannelDetails {

    String channelName;
    String subscribeEventName;
    String publishEventName;
    JsonNode node;
    private final String SUBSCRIBE = "subscribe";
    private final String PUBLISH = "publish";

    public ChannelDetails(String channelName, String subscribeEventName, String publishEventName, JsonNode node) {
        this.channelName = channelName;
        this.subscribeEventName = subscribeEventName;
        this.publishEventName = publishEventName;
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

    public Map.Entry<String, List<String>> getSectionsForChannelEvent() {
        if (subscribeEventName.isBlank() && publishEventName.isBlank()) {
            return new AbstractMap.SimpleEntry<>("", Collections.emptyList());
        }
        if (!subscribeEventName.isBlank() && !publishEventName.isBlank()) {
            return new AbstractMap.SimpleEntry<>(publishEventName, List.of(PUBLISH, SUBSCRIBE));
        } else if (!subscribeEventName.isBlank()) {
            return new AbstractMap.SimpleEntry<>(subscribeEventName, List.of(SUBSCRIBE));
        } else {
            return new AbstractMap.SimpleEntry<>(publishEventName, List.of(PUBLISH));
        }
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
}
