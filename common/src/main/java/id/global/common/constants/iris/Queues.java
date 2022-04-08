package id.global.common.constants.iris;

@SuppressWarnings("unused")
public enum Queues {
    FRONTEND_SUFFIX("frontend"),
    SUBSCRIPTION("subscription"),
    DEAD_LETTER(Constants.DEAD_LETTER),
    ERROR("error"),
    RETRY("retry"),
    RETRY_WAIT_TTL_PREFIX("retry.wait-"),
    RETRY_WAIT_ENDED("retry.wait-ended");

    private final String value;

    Queues(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static class Constants {
        public static final String DEAD_LETTER_PREFIX = "dead.";
        public static final String DEAD_LETTER = DEAD_LETTER_PREFIX + "dead-letter";
    }
}
