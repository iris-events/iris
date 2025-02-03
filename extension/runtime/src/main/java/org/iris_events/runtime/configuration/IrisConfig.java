package org.iris_events.runtime.configuration;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "iris")
@ConfigRoot(phase = ConfigPhase.RUN_TIME, prefix = "quarkus")
public interface IrisConfig {

    /**
     * Connection retry initial backoff interval
     */
    @WithDefault("1000")
    long backoffIntervalMillis();

    /**
     * Connection retry backoff multiplier
     */

    @WithDefault("1.5")
    double backoffMultiplier();

    /**
     * Connection retry max backoff multiplier
     */
    @WithName("backoff-max-interval-millis")
    @WithDefault("30000")
    long backoffMaxIntervalMillis();

    /**
     * Connection max retries
     */
    @WithDefault("10")
    int maxRetries();

    /**
     * Number of messages to batch for delivery confirmation
     * <p>
     * Set to 1 for immediate confirmation of each message.
     * Set to 0 for no confirmations.
     */
    @WithDefault("1")
    long confirmationBatchSize();

    /**
     * Number of retries for Iris messages
     */
    @WithName("retry-max-count")
    @WithDefault("3")
    int retryMaxCount();

    /**
     * Iris RPC request timeout
     */
    //@ConfigItem(name = "rpc.timeout", defaultValue = "2000")
    @WithDefault("2000")
    int rpcTimeout();

    /**
     * RabbitMQ broker host
     */
    @WithName("rabbitmq-host")
    @WithDefault("${rabbitmq-host:localhost}")
    String host();

    /**
     * RabbitMQ protocol (amqp/amqps)
     */

    @WithName("rabbitmq-protocol")
    @WithDefault("${rabbitmq-protocol}")
    Optional<String> protocol();

    /**
     * RabbitMQ port
     */

    @WithName("rabbitmq-port")
    @WithDefault("${rabbitmq-port}")
    Optional<Integer> port();

    /**
     * Use ssl for RabbitMQ broker connection
     */

    @WithName("rabbitmq-ssl")
    @WithDefault("${rabbitmq-ssl}")
    Optional<Boolean> ssl();

    /**
     * RabbitMQ broker username
     */
    @WithName("rabbitmq-username")
    @WithDefault("${rabbitmq-username:guest}")
    String username();

    /**
     * RabbitMQ broker password
     */
    @WithName("rabbitmq-password")
    @WithDefault("${rabbitmq-password:guest}")
    String password();

    /**
     * RabbitMQ broker virtual host
     */
    @WithName("rabbitmq-virtual-host")
    @WithDefault("${rabbitmq-virtual-host:/}")
    String virtualHost();

    default int getPort() {
        return protocol().map(protocol -> switch (protocol) {
            case "amqp" -> port().orElse(5672);
            case "amqps" -> port().orElse(5671);
            default -> throw new IllegalStateException("Unknown protocol value: " + protocol);
        }).orElseGet(() -> port().orElse(5672));
    }

    default boolean isSsl() {
        return protocol().map(protocol -> switch (protocol) {
            case "amqp" -> ssl().orElse(false);
            case "amqps" -> ssl().orElse(true);
            default -> throw new IllegalStateException("Unknown protocol value: " + protocol);
        }).orElseGet(() -> ssl().orElse(false));
    }

}
