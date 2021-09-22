package io.smallrye.asyncapi.runtime.scanner.model;

public class ChannelInfo {

    private final String eventKey;
    private final ChannelBindingsInfo bindingsInfo;
    private final String operationType;
    private final String[] rolesAllowed;

    public ChannelInfo(String eventKey, ChannelBindingsInfo bindingsInfo, String operationType, String[] rolesAllowed) {
        this.eventKey = eventKey;
        this.bindingsInfo = bindingsInfo;
        this.operationType = operationType;
        this.rolesAllowed = rolesAllowed;
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
}
