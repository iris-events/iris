package id.global.event.messaging.runtime.context;

import id.global.common.annotations.amqp.ExchangeType;

public class AmqpContext {
    private String exchange;
    private String[] bindingKeys;
    private ExchangeType exchangeType;

    public AmqpContext() {
    }

    public AmqpContext(String exchange, String[] bindingKeys, ExchangeType exchangeType) {
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

    public void setExchange(String exchange) {
        this.exchange = exchange;
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
