package myau;

import myau.Event;

public class PreWalkingUpdateEvent
extends Event {
    private RotationUpdateEvent rotation;

    public PreWalkingUpdateEvent() {
        this(null);
    }

    public PreWalkingUpdateEvent(RotationUpdateEvent rotation) {
        this.rotation = rotation;
    }

    public RotationUpdateEvent getRotation() {
        return rotation;
    }

    public void setRotation(RotationUpdateEvent rotation) {
        this.rotation = rotation;
    }
}
