package id.global.iris.asyncapi.runtime.scanner.model;

import id.global.iris.common.annotations.ExchangeType;

public class ChannelBindingsInfo {
    private final String exchange;
    private final String queue;
    private final ExchangeType exchangeType;
    private final boolean exchangeDurable;
    private final boolean exchangeAutoDelete;
    private final String exchangeVhost;
    private final Boolean queueDurable;
    private final boolean queueExclusive;
    private final Boolean queueAutoDelete;
    private final String queueVhost;

    public ChannelBindingsInfo(String exchange, String queue, ExchangeType exchangeType) {
        this(exchange, queue, exchangeType, true, false, "/", null, false, null, "/");
    }

    public ChannelBindingsInfo(String exchange, String queue, ExchangeType exchangeType, boolean durable, boolean autodelete) {
        this(exchange, queue, exchangeType, true, false, "/", durable, false, autodelete, "/");
    }

    public ChannelBindingsInfo(String exchange, String queue, ExchangeType exchangeType, boolean exchangeDurable,
            boolean exchangeAutoDelete,
            String exchangeVhost, Boolean queueDurable, boolean queueExclusive, Boolean queueAutoDelete, String queueVhost) {
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

    public Boolean isQueueDurable() {
        return queueDurable;
    }

    public Boolean isQueueAutoDelete() {
        return queueAutoDelete;
    }

    public String getQueueVhost() {
        return queueVhost;
    }

}
