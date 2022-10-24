package id.global.iris.asyncapi.runtime.scanner.model;

import java.math.BigDecimal;

import io.apicurio.datamodels.asyncapi.v2.models.Aai20Schema;

public class GidAai20Schema extends Aai20Schema {
    public String existingJavaType;
    public BigDecimal exclusiveMaximum;
    public BigDecimal exclusiveMinimum;

    public String getExistingJavaType() {
        return existingJavaType;
    }

    public void setExistingJavaType(final String existingJavaType) {
        this.existingJavaType = existingJavaType;
    }

    public BigDecimal getExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public void setExclusiveMaximum(final BigDecimal exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    public BigDecimal getExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public void setExclusiveMinimum(final BigDecimal exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }
}
