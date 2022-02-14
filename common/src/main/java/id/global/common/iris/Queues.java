package id.global.common.iris;

public class Queues {
    public static final String FRONTEND_SUFFIX = "frontend";
    public static final String DEAD_LETTER_PREFIX = "dead.";
    public static final String DEAD_LETTER = DEAD_LETTER_PREFIX + "dead-letter";
    public static final String ERROR = "error";
    public static final String RETRY = "retry";
    public static final String RETRY_WAIT_TTL_PREFIX = "retry.wait-";
    public static final String RETRY_WAIT_ENDED = "retry.wait-ended";
}
