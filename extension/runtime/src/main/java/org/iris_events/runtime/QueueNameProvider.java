package org.iris_events.runtime;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.common.Queues;
import org.iris_events.context.IrisContext;

@ApplicationScoped
public class QueueNameProvider {

    final String applicationName;
    final String instanceName;

    @Inject
    public QueueNameProvider(final InstanceInfoProvider instanceInfoProvider) {
        this.applicationName = instanceInfoProvider.getApplicationName();
        this.instanceName = instanceInfoProvider.getInstanceName();
    }

    public String getQueueName(final IrisContext context) {
        final var name = context.getName();
        if (context.isRpc()) {
            return getRpcRequestQueueName(name);
        }

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

    public String getFrontendQueueName() {
        return String.format("%s.%s", applicationName, Queues.FRONTEND_SUFFIX.getValue());
    }

    public String getRpcRequestQueueName(String eventName) {
        return String.format("%s-%s.rpc.req", eventName, instanceName);
    }

    public String getRpcResponseQueueName(String eventName) {
        return String.format("%s-%s.rpc.res", eventName, instanceName);
    }
}
