package id.global.event.messaging.runtime.context;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.Scope;
import id.global.common.auth.jwt.Role;
import id.global.common.iris.Queues;

public final class AmqpContext {

    private static final String DEAD_PREFIX = "dead.";
    private static final String DEAD_LETTER = "dead-letter";
    private static final ExchangeType DEFAULT_EXCHANGE_TYPE = ExchangeType.FANOUT;

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
    private Set<Role> handlerRolesAllowed;

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
            String deadLetterQueue,
            Set<Role> handlerRolesAllowed) {
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
        this.handlerRolesAllowed = handlerRolesAllowed;
    }

    public ExchangeType exchangeType() {
        return Optional.ofNullable(exchangeType).orElse(DEFAULT_EXCHANGE_TYPE);
    }

    public boolean isFrontendMessage() {
        return scope == Scope.FRONTEND;
    }

    public String buildQueueName(final String applicationName, final String instanceName) {

        StringBuilder stringBuffer = new StringBuilder()
                .append(applicationName)
                .append(".")
                .append(name);

        if (consumerOnEveryInstance && Objects.nonNull(instanceName) && !instanceName.isBlank()) {
            stringBuffer.append(".").append(instanceName);
        }

        if (exchangeType == ExchangeType.DIRECT || exchangeType == ExchangeType.TOPIC) {
            final var bindingKeys = String.join("-", getBindingKeys());
            stringBuffer.append(".").append(bindingKeys);
        }

        return stringBuffer.toString();
    }

    public Optional<String> getDeadLetterQueueName() {
        final var deadLetterQueue = getDeadLetterQueue();
        if (deadLetterQueue.isBlank()) {
            return Optional.empty();
        }

        if (isFrontendMessage() && isDefaultDeadLetterQueue()) {
            return Optional.of(Queues.DEAD_LETTER_FRONTEND);
        }

        return Optional.of(DEAD_PREFIX + deadLetterQueue);
    }

    public boolean isDefaultDeadLetterQueue() {
        return deadLetterQueue.equals(DEAD_LETTER);
    }

    public Optional<String> getDeadLetterExchangeName() {
        return getDeadLetterQueueName();
    }

    public String getDeadLetterRoutingKey(final String queueName) {
        return DEAD_PREFIX + queueName;
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
        this.deadLetterQueue = deadLetterQueue.trim();
    }

    public Set<Role> getHandlerRolesAllowed() {
        return handlerRolesAllowed;
    }

    public void setHandlerRolesAllowed(final Set<Role> handlerRolesAllowed) {
        this.handlerRolesAllowed = handlerRolesAllowed;
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
                Objects.equals(this.deadLetterQueue, that.deadLetterQueue) &&
                Objects.equals(this.handlerRolesAllowed, that.handlerRolesAllowed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, bindingKeys, exchangeType, scope, durable, autoDelete, consumerOnEveryInstance, prefetch, ttl,
                deadLetterQueue, handlerRolesAllowed);
    }

    @Override
    public String toString() {
        final var handlerRolesJoiner = new StringJoiner(", ", "[", "]");
        if (handlerRolesAllowed != null) {
            handlerRolesAllowed.forEach(role -> handlerRolesJoiner.add(role.value()));
        }
        return "AmqpContext[" +
                "name=" + name + ", " +
                "bindingKeys=" + bindingKeys + ", " +
                "exchangeType=" + exchangeType + ", " +
                "scope=" + scope + ", " +
                "durable=" + durable + ", " +
                "autoDelete=" + autoDelete + ", " +
                "consumerOnEveryInstance=" + consumerOnEveryInstance + ", " +
                "prefetch=" + prefetch + ", " +
                "ttl=" + ttl + ", " +
                "deadLetterQueue=" + deadLetterQueue + ", " +
                "handlerRolesAllowed=" + handlerRolesJoiner + ']';
    }

}
