package org.iris_events.runtime.channel;

public class ChannelKey {

    public static final String CHANNEL_FORMAT = "%s_%s";

    public static String create(String exchange, String routingKey) {
        return String.format(CHANNEL_FORMAT, exchange, routingKey);
    }
}
