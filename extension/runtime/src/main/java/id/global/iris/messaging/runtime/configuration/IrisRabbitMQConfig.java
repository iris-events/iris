package id.global.iris.messaging.runtime.configuration;

import java.util.Optional;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class IrisRabbitMQConfig {

    /**
     * Connection retry initial backoff interval
     */
    @ConfigProperty(name = "iris.backoff-interval-millis", defaultValue = "1000")
    long backoffIntervalMillis;

    /**
     * Connection retry backoff multiplier
     */
    @ConfigProperty(name = "iris.backoff-multiplier", defaultValue = "1.5")
    double backoffMultiplier;

    /**
     * Connection max retries
     */
    @ConfigProperty(name = "iris.max-retries", defaultValue = "10")
    int maxRetries;

    /**
     * Number of messages to batch for delivery confirmation
     * <p>
     * Set to 1 for immediate confirmation of each message.
     * Set to 0 for no confirmations.
     */
    @ConfigProperty(name = "iris.confirmation-batch-size", defaultValue = "1")
    long confirmationBatchSize;

    /**
     * Number of retries for Iris messages
     */
    @ConfigProperty(name = "iris.retry-max-count", defaultValue = "3")
    int retryMaxCount;

    /**
     * RabbitMQ broker host
     */
    @ConfigProperty(name = "rabbitmq-host", defaultValue = "localhost")
    String host;

    /**
     * RabbitMQ protocol (amqp/amqps)
     */
    @ConfigProperty(name = "rabbitmq-protocol")
    Optional<String> protocol;

    /**
     * RabbitMQ port
     */
    @ConfigProperty(name = "rabbitmq-port")
    Optional<Integer> port;

    /**
     * Use ssl for RabbitMQ broker connection
     */
    @ConfigProperty(name = "rabbitmq-ssl")
    Optional<Boolean> ssl;

    /**
     * RabbitMQ broker username
     */
    @ConfigProperty(name = "rabbitmq-username", defaultValue = "guest")
    String username;

    /**
     * RabbitMQ broker password
     */
    @ConfigProperty(name = "rabbitmq-password", defaultValue = "guest")
    String password;

    /**
     * RabbitMQ broker virtual host
     */
    @ConfigProperty(name = "rabbitmq-virtual-host", defaultValue = "/")
    String virtualHost;

    public long getBackoffIntervalMillis() {
        return backoffIntervalMillis;
    }

    public IrisRabbitMQConfig setBackoffIntervalMillis(final long backoffIntervalMillis) {
        this.backoffIntervalMillis = backoffIntervalMillis;
        return this;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public IrisRabbitMQConfig setBackoffMultiplier(final double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
        return this;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public IrisRabbitMQConfig setMaxRetries(final int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public long getConfirmationBatchSize() {
        return confirmationBatchSize;
    }

    public IrisRabbitMQConfig setConfirmationBatchSize(final long confirmationBatchSize) {
        this.confirmationBatchSize = confirmationBatchSize;
        return this;
    }

    public int getRetryMaxCount() {
        return retryMaxCount;
    }

    public IrisRabbitMQConfig setRetryMaxCount(final int retryMaxCount) {
        this.retryMaxCount = retryMaxCount;
        return this;
    }

    public String getHost() {
        return host;
    }

    public IrisRabbitMQConfig setHost(final String host) {
        this.host = host;
        return this;
    }

    public Optional<String> getProtocol() {
        return protocol;
    }

    public IrisRabbitMQConfig setProtocol(final String protocol) {
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

    public IrisRabbitMQConfig setPort(final int port) {
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

    public IrisRabbitMQConfig setSsl(final boolean ssl) {
        this.ssl = Optional.of(ssl);
        return this;
    }

    public String getUsername() {
        return username;
    }

    public IrisRabbitMQConfig setUsername(final String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public IrisRabbitMQConfig setPassword(final String password) {
        this.password = password;
        return this;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public IrisRabbitMQConfig setVirtualHost(final String virtualHost) {
        this.virtualHost = virtualHost;
        return this;
    }
}
