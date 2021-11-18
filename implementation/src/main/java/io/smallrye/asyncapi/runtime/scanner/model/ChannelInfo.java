package io.smallrye.asyncapi.runtime.scanner.model;

public class ChannelInfo {

    private final String eventKey;
    private final ChannelBindingsInfo bindingsInfo;
    private final String operationType;
    private final String[] rolesAllowed;
    private final String deadLetterQueue;
    private final int ttl;


    public ChannelInfo(String eventKey, ChannelBindingsInfo bindingsInfo, String operationType, String[] rolesAllowed,
            String deadLetterQueue, int ttl) {
        this.eventKey = eventKey;
        this.bindingsInfo = bindingsInfo;
        this.operationType = operationType;
        this.rolesAllowed = rolesAllowed;
        this.deadLetterQueue = deadLetterQueue;
        this.ttl = ttl;
    }

    public String getEventKey() {
        return eventKey;
    }

    public ChannelBindingsInfo getBindingsInfo() {
        return bindingsInfo;
    }

    public String getOperationType() {
        return operationType;
    }

    public String[] getRolesAllowed() {
        return rolesAllowed;
    }

    public String getDeadLetterQueue() {
        return deadLetterQueue;
    }

    public int getTtl() {
        return ttl;
    }
}
