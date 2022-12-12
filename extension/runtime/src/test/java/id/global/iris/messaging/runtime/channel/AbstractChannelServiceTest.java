package id.global.iris.messaging.runtime.channel;

import id.global.iris.messaging.runtime.channel.mock.MockConnection;
import id.global.iris.messaging.runtime.configuration.IrisRabbitMQConfig;
import id.global.iris.messaging.runtime.connection.ConsumerConnectionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class AbstractChannelServiceTest {

    private ChannelService channelService;
    private IrisRabbitMQConfig irisRabbitMQConfigMock;
    private ConsumerConnectionProvider connectionProviderMock;
    private AtomicInteger closeCount;

    @BeforeEach
    public void setup() {
        this.irisRabbitMQConfigMock = Mockito.mock(IrisRabbitMQConfig.class);
        this.connectionProviderMock = Mockito.mock(ConsumerConnectionProvider.class);

        this.channelService = new TestChannelService(connectionProviderMock, irisRabbitMQConfigMock);
        this.closeCount = new AtomicInteger();
    }

    @Test
    void getOrCreateChannelById() throws IOException {
        final var dummyConnection = new MockConnection();
        Mockito.when(connectionProviderMock.getConnection()).thenReturn(dummyConnection);

        final var channelId = "test-channel-1";
        // First invocation creates and adds channel to map
        final var channel = channelService.getOrCreateChannelById(channelId);
        Mockito.verify(connectionProviderMock, Mockito.times(1)).getConnection();
        // Second invocation returns already saved channel
        final var sameChannel = channelService.getOrCreateChannelById(channelId);
        Mockito.verify(connectionProviderMock, Mockito.times(1)).getConnection();

        assertThat(channel, is(notNullValue()));
        assertThat(sameChannel, is(notNullValue()));
        assertThat(channelService.getChannelMap().size(), is(1));
    }

    @Test
    void removeChannel() throws IOException {
        final var dummyConnection = new MockConnection(closeCount);
        Mockito.when(connectionProviderMock.getConnection()).thenReturn(dummyConnection);

        final var channelId = "test-channel-1";
        channelService.getOrCreateChannelById(channelId);

        assertThat(channelService.getChannelMap().size(), is(1));
        channelService.removeChannel(channelId);
        assertThat(channelService.getChannelMap().size(), is(0));
        assertThat(closeCount.get(), is(1));
    }

    @Test
    void removeNonExistingChannel() throws IOException {
        final var channelId = "test-channel-1";

        assertThat(channelService.getChannelMap().size(), is(0));
        channelService.removeChannel(channelId);
        assertThat(channelService.getChannelMap().size(), is(0));
    }

    @Test
    void createChannel() throws IOException {
        final var dummyConnection = new MockConnection();
        Mockito.when(connectionProviderMock.getConnection()).thenReturn(dummyConnection);

        final var channel = channelService.createChannel();
        Mockito.verify(connectionProviderMock, Mockito.times(1)).getConnection();
        assertThat(channel, is(notNullValue()));
    }

    @Test
    void closeAndRemoveAllChannels() throws IOException {
        final var dummyConnection = new MockConnection(this.closeCount);
        Mockito.when(connectionProviderMock.getConnection()).thenReturn(dummyConnection);

        channelService.getOrCreateChannelById("1");
        channelService.getOrCreateChannelById("2");

        assertThat(channelService.getChannelMap().size(), is(2));

        channelService.closeAndRemoveAllChannels();

        // TODO check if channel.close() was actually called
        assertThat(channelService.getChannelMap().size(), is(0));
        assertThat(closeCount.get(), is(2));
    }

}