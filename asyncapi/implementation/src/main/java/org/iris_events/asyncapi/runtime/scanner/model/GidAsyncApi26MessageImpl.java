package org.iris_events.asyncapi.runtime.scanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26MessageImpl;

public class GidAsyncApi26MessageImpl extends AsyncApi26MessageImpl {
    @JsonProperty("x-response")
    public Object response;

    public GidAsyncApi26MessageImpl() {
        super();
    }

}
