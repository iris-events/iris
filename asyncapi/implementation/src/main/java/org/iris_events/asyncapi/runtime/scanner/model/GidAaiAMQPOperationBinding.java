package org.iris_events.asyncapi.runtime.scanner.model;

import io.apicurio.datamodels.asyncapi.models.bindings.AaiAMQPOperationBinding;

public class GidAaiAMQPOperationBinding extends AaiAMQPOperationBinding {

    private int deliveryMode;

    public int getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(final int deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

}
