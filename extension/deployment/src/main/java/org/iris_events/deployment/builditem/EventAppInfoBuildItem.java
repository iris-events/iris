package org.iris_events.deployment.builditem;

import org.iris_events.context.EventAppContext;
import io.quarkus.builder.item.SimpleBuildItem;

public final class EventAppInfoBuildItem extends SimpleBuildItem {

    private final EventAppContext eventAppContext;

    public EventAppInfoBuildItem(EventAppContext eventAppContext) {
        this.eventAppContext = eventAppContext;
    }

    public EventAppContext getEventAppContext() {
        return eventAppContext;
    }
}
