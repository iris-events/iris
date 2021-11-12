package id.global.event.messaging.deployment;

import id.global.event.messaging.runtime.context.EventAppContext;
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
