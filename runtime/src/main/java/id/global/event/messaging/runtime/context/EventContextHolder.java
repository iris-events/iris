package id.global.event.messaging.runtime.context;

import com.rabbitmq.client.AMQP;

public class EventContextHolder {
    private AMQP.BasicProperties amqpBasicProperties;

    EventContextHolder() {
    }

    public AMQP.BasicProperties getAmqpBasicProperties() {
        return amqpBasicProperties;
    }

    public void setAmqpBasicProperties(AMQP.BasicProperties amqpBasicProperties) {
        this.amqpBasicProperties = amqpBasicProperties;
    }
}
