package id.global.event.messaging.runtime.context;

import id.global.event.messaging.runtime.enums.ExchangeType;

public class AmqpContext {
    private String queue;
    private String exchange;
    private String[] bindingKeys;
    private ExchangeType exchangeType;

    public AmqpContext() {
    }

    public AmqpContext(String queue, String exchange, String[] bindingKeys, ExchangeType exchangeType) {
        this.queue = queue;
        this.exchange = exchange;
        this.bindingKeys = bindingKeys;
        this.exchangeType = exchangeType;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(ExchangeType exchangeType) {
        this.exchangeType = exchangeType;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getQueue() {
        return queue;
    }

    public String getExchange() {
        return exchange;
    }

    public String[] getBindingKeys() {
        return bindingKeys;
    }

    public void setBindingKeys(String[] bindingKeys) {
        this.bindingKeys = bindingKeys;
    }
}
