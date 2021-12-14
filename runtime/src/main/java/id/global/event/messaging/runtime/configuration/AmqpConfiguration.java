package id.global.event.messaging.runtime.configuration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "amqp", phase = ConfigPhase.RUN_TIME)
public final class AmqpConfiguration {
    /**
     * rabbitmq connection url
     */
    @ConfigItem(defaultValue = "rabbitmq")
    String url;

    /**
     * rabbitmq connection port
     */
    @ConfigItem(defaultValue = "5671")
    String port;

    /**
     * rabbitmq username
     */
    @ConfigItem(defaultValue = "user")
    String username;

    /**
     * rabbitmq password
     */
    @ConfigItem(defaultValue = "user")
    String password;

    /**
     * use username and password authentication
     */
    @ConfigItem(defaultValue = "true")
    String authenticated;

    /**
     * use username and password authentication
     */
    @ConfigItem(defaultValue = "true")
    boolean sslEnabled;

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
     * Number of retries for amqp messages
     */
    @ConfigItem(defaultValue = "3")
    int retryMaxCount;

    /**
     * Initial retry backoff interval in milliseconds
     */
    @ConfigItem(defaultValue = "5000")
    long retryInitialInterval;

    /**
     * Retry backoff multiplier
     */
    @ConfigItem(defaultValue = "1.5")
    double retryFactor;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getPort() {
        return Integer.parseInt(port);
    }

    public void setPort(int port) {
        this.port = String.valueOf(port);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAuthenticated() {
        return Boolean.parseBoolean(authenticated);
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = String.valueOf(authenticated);
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(String authenticated) {
        this.authenticated = authenticated;
    }

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

    public long getRetryInitialInterval() {
        return retryInitialInterval;
    }

    public void setRetryInitialInterval(long retryInitialInterval) {
        this.retryInitialInterval = retryInitialInterval;
    }

    public double getRetryFactor() {
        return retryFactor;
    }

    public void setRetryFactor(double retryFactor) {
        this.retryFactor = retryFactor;
    }

    @Override
    public String toString() {
        return "AmqpConfiguration{" +
                "url='" + url + '\'' +
                ", port='" + port + '\'' +
                ", username='" + username + '\'' +
                ", authenticated='" + authenticated + '\'' +
                ", sslEnabled=" + sslEnabled +
                ", backoffIntervalMillis=" + backoffIntervalMillis +
                ", backoffMultiplier=" + backoffMultiplier +
                ", maxRetries=" + maxRetries +
                ", confirmationBatchSize=" + confirmationBatchSize +
                ", retryMaxCount=" + retryMaxCount +
                ", retryInitialInterval=" + retryInitialInterval +
                ", retryFactor=" + retryFactor +
                '}';
    }
}
