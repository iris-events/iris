package id.global.iris.messaging.runtime;

import java.util.Objects;

import javax.inject.Inject;

import id.global.common.iris.annotations.ExchangeType;
import id.global.iris.messaging.runtime.context.AmqpContext;

public class QueueNameProvider {

    @Inject
    InstanceInfoProvider instanceInfoProvider;

    public String getQueueName(final AmqpContext context) {
        final var applicationName = instanceInfoProvider.getApplicationName();
        final var instanceName = instanceInfoProvider.getInstanceName();
        final var name = context.getName();
        final var exchangeType = context.getExchangeType();

        StringBuilder stringBuffer = new StringBuilder()
                .append(applicationName)
                .append(".")
                .append(name);

        if (context.isConsumerOnEveryInstance() && Objects.nonNull(instanceName) && !instanceName.isBlank()) {
            stringBuffer.append(".").append(instanceName);
        }

        if (exchangeType == ExchangeType.DIRECT || exchangeType == ExchangeType.TOPIC) {
            final var bindingKeys = String.join("-", context.getBindingKeys());
            stringBuffer.append(".").append(bindingKeys);
        }

        return stringBuffer.toString();
    }
}
