package org.iris_events.runtime.connection;

class ConnectionFactoryProviderTest {

    /*
     * @Test
     * void shouldSetAllProperties() {
     * final IrisConfig irisConfiguration = new IrisConfig();
     *
     * final var username = "username.mock";
     * final var password = "password.mock";
     * final var url = "localhost.mock";
     * final var port = 1234;
     * final var sslEnabled = true;
     *
     * irisConfiguration.setUsername(username);
     * irisConfiguration.setPassword(password);
     * irisConfiguration.setHost(url);
     * irisConfiguration.setProtocol(null);
     * irisConfiguration.setPort(port);
     * irisConfiguration.setSsl(sslEnabled);
     *
     * final var factoryProvider = new ConnectionFactoryProvider(irisConfiguration);
     * final var connectionFactory = factoryProvider.getConnectionFactory();
     *
     * assertThat(connectionFactory, notNullValue());
     * assertThat(connectionFactory.getUsername(), is(username));
     * assertThat(connectionFactory.getPassword(), is(password));
     * assertThat(connectionFactory.getHost(), is(url));
     * assertThat(connectionFactory.getPort(), is(port));
     * assertThat(connectionFactory.isSSL(), is(sslEnabled));
     * }
     *
     * @Test
     * void shouldUrlEncodeUsernameAndPassword() {
     * final var username = "u$ern@me.m//ck";
     * final var password = "p@$$word.m//ck";
     * final var port = 5432;
     *
     * final IrisConfig irisConfiguration = new IrisConfig()
     * .setUsername(username)
     * .setPassword(password)
     * .setPort(port)
     * .setProtocol(null)
     * .setSsl(false); // port is required here as mocked class does not provide default values
     *
     * final var factoryProvider = new ConnectionFactoryProvider(irisConfiguration);
     * final var connectionFactory = factoryProvider.getConnectionFactory();
     *
     * assertThat(connectionFactory.getUsername(), is(username));
     * assertThat(connectionFactory.getPassword(), is(password));
     * }
     */
}
