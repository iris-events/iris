package io.smallrye.asyncapi.runtime.scanner.model;

public enum Status {
    DORMANT("dormant"),
    LIVE("live"),
    DEAD("dead");

    private final String status;

    Status(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
