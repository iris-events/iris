package id.global.iris.messaging.it;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import id.global.common.iris.message.ErrorMessage;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;

public abstract class AbstractIntegrationTest extends IsolatedEventContextTest {

    @Inject
    public RabbitMQClient rabbitMQClient;

    @Inject
    public ObjectMapper objectMapper;

    public Channel channel;

    public abstract String getErrorMessageQueue();

    protected ErrorMessage getErrorResponse(long maxTimeoutSeconds) throws Exception {
        final int waitTimeoutMillis = 50;
        final var timoutMillis = maxTimeoutSeconds * 1000;
        final int maxRetries = (int) (timoutMillis / waitTimeoutMillis);

        return getErrorResponse(waitTimeoutMillis, 0, maxRetries);
    }

    private ErrorMessage getErrorResponse(int waitTimeoutMillis, final int retries, int maxRetries) throws Exception {
        final var getResponse = channel.basicGet(getErrorMessageQueue(), true);

        if (getResponse != null) {
            return objectMapper.readValue(getResponse.getBody(), ErrorMessage.class);
        }

        if (retries <= maxRetries) {
            final var currentRetry = retries + 1;

            Thread.sleep(waitTimeoutMillis);
            return getErrorResponse(waitTimeoutMillis, currentRetry, maxRetries);
        }

        return null;
    }
}
