package id.global.event.messaging.runtime;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.jboss.logging.Logger;

import com.rabbitmq.client.ConnectionFactory;

import id.global.event.messaging.runtime.configuration.AmqpConfiguration;

public class Common {
    private static final Logger LOG = Logger.getLogger(Common.class);

    public static String getHostName() {
        String hostName;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            hostName = System.getenv("COMPUTERNAME");
        } else {
            hostName = System.getenv("HOSTNAME");
        }
        if (hostName == null) {
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LOG.error("Can't get hostname!", e);
            }
        }
        return hostName;
    }

    public static ConnectionFactory getConnectionFactory(AmqpConfiguration amqpConfiguration) {
        ConnectionFactory factory = new ConnectionFactory();

        int port = amqpConfiguration.isSslEnabled() ? 5671 : 5672;
        if (amqpConfiguration.getPort() != 0) {
            port = amqpConfiguration.getPort();
        }
        String protocol = amqpConfiguration.isSslEnabled() ? "amqps" : "amqp";

        LOG.info(String.format("AMQP configuration: protocol=%s, url=%s, port=%s, username=%s, ssl=%s", protocol,
                amqpConfiguration.getUrl(), port, amqpConfiguration.getUsername(), amqpConfiguration.isSslEnabled()));

        try {
            if (amqpConfiguration.isAuthenticated()) {
                String connectionUrl = String.format("%s://%s:%s@%s:%s%s",
                        protocol,
                        amqpConfiguration.getUsername(),
                        amqpConfiguration.getPassword(),
                        amqpConfiguration.getUrl(),
                        port,
                        "/%2f");

                LOG.info("Setting factory URL = " + connectionUrl);
                factory.setUri(connectionUrl);

            } else {
                String connectionUrl = String.format("%s://%s:%s%s",
                        protocol,
                        amqpConfiguration.getUrl(),
                        port,
                        "/%2f");

                LOG.info("Setting factory URL = " + connectionUrl);
                factory.setUri(connectionUrl);
            }
            factory.setAutomaticRecoveryEnabled(true);

            return factory;
        } catch (URISyntaxException | NoSuchAlgorithmException | KeyManagementException e) {
            LOG.error("Cannot create ConnectionFactory!", e);
            return null;
        }

    }
}
