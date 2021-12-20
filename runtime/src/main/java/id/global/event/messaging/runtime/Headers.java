package id.global.event.messaging.runtime;

public class Headers {
    public static final String ORIGIN_SERVICE_ID = "originServiceId";
    public static final String CURRENT_SERVICE_ID = "currentServiceId";
    public static final String INSTANCE_ID = "instanceId";
    public static final String EVENT_TYPE = "eventType";
    public static final String SESSION_ID = "sessionId";
    public static final String USER_ID = "userId";
    public static final String ROUTER = "router";

    public static class QueueDeclarationHeaders {
        public static final String X_MESSAGE_TTL = "x-message-ttl";
        public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
        public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    }

    public static class RequeueHeaders {
        public static final String X_ORIGINAL_EXCHANGE = "x-original-exchange";
        public static final String X_ORIGINAL_ROUTING_KEY = "x-original-routing-key";
        public static final String X_RETRY_COUNT = "x-retry-count";
    }
}
