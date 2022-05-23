package id.global.iris.asyncapi.runtime.scanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Status {
    @JsonProperty("dormant")
    DORMANT(),
    @JsonProperty("live")
    LIVE(),
    @JsonProperty("dead")
    DEAD();
}
