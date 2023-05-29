package org.iris_events.asyncapi.runtime.scanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.apicurio.datamodels.asyncapi.v2.models.Aai20Message;

public class GidAai20Message extends Aai20Message {
    @JsonProperty("x-response")
    public Object response;

    public GidAai20Message(String name) {
        super(name);
    }

}
