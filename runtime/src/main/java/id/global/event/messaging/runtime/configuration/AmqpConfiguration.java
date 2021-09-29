package id.global.event.messaging.runtime.configuration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class AmqpConfiguration {

    ConsumerConfiguration configuration;

    ProducerConfiguration producerConfiguration;
    /**
     * disable initialization of consumers
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

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

    public ConsumerConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ConsumerConfiguration configuration) {
        this.configuration = configuration;
    }

    public ProducerConfiguration getProducerConfiguration() {
        return producerConfiguration;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "AmqpConfiguration{" +
                "configuration=" + configuration +
                ", producerConfiguration=" + producerConfiguration +
                ", url='" + url + '\'' +
                ", port='" + port + '\'' +
                ", username='" + username + '\'' +
                ", authenticated='" + authenticated + '\'' +
                ", sslEnabled=" + sslEnabled +
                ", enabled=" + enabled +
                '}';
    }
}
