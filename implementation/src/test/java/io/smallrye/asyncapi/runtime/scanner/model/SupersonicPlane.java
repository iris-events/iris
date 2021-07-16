package io.smallrye.asyncapi.runtime.scanner.model;

import io.smallrye.asyncapi.spec.annotations.media.Schema;

@Schema(allOf = { Plane.class, SupersonicPlane.class })
public class SupersonicPlane extends Plane {
    long machSpeed;

    public SupersonicPlane(String model, int passengerCapacity) {
        super(model, passengerCapacity);
        this.machSpeed = 1;
    }

    public SupersonicPlane(String model, int passengerCapacity, long machSpeed) {
        super(model, passengerCapacity);
        this.machSpeed = machSpeed;
    }

    public long getMachSpeed() {
        return machSpeed;
    }
}
