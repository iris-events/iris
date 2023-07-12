package org.iris_events.deployment.builditem;

import org.jboss.jandex.ClassInfo;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.Scope;
import io.quarkus.builder.item.MultiBuildItem;

public final class MessageInfoBuildItem extends MultiBuildItem {
    private final ClassInfo annotatedClassInfo;
    private final ExchangeType exchangeType;
    private final String name;
    private final String routingKey;
    private final Scope scope;
    private final Integer cacheTtl;

    public MessageInfoBuildItem(
            ClassInfo annotatedClassInfo,
            ExchangeType exchangeType,
            String name,
            String routingKey,
            Scope scope,
            Integer cacheTtl) {
        this.annotatedClassInfo = annotatedClassInfo;
        this.exchangeType = exchangeType;
        this.name = name;
        this.routingKey = routingKey;
        this.scope = scope;
        this.cacheTtl = cacheTtl;
    }

    public MessageInfoBuildItem(final ClassInfo annotatedClassInfo) {
        this.annotatedClassInfo = annotatedClassInfo;
        this.exchangeType = null;
        this.name = null;
        this.routingKey = null;
        this.scope = null;
        this.cacheTtl = null;
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
}
