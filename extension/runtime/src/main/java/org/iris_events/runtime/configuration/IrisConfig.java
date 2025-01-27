package org.iris_events.runtime.configuration;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class IrisConfig {

    /**
     * Connection retry initial backoff interval
     */
    @ConfigItem(name = "backoff-interval-millis", defaultValue = "1000")
    long backoffIntervalMillis;

    /**
     * Connection retry backoff multiplier
     */
    @ConfigItem(name = "backoff-multiplier", defaultValue = "1.5")
    double backoffMultiplier;

    /**
     * Connection retry max backoff multiplier
     */
    @ConfigItem(name = "backoff-max-interval-millis", defaultValue = "30000")
    long backoffMaxIntervalMillis;

    /**
     * Connection max retries
     */
    @ConfigItem(name = "max-retries", defaultValue = "10")
    int maxRetries;

    /**
     * Number of messages to batch for delivery confirmation
     * <p>
     * Set to 1 for immediate confirmation of each message.
     * Set to 0 for no confirmations.
     */
    @ConfigItem(name = "confirmation-batch-size", defaultValue = "1")
    long confirmationBatchSize;

    /**
     * Number of retries for Iris messages
     */
    @ConfigItem(name = "retry-max-count", defaultValue = "3")
    int retryMaxCount;

    /**
     * Iris RPC request timeout
     */
    @ConfigItem(name = "rpc.timeout", defaultValue = "2000")
    int rpcTimeout;

    /**
     * RabbitMQ broker host
     */
    @ConfigItem(name = "rabbitmq-host", defaultValue = "${rabbitmq-host:localhost}")
    String host;

    /**
     * RabbitMQ protocol (amqp/amqps)
     */
    @ConfigItem(name = "rabbitmq-protocol", defaultValue = "${rabbitmq-protocol}")
    Optional<String> protocol;

    /**
     * RabbitMQ port
     */
    @ConfigItem(name = "rabbitmq-port", defaultValue = "${rabbitmq-port}")
    Optional<Integer> port;

    /**
     * Use ssl for RabbitMQ broker connection
     */
    @ConfigItem(name = "rabbitmq-ssl", defaultValue = "${rabbitmq-ssl}")
    Optional<Boolean> ssl;

    /**
     * RabbitMQ broker username
     */
    @ConfigItem(name = "rabbitmq-username", defaultValue = "${rabbitmq-username:guest}")
    String username;

    /**
     * RabbitMQ broker password
     */
    @ConfigItem(name = "rabbitmq-password", defaultValue = "${rabbitmq-password:guest}")
    String password;

    /**
     * RabbitMQ broker virtual host
     */
    @ConfigItem(name = "rabbitmq-virtual-host", defaultValue = "${rabbitmq-virtual-host:/}")
    String virtualHost;

    public long getBackoffIntervalMillis() {
        return backoffIntervalMillis;
    }

    public IrisConfig setBackoffIntervalMillis(final long backoffIntervalMillis) {
        this.backoffIntervalMillis = backoffIntervalMillis;
        return this;
    }

    public long getBackoffMaxIntervalMillis() {
        return backoffMaxIntervalMillis;
    }

    public IrisConfig setBackoffMaxIntervalMillis(final long backoffMaxIntervalMillis) {
        this.backoffMaxIntervalMillis = backoffMaxIntervalMillis;
        return this;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public IrisConfig setBackoffMultiplier(final double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
        return this;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public IrisConfig setMaxRetries(final int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public long getConfirmationBatchSize() {
        return confirmationBatchSize;
    }

    public IrisConfig setConfirmationBatchSize(final long confirmationBatchSize) {
        this.confirmationBatchSize = confirmationBatchSize;
        return this;
    }

    public int getRetryMaxCount() {
        return retryMaxCount;
    }

    public IrisConfig setRetryMaxCount(final int retryMaxCount) {
        this.retryMaxCount = retryMaxCount;
        return this;
    }

    public String getHost() {
        return host;
    }

    public IrisConfig setHost(final String host) {
        this.host = host;
        return this;
    }

    public Optional<String> getProtocol() {
        return protocol;
    }

    public IrisConfig setProtocol(final String protocol) {
        this.protocol = Optional.ofNullable(protocol);
        return this;
    }

    public int getPort() {
        return protocol.map(protocol -> switch (protocol) {
            case "amqp" -> port.orElse(5672);
            case "amqps" -> port.orElse(5671);
            default -> throw new IllegalStateException("Unknown protocol value: " + protocol);
        }).orElseGet(() -> port.orElse(5672));
    }

    public IrisConfig setPort(final int port) {
        this.port = Optional.of(port);
        return this;
    }

    public boolean isSsl() {
        return protocol.map(protocol -> switch (protocol) {
            case "amqp" -> ssl.orElse(false);
            case "amqps" -> ssl.orElse(true);
            default -> throw new IllegalStateException("Unknown protocol value: " + protocol);
        }).orElseGet(() -> ssl.orElse(false));
    }

    public IrisConfig setSsl(final boolean ssl) {
        this.ssl = Optional.of(ssl);
        return this;
    }

    public String getUsername() {
        return username;
    }

    public IrisConfig setUsername(final String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public IrisConfig setPassword(final String password) {
        this.password = password;
        return this;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public IrisConfig setVirtualHost(final String virtualHost) {
        this.virtualHost = virtualHost;
        return this;
    }

    public int getRpcTimeout() {
        return rpcTimeout;
    }

    public void setRpcTimeout(final int rpcTimeout) {
        this.rpcTimeout = rpcTimeout;
    }
}
