package org.iris_events.asyncapi.runtime.scanner.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26SchemaImpl;

public class GidAsyncApi26Schema extends AsyncApi26SchemaImpl {
    public String existingJavaType;
    public BigDecimal exclusiveMaximum;
    public BigDecimal exclusiveMinimum;

    @JsonProperty("x-iris-generated")
    public Boolean irisGenerated;

    @JsonProperty("x-cached-message-ttl-seconds")
    public Integer cachedMessage;

    public void setExistingJavaType(final String existingJavaType) {
        this.existingJavaType = existingJavaType;
    }

    public void setExclusiveMaximum(final BigDecimal exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    public void setExclusiveMinimum(final BigDecimal exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public void setIrisGenerated(final Boolean irisGenerated) {
        this.irisGenerated = irisGenerated;
    }

    public void setCachedMessage(final Integer cachedMessage) {
        this.cachedMessage = cachedMessage;
    }

    @Override
    public String getType() {
        return super.getType();
    }

    @Override
    public void setType(final String value) {
        super.setType(value);
    }
}
