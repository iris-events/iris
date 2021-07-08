package id.global.event.messaging.runtime.configuration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class AmqpConfiguration {

    /**
     * rabbitmq connection url
     */
    @ConfigItem(defaultValue = "rabbitmq")
    String url;

    /**
     * rabbitmq connection port
     */
    @ConfigItem(defaultValue = "5672")
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

    ConsumerConfiguration configuration;
    ProducerConfiguration producerConfiguration;

    public ConsumerConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ConsumerConfiguration configuration) {
        this.configuration = configuration;
    }

    public ProducerConfiguration getProducerConfiguration() {
        return producerConfiguration;
    }

    public void setProducerConfiguration(ProducerConfiguration producerConfiguration) {
        this.producerConfiguration = producerConfiguration;
    }
}
