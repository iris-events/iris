package org.iris_events.deployment.builditem;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.Scope;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;

public final class MessageInfoBuildItem extends MultiBuildItem {
    private final ClassInfo annotatedClassInfo;
    private final ExchangeType exchangeType;
    private final String name;
    private final String routingKey;
    private final Scope scope;
    private final Integer cacheTtl;
    private final Type rpcResponseType;

    public MessageInfoBuildItem(
            final ClassInfo annotatedClassInfo,
            final ExchangeType exchangeType,
            final String name,
            final String routingKey,
            final Scope scope,
            final Integer cacheTtl,
            final Type rpcResponseType) {
        this.annotatedClassInfo = annotatedClassInfo;
        this.exchangeType = exchangeType;
        this.name = name;
        this.routingKey = routingKey;
        this.scope = scope;
        this.cacheTtl = cacheTtl;
        this.rpcResponseType = rpcResponseType;
    }

    public MessageInfoBuildItem(final ClassInfo annotatedClassInfo, final String name, final Type rpcResponseType) {
        this.annotatedClassInfo = annotatedClassInfo;
        this.exchangeType = null;
        this.name = name;
        this.routingKey = null;
        this.scope = null;
        this.cacheTtl = null;
        this.rpcResponseType = rpcResponseType;
    }

    public ClassInfo getAnnotatedClassInfo() {
        return annotatedClassInfo;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public String getName() {
        return name;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public Scope getScope() {
        return scope;
    }

    public Type getRpcResponseType() {
        return rpcResponseType;
    }

    @Override
    public String toString() {
        return "MessageInfoBuildItem{" +
                "annotatedClassInfo=" + annotatedClassInfo +
                ", exchangeType=" + exchangeType +
                ", name='" + name + '\'' +
                ", routingKey='" + routingKey + '\'' +
                ", scope=" + scope +
                ", cacheTtl=" + cacheTtl +
                ", rpcResponseType=" + rpcResponseType +
                '}';
    }
}
