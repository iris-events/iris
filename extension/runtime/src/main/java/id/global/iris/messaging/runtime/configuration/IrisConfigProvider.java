package id.global.iris.messaging.runtime.configuration;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.DefaultBean;
import io.smallrye.reactive.messaging.rabbitmq.RabbitMQConnector;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;

@ApplicationScoped
public class IrisConfigProvider {
    private static final Logger log = LoggerFactory.getLogger(IrisConfigProvider.class);

    @Inject
    @Connector(value = "smallrye-rabbitmq")
    RabbitMQConnector connector;

    @Produces
    public RabbitMQClient createClient(RabbitMQOptions options) {
        return RabbitMQClient.create(connector.getVertx().getDelegate(), options);
    }

    @Produces
    @DefaultBean
    @ApplicationScoped
    @Named("IrisRabbitMQOptions")
    public RabbitMQOptions createOptions(Config config) {
        var host = config.getOptionalValue("rabbitmq-host", String.class).orElse(RabbitMQOptions.DEFAULT_HOST);
        final var optionalProtocol = config.getOptionalValue("rabbit-protocol", String.class);
        var port = getPort(config, optionalProtocol);
        var ssl = getSsl(config, optionalProtocol);
        var username = config.getOptionalValue("rabbitmq-username", String.class).orElse(RabbitMQOptions.DEFAULT_USER);
        var password = config.getOptionalValue("rabbitmq-password", String.class).orElse(RabbitMQOptions.DEFAULT_PASSWORD);
        var virtualHost = config.getOptionalValue("rabbitmq-virtual-host", String.class)
                .orElse(RabbitMQOptions.DEFAULT_VIRTUAL_HOST);

        var options = new RabbitMQOptions()
                .setHost(host)
                .setPort(port)
                .setUser(username)
                .setPassword(password)
                .setSsl(ssl)
                .setVirtualHost(virtualHost);
        log.info("RabbitMQ options: host: {}, port: {}, ssl: {}", options.getHost(), options.getPort(), options.isSsl());

        return options;

    }

    private int getPort(final Config config, final Optional<String> optionalProtocol) {
        return optionalProtocol.map(protocol -> switch (protocol) {
            case "amqp" -> 5672;
            case "amqps" -> 5671;
            default -> throw new IllegalStateException("Unknown protocol value: " + protocol);
        })
                .orElseGet(() -> config.getOptionalValue("rabbitmq-port", Integer.class).orElse(5672));
    }

    private boolean getSsl(final Config config, final Optional<String> optionalProtocol) {
        return optionalProtocol.map(protocol -> switch (protocol) {
            case "amqp" -> false;
            case "amqps" -> true;
            default -> throw new IllegalStateException("Unknown protocol value: " + protocol);
        })
                .orElseGet(() -> config.getOptionalValue("rabbitmq-ssl", Boolean.class).orElse(false));
    }
}
