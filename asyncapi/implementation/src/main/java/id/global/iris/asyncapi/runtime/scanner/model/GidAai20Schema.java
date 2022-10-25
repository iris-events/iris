package id.global.iris.asyncapi.runtime.scanner.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.apicurio.datamodels.asyncapi.v2.models.Aai20Schema;

public class GidAai20Schema extends Aai20Schema {
    public String existingJavaType;
    public BigDecimal exclusiveMaximum;
    public BigDecimal exclusiveMinimum;

    @JsonProperty("x-iris-generated")
    public Boolean generatedClass;

    public void setExistingJavaType(final String existingJavaType) {
        this.existingJavaType = existingJavaType;
    }

    public void setExclusiveMaximum(final BigDecimal exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    public void setExclusiveMinimum(final BigDecimal exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public void setGeneratedClass(final Boolean generatedClass) {
        this.generatedClass = generatedClass;
    }
}
