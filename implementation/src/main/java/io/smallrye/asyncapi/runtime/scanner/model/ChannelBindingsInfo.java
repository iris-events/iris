package io.smallrye.asyncapi.runtime.scanner.model;

import id.global.common.annotations.amqp.ExchangeType;

public class ChannelBindingsInfo {
    private final String exchange;
    private final String queue;
    private final ExchangeType exchangeType;
    private final boolean exchangeDurable;
    private final boolean exchangeAutoDelete;
    private final String exchangeVhost;
    private final boolean queueDurable;
    private final boolean queueExclusive;
    private final boolean queueAutoDelete;
    private final String queueVhost;

    public ChannelBindingsInfo(String exchange, String queue, ExchangeType exchangeType, boolean durable, boolean autodelete) {
        this(exchange, queue, exchangeType, true, false, "/", durable, false, autodelete, "/");
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
