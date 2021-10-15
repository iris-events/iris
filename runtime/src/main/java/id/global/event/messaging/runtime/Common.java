package id.global.event.messaging.runtime;

public class Common {
    public static String createChannelKey(String exchange, String routingKey) {
        return String.format("%s_%s", exchange, routingKey);
    }
}
