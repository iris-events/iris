package id.global.iris.asyncapi.runtime.scanner.model;

import io.apicurio.datamodels.asyncapi.v2.models.Aai20Schema;

public class GidAai20Schema extends Aai20Schema {
    public String existingJavaType;

    public String getExistingJavaType() {
        return existingJavaType;
    }

    public void setExistingJavaType(final String existingJavaType) {
        this.existingJavaType = existingJavaType;
    }
}
