import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.consumer.AmqpConsumerContainer;
import id.global.event.messaging.runtime.context.AmqpContext;
import io.smallrye.asyncapi.runtime.scanner.model.ExchangeType;

public class AmqpConsumerContainerTest {

    @Test
    public void disableConfigShouldNotInitConsumers() {
        AmqpConfiguration config = new AmqpConfiguration();
        config.setUrl("localhost");
        config.setPort(5672);
        config.setUsername("user");
        config.setPassword("password");
        config.setAuthenticated(false);
        config.setSslEnabled(false);

        config.setConsumersDisabled(true);

        AmqpConsumerContainer consumerContainer = new AmqpConsumerContainer(config, new ObjectMapper());
        consumerContainer.initConsumer();
        AmqpContext amqpContext = new AmqpContext("dummy_queue", "dummy_exchange", null, ExchangeType.DIRECT);
        consumerContainer.addConsumer(null, null, amqpContext, null);

        assertThat(consumerContainer.getConnection(), is(nullValue()));
        assertThat(consumerContainer.getNumOfConsumers(), is(0));
    }
}
