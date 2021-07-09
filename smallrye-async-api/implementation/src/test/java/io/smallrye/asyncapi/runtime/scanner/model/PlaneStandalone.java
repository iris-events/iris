package io.smallrye.asyncapi.runtime.scanner.model;

import io.smallrye.asyncapi.spec.annotations.media.Schema;

@Schema(name = "PlaneStandalone")
public class PlaneStandalone {
    @Schema(required = true)
    private String model;

    @Schema(required = true)
    private int passengerCapacity;

    public PlaneStandalone(String model, int passengerCapacity) {
        this.model = model;
        this.passengerCapacity = passengerCapacity;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getPassengerCapacity() {
        return passengerCapacity;
    }

    public void setPassengerCapacity(int passengerCapacity) {
        this.passengerCapacity = passengerCapacity;
    }
}
