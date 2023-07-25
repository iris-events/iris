package org.iris_events.asyncapi.runtime.scanner.model;

import java.util.Set;

import org.jboss.jandex.Type;

public class ChannelInfo {

    private final String eventKey;
    private final ChannelBindingsInfo bindingsInfo;
    private final OperationBindingsInfo operationBindingsInfo;
    private final String operationType;
    private final Set<String> rolesAllowed;
    private final String deadLetterQueue;
    private final Integer ttl;
    private final Type responseType;

    public ChannelInfo(String eventKey, ChannelBindingsInfo bindingsInfo, OperationBindingsInfo operationBindingsInfo,
            String operationType, Set<String> rolesAllowed,
            String deadLetterQueue, Integer ttl, Type responseType) {
        this.eventKey = eventKey;
        this.bindingsInfo = bindingsInfo;
        this.operationBindingsInfo = operationBindingsInfo;
        this.operationType = operationType;
        this.rolesAllowed = rolesAllowed;
        this.deadLetterQueue = deadLetterQueue;
        this.ttl = ttl;
        this.responseType = responseType;
    }

    public String getEventKey() {
        return eventKey;
    }

    public ChannelBindingsInfo getBindingsInfo() {
        return bindingsInfo;
    }

    public OperationBindingsInfo getOperationBindingsInfo() {
        return operationBindingsInfo;
    }

    public String getOperationType() {
        return operationType;
    }

    public Set<String> getRolesAllowed() {
        return rolesAllowed;
    }

    public String getDeadLetterQueue() {
        return deadLetterQueue;
    }

    public Integer getTtl() {
        return ttl;
    }

    public Type getResponseType() {
        return responseType;
    }
}
