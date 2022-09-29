package id.global.iris.messaging.runtime;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.constants.Queues;
import id.global.iris.messaging.runtime.context.IrisContext;

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
}
