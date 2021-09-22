package io.smallrye.asyncapi.runtime.scanner.model;

import id.global.asyncapi.spec.enums.ExchangeType;

public class ChannelBindingsInfo {
    private String exchange;
    private String queue;
    private ExchangeType exchangeType;
    private boolean exchangeDurable;
    private boolean exchangeAutoDelete;
    private String exchangeVhost;
    private boolean queueDurable;
    private boolean queueExclusive;
    private boolean queueAutoDelete;
    private String queueVhost;

    public ChannelBindingsInfo(String exchange, String queue, ExchangeType exchangeType) {

        this(exchange, queue, exchangeType, true, false, "/", true, false, false, "/");
    }

    public ChannelBindingsInfo(String exchange, String queue, ExchangeType exchangeType, boolean exchangeDurable,
            boolean exchangeAutoDelete,
            String exchangeVhost, boolean queueDurable, boolean queueExclusive, boolean queueAutoDelete, String queueVhost) {
        this.exchange = exchange;
        this.queue = queue;
        this.exchangeType = exchangeType;
        this.exchangeDurable = exchangeDurable;
        this.exchangeAutoDelete = exchangeAutoDelete;
        this.exchangeVhost = exchangeVhost;
        this.queueDurable = queueDurable;
        this.queueExclusive = queueExclusive;
        this.queueAutoDelete = queueAutoDelete;
        this.queueVhost = queueVhost;
    }

    public String getExchange() {
        return exchange;
    }

    public String getQueue() {
        return queue;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public boolean isExchangeDurable() {
        return exchangeDurable;
    }

    public boolean isExchangeAutoDelete() {
        return exchangeAutoDelete;
    }

    public boolean isQueueExclusive() {
        return queueExclusive;
    }

    public String getExchangeVhost() {
        return exchangeVhost;
    }

    public boolean isQueueDurable() {
        return queueDurable;
    }

    public boolean isQueueAutoDelete() {
        return queueAutoDelete;
    }

    public String getQueueVhost() {
        return queueVhost;
    }

}
