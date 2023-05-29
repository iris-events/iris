package org.iris_events.asyncapi.runtime.scanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Status {
    @JsonProperty("dormant")
    DORMANT(),
    @JsonProperty("live")
    LIVE(),
    @JsonProperty("dead")
    DEAD();
}
