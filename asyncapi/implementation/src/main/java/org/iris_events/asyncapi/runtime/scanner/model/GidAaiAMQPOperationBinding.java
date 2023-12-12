package org.iris_events.asyncapi.runtime.scanner.model;

import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26BindingImpl;

public class GidAaiAMQPOperationBinding extends AsyncApi26BindingImpl {

    private int deliveryMode;

    public int getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(final int deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

}
