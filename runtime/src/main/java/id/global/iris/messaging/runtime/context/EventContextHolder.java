package id.global.iris.messaging.runtime.context;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

public class EventContextHolder {
    private AMQP.BasicProperties amqpBasicProperties;
    private Envelope envelope;

    EventContextHolder() {
    }

    public AMQP.BasicProperties getAmqpBasicProperties() {
        return amqpBasicProperties;
    }

    public void setAmqpBasicProperties(AMQP.BasicProperties amqpBasicProperties) {
        this.amqpBasicProperties = amqpBasicProperties;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public void setEnvelope(final Envelope envelope) {
        this.envelope = envelope;
    }
}
