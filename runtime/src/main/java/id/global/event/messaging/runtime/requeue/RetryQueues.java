package id.global.event.messaging.runtime.requeue;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import id.global.event.messaging.runtime.configuration.AmqpConfiguration;

@ApplicationScoped
public class RetryQueues {
    private static final String RETRY_QUEUE_TEMPLATE = "retry.retry-queue-%d";

    private final List<RetryQueue> retryQueues;
    private final long initialInterval;
    private final double factor;

    @Inject
    public RetryQueues(AmqpConfiguration configuration) {
        this(configuration.getRetryMaxCount(), configuration.getRetryInitialInterval(), configuration.getRetryFactor());

    }

    private RetryQueues(int maxRetries, long initialInterval, double factor) {
        this.retryQueues = new ArrayList<>();
        this.factor = factor;
        this.initialInterval = initialInterval;

        for (int i = 0; i < maxRetries; i++) {
            retryQueues.add(new RetryQueue(String.format(RETRY_QUEUE_TEMPLATE, i), getTtl(i)));
        }
    }

    public int getMaxRetryCount() {
        return retryQueues.size();
    }

    public RetryQueue getNextQueue(int retryCount) {
        return retryQueues.get(retryCount);
    }

    private long getTtl(int retryCount) {
        return (long) (initialInterval + (initialInterval * retryCount * factor));
    }
}
