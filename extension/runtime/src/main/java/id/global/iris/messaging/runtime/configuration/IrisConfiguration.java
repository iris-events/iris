package id.global.iris.messaging.runtime.configuration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "iris", phase = ConfigPhase.RUN_TIME)
public final class IrisConfiguration {

    /**
     * Connection retry initial backoff interval
     */
    @ConfigItem(defaultValue = "1000")
    long backoffIntervalMillis;

    /**
     * Connection retry backoff multiplier
     */
    @ConfigItem(defaultValue = "1.5")
    double backoffMultiplier;

    /**
     * Connection max retries
     */
    @ConfigItem(defaultValue = "10")
    int maxRetries;

    /**
     * Number of messages to batch for delivery confirmation
     * <p>
     * Set to 1 for immediate confirmation of each message.
     * Set to 0 for no confirmations.
     */
    @ConfigItem(defaultValue = "1")
    long confirmationBatchSize;

    /**
     * Number of retries for Iris messages
     */
    @ConfigItem(defaultValue = "3")
    int retryMaxCount;

    public long getBackoffIntervalMillis() {
        return backoffIntervalMillis;
    }

    public void setBackoffIntervalMillis(long backoffIntervalMillis) {
        this.backoffIntervalMillis = backoffIntervalMillis;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getConfirmationBatchSize() {
        return confirmationBatchSize;
    }

    public void setConfirmationBatchSize(long confirmationBatchSize) {
        this.confirmationBatchSize = confirmationBatchSize;
    }

    public int getRetryMaxCount() {
        return retryMaxCount;
    }

    public void setRetryMaxCount(int retryMaxCount) {
        this.retryMaxCount = retryMaxCount;
    }

    @Override
    public String toString() {
        return "IrisConfiguration{" +
                "confirmationBatchSize=" + confirmationBatchSize +
                ", retryMaxCount=" + retryMaxCount +
                '}';
    }
}
