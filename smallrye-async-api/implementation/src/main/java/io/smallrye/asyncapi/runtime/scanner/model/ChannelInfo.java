package io.smallrye.asyncapi.runtime.scanner.model;

public class ChannelInfo {

    private String eventKey;
    private ChannelBindingsInfo bindingsInfo;
    private String operationType;

    public ChannelInfo(String eventKey, ChannelBindingsInfo bindingsInfo, String operationType) {
        this.eventKey = eventKey;
        this.bindingsInfo = bindingsInfo;
        this.operationType = operationType;
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
}
