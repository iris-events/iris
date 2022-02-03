package id.global.common.headers.amqp;

public class MessagingHeaders {
    public static class Message {
        public static final String ORIGIN_SERVICE_ID = "originServiceId";
        public static final String CURRENT_SERVICE_ID = "currentServiceId";
        public static final String INSTANCE_ID = "instanceId";
        public static final String PROXY_IP_ADDRESS = "proxyIpAddress";
        public static final String USER_AGENT = "userAgent";
        public static final String IP_ADDRESS = "ipAddress";
        public static final String DEVICE = "device";
        public static final String CLIENT_TRACE_ID = "clientTraceId";
        public static final String EVENT_TYPE = "eventType";
        public static final String SESSION_ID = "sessionId";
        public static final String USER_ID = "userId";
        public static final String ROUTER = "router";
        public static final String JWT = "jwt";
        public static final String ANONYMOUS_ID = "anonId";
        public static final String REQUEST_VIA = "X-Request-Via";
        public static final String REQUEST_REFERER = "X-Request-Referer";
        public static final String REQUEST_URI = "X-Request-URI";
    }

    public static class RequeueMessage {
        public static final String X_ORIGINAL_EXCHANGE = "x-original-exchange";
        public static final String X_ORIGINAL_ROUTING_KEY = "x-original-routing-key";
        public static final String X_RETRY_COUNT = "x-retry-count";
        public static final String X_MAX_RETRIES = "x-max-retries";
        public static final String X_NOTIFY_CLIENT = "x-notify-client";
        public static final String X_ERROR_CODE = "x-error-code";
    }

    public static class QueueDeclaration {
        public static final String X_MESSAGE_TTL = "x-message-ttl";
        public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
        public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    }
}
