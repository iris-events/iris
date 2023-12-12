package org.iris_events.asyncapi.runtime.scanner.model;

import java.util.Map;

import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26BindingImpl;

public class GidAai20AmqpChannelBindings extends AsyncApi26BindingImpl {

    // is = queue or is = routingKey
    private String is;
    // Use exchange.name, exchange.type, exchange.durable, exchange.autoDelete, exchange.vhost
    private Map<String, Object> exchange;
    // Use queue.name, queue.durable, queue.exclusive, queue.autoDelete, queue.vhost
    private Map<String, Object> queue;

    private String bindingVersion;

    public GidAai20AmqpChannelBindings() {
    }

    public String getIs() {
        return is;
    }

    public void setIs(String is) {
        this.is = is;
    }

    public Map<String, Object> getExchange() {
        return exchange;
    }

    public void setExchange(Map<String, Object> exchange) {
        this.exchange = exchange;
    }

    public Map<String, Object> getQueue() {
        return queue;
    }

    public void setQueue(Map<String, Object> queue) {
        this.queue = queue;
    }

    public String getBindingVersion() {
        return bindingVersion;
    }

    public void setBindingVersion(String bindingVersion) {
        this.bindingVersion = bindingVersion;
    }
}
