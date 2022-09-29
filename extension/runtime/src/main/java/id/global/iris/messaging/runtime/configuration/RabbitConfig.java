package id.global.iris.messaging.runtime.configuration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.DefaultBean;
import io.smallrye.reactive.messaging.rabbitmq.RabbitMQConnector;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;

@ApplicationScoped
public class RabbitConfig {
    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

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
    public RabbitMQOptions createOptions(Config config) {
        var host = config.getOptionalValue("rabbitmq-host", String.class).orElse(RabbitMQOptions.DEFAULT_HOST);
        var port = config.getOptionalValue("rabbitmq-port", Integer.class).orElse(5672);
        var username = config.getOptionalValue("rabbitmq-username", String.class).orElse(RabbitMQOptions.DEFAULT_USER);
        var password = config.getOptionalValue("rabbitmq-password", String.class).orElse(RabbitMQOptions.DEFAULT_PASSWORD);
        var ssl = config.getOptionalValue("rabbitmq-ssl", Boolean.class).orElse(false);
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
}
