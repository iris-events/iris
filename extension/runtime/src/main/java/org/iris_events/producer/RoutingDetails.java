package org.iris_events.producer;

import java.util.Objects;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.Scope;

public final class RoutingDetails {
    private final String eventName;
    private final String exchange;
    private final ExchangeType exchangeType;
    private final String routingKey;
    private final Scope scope;
    private final String userId;
    private final String sessionId;
    private final String subscriptionId;
    private final boolean persistent;
    private final Integer cacheTtl;
    private final boolean propagate;

    public RoutingDetails(final String eventName, final String exchange, final ExchangeType exchangeType,
            final String routingKey, final Scope scope, final String userId, final String sessionId,
            final String subscriptionId, final boolean persistent, final Integer cacheTtl, final boolean propagate) {
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
        this.propagate = propagate;
    }

    public String getEventName() {
        return eventName;
    }

    public String getExchange() {
        return exchange;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public Scope getScope() {
        return scope;
    }

    public String getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public boolean getPersistent() {
        return persistent;
    }

    public Integer getCacheTtl() {
        return cacheTtl;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public boolean getPropagate() {
        return propagate;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final RoutingDetails that = (RoutingDetails) o;
        return persistent == that.persistent && Objects.equals(eventName, that.eventName) && Objects.equals(
                exchange, that.exchange) && exchangeType == that.exchangeType && Objects.equals(routingKey,
                        that.routingKey)
                && scope == that.scope && Objects.equals(userId, that.userId)
                && Objects.equals(sessionId, that.sessionId) && Objects.equals(subscriptionId,
                        that.subscriptionId)
                && Objects.equals(cacheTtl, that.cacheTtl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventName, exchange, exchangeType, routingKey, scope, userId, sessionId, subscriptionId, persistent,
                cacheTtl);
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
            return new MiscRoutingDetailsBuilder(eventName, exchange, exchangeType, routingKey, scope, true);
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

        private boolean propagate;

        public MiscRoutingDetailsBuilder(final String eventName, final String exchange, final ExchangeType exchangeType,
                final String routingKey, final Scope scope, final boolean propagate) {

            this.eventName = eventName;
            this.exchange = exchange;
            this.exchangeType = exchangeType;
            this.routingKey = routingKey;
            this.scope = scope;
            this.propagate = propagate;
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

        public MiscRoutingDetailsBuilder propagate(final boolean propagate) {
            this.propagate = propagate;
            return this;
        }

        public RoutingDetails build() {
            return new RoutingDetails(eventName, exchange, exchangeType, routingKey, scope, userId, sessionId, subscriptionId,
                    persistent, cacheTtl, propagate);
        }
    }
}
