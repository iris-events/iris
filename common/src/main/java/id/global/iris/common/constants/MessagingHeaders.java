package id.global.iris.common.constants;

@SuppressWarnings("unused")
public class MessagingHeaders {
    public static class Message {
        public static final String ORIGIN_SERVICE_ID = "x-origin-service-id";
        public static final String CURRENT_SERVICE_ID = "x-current-service-id";
        public static final String INSTANCE_ID = "x-instance-id";
        public static final String PROXY_IP_ADDRESS = "x-proxy-ip-address";
        public static final String USER_AGENT = "x-user-agent";
        public static final String IP_ADDRESS = "x-ip-address";
        public static final String DEVICE = "x-device";
        public static final String CLIENT_TRACE_ID = "x-client-trace-id";
        public static final String EVENT_TYPE = "x-event-type";
        public static final String SESSION_ID = "x-session-id";
        public static final String USER_ID = "x-user-id";
        public static final String ROUTER = "x-router";
        public static final String JWT = "x-jwt";
        public static final String ANONYMOUS_ID = "x-anon-id";
        public static final String REQUEST_VIA = "x-request-via";
        public static final String REQUEST_REFERER = "x-request-referer";
        public static final String REQUEST_URI = "x-request-uri";
        public static final String SERVER_TIMESTAMP = "x-server-timestamp";
        public static final String SUBSCRIPTION_ID = "x-subscription-id";
        public static final String CORRELATION_ID = "x-correlation-id";
    }

    public static class RequeueMessage {
        public static final String X_ORIGINAL_EXCHANGE = "x-original-exchange";
        public static final String X_ORIGINAL_ROUTING_KEY = "x-original-routing-key";
        public static final String X_ORIGINAL_QUEUE = "x-original-queue";
        public static final String X_RETRY_COUNT = "x-retry-count";
        public static final String X_MAX_RETRIES = "x-max-retries";
        public static final String X_NOTIFY_CLIENT = "x-notify-client";
        public static final String X_ERROR_CODE = "x-error-code";
        public static final String X_ERROR_TYPE = "x-error-type";
        public static final String X_ERROR_MESSAGE = "x-error-message";
    }

    public static class QueueDeclaration {
        public static final String X_MESSAGE_TTL = "x-message-ttl";
        public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
        public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    }
}
