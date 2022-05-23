package id.global.iris.messaging.runtime.producer;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CorrelationIdProvider {
    public String getCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
