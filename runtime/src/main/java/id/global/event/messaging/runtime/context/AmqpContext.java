package id.global.event.messaging.runtime.context;

import java.util.List;
import java.util.Objects;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.Scope;

public final class AmqpContext {
    private String name;
    private List<String> bindingKeys;
    private ExchangeType exchangeType;
    private Scope scope;
    private boolean durable;
    private boolean autoDelete;
    private boolean consumerOnEveryInstance;
    private int prefetch;
    private long ttl;
    private String deadLetterQueue;

    public AmqpContext() {
    }

    public AmqpContext(String name,
            List<String> bindingKeys,
            ExchangeType exchangeType,
            Scope scope,
            boolean durable,
            boolean autoDelete,
            boolean consumerOnEveryInstance,
            int prefetch,
            long ttl,
            String deadLetterQueue) {
        this.name = name;
        this.bindingKeys = bindingKeys;
        this.exchangeType = exchangeType;
        this.scope = scope;
        this.durable = durable;
        this.autoDelete = autoDelete;
        this.consumerOnEveryInstance = consumerOnEveryInstance;
        this.prefetch = prefetch;
        this.ttl = ttl;
        this.deadLetterQueue = deadLetterQueue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getBindingKeys() {
        return bindingKeys;
    }

    public void setBindingKeys(List<String> bindingKeys) {
        this.bindingKeys = bindingKeys;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(ExchangeType exchangeType) {
        this.exchangeType = exchangeType;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public boolean isAutoDelete() {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
    }

    public boolean isConsumerOnEveryInstance() {
        return consumerOnEveryInstance;
    }

    public void setConsumerOnEveryInstance(boolean consumerOnEveryInstance) {
        this.consumerOnEveryInstance = consumerOnEveryInstance;
    }

    public int getPrefetch() {
        return prefetch;
    }

    public void setPrefetch(int prefetch) {
        this.prefetch = prefetch;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public String getDeadLetterQueue() {
        return deadLetterQueue;
    }

    public void setDeadLetterQueue(String deadLetterQueue) {
        this.deadLetterQueue = deadLetterQueue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (AmqpContext) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.bindingKeys, that.bindingKeys) &&
                Objects.equals(this.exchangeType, that.exchangeType) &&
                Objects.equals(this.scope, that.scope) &&
                this.durable == that.durable &&
                this.autoDelete == that.autoDelete &&
                this.consumerOnEveryInstance == that.consumerOnEveryInstance &&
                this.prefetch == that.prefetch &&
                this.ttl == that.ttl &&
                Objects.equals(this.deadLetterQueue, that.deadLetterQueue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, bindingKeys, exchangeType, scope, durable, autoDelete, consumerOnEveryInstance, prefetch,
                ttl, deadLetterQueue);
    }

    @Override
    public String toString() {
        return "AmqpContext[" +
                "exchange=" + name + ", " +
                "bindingKeys=" + bindingKeys + ", " +
                "exchangeType=" + exchangeType + ", " +
                "scope=" + scope + ", " +
                "durable=" + durable + ", " +
                "autoDelete=" + autoDelete + ", " +
                "consumerOnEveryInstance=" + consumerOnEveryInstance + ", " +
                "prefetch=" + prefetch + ", " +
                "ttl=" + ttl + ", " +
                "deadLetterQueue=" + deadLetterQueue + "]";
    }

}
