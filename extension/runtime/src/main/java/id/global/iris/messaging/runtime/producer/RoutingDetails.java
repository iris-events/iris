package id.global.iris.messaging.runtime.producer;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.Scope;

public final class RoutingDetails {
    String eventName;
    String exchange;
    ExchangeType exchangeType;
    String routingKey;
    Scope scope;
    String userId;
    String sessionId;
    String subscriptionId;
    boolean persistent;
    Integer cacheTtl;

    public RoutingDetails(final String eventName, final String exchange, final ExchangeType exchangeType,
            final String routingKey, final Scope scope,
            final String userId, final String sessionId, final String subscriptionId, final boolean persistent,
            final Integer cacheTtl) {
        this.eventName = eventName;
        this.exchange = exchange;
        this.exchangeType = exchangeType;
        this.routingKey = routingKey;
        this.scope = scope;
        this.userId = userId;
        this.sessionId = sessionId;
        this.subscriptionId = subscriptionId;
        this.persistent = persistent;
        this.cacheTtl = cacheTtl;
    }

    public String eventName() {
        return eventName;
    }

    public String exchange() {
        return exchange;
    }

    public ExchangeType exchangeType() {
        return exchangeType;
    }

    public String routingKey() {
        return routingKey;
    }

    public Scope scope() {
        return scope;
    }

    public String userId() {
        return userId;
    }

    public String sessionId() {
        return sessionId;
    }

    public String subscriptionId() {
        return subscriptionId;
    }

    public boolean persistent() {
        return persistent;
    }

    public Integer cacheTtl() {
        return cacheTtl;
    }

    public static class Builder {
        public ExchangeBuilder eventName(final String eventName) {
            return new ExchangeBuilder(eventName);
        }

    }

    public static class ExchangeBuilder {

        private final String eventName;

        protected ExchangeBuilder(final String eventName) {
            this.eventName = eventName;
        }

        public ExchangeTypeBuilder exchange(final String exchange) {
            return new ExchangeTypeBuilder(eventName, exchange);
        }

    }

    public static class ExchangeTypeBuilder {

        private final String eventName;
        private final String exchange;

        public ExchangeTypeBuilder(final String eventName, final String exchange) {
            this.eventName = eventName;
            this.exchange = exchange;
        }

        public RoutingKeyBuilder exchangeType(final ExchangeType exchangeType) {
            return new RoutingKeyBuilder(eventName, exchange, exchangeType);
        }

    }

    public static class RoutingKeyBuilder {

        private final String eventName;
        private final String exchange;
        private final ExchangeType exchangeType;

        public RoutingKeyBuilder(final String eventName, final String exchange, final ExchangeType exchangeType) {
            this.eventName = eventName;
            this.exchange = exchange;
            this.exchangeType = exchangeType;
        }

        public ScopeBuilder routingKey(final String routingKey) {
            return new ScopeBuilder(eventName, exchange, exchangeType, routingKey);
        }
    }

    public static class ScopeBuilder {

        private final String eventName;
        private final String exchange;
        private final ExchangeType exchangeType;
        private final String routingKey;

        public ScopeBuilder(final String eventName, final String exchange, final ExchangeType exchangeType,
                final String routingKey) {

            this.eventName = eventName;
            this.exchange = exchange;
            this.exchangeType = exchangeType;
            this.routingKey = routingKey;
        }

        public MiscRoutingDetailsBuilder scope(final Scope scope) {
            return new MiscRoutingDetailsBuilder(eventName, exchange, exchangeType, routingKey, scope);
        }
    }

    public static class MiscRoutingDetailsBuilder {

        private final String eventName;
        private final String exchange;
        private final ExchangeType exchangeType;
        private final String routingKey;
        private final Scope scope;
        private String userId;
        private String sessionId;
        private String subscriptionId;
        private boolean persistent;
        private Integer cacheTtl;

        public MiscRoutingDetailsBuilder(final String eventName, final String exchange, final ExchangeType exchangeType,
                final String routingKey, final Scope scope) {

            this.eventName = eventName;
            this.exchange = exchange;
            this.exchangeType = exchangeType;
            this.routingKey = routingKey;
            this.scope = scope;
        }

        public MiscRoutingDetailsBuilder userId(final String userId) {
            this.userId = userId;
            return this;
        }

        public MiscRoutingDetailsBuilder sessionId(final String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public MiscRoutingDetailsBuilder subscriptionId(final String subscriptionId) {
            this.subscriptionId = subscriptionId;
            return this;
        }

        public MiscRoutingDetailsBuilder persistent(final boolean persistent) {
            this.persistent = persistent;
            return this;
        }

        public MiscRoutingDetailsBuilder cacheTtl(final Integer cacheTtl) {
            this.cacheTtl = cacheTtl;
            return this;
        }

        public RoutingDetails build() {
            return new RoutingDetails(eventName, exchange, exchangeType, routingKey, scope, userId, sessionId, subscriptionId,
                    persistent, cacheTtl);
        }
    }
}
