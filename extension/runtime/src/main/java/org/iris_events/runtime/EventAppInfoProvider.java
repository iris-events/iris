package org.iris_events.runtime;

import jakarta.enterprise.context.ApplicationScoped;

import org.iris_events.context.EventAppContext;

@ApplicationScoped
public class EventAppInfoProvider {

    private EventAppContext eventAppContext;

    public EventAppInfoProvider() {
    }

    public void addEventAppContext(final EventAppContext eventAppContext) {
        this.eventAppContext = eventAppContext;
    }

    public EventAppContext getEventAppContext() {
        return eventAppContext;
    }

    public String getApplicationId() {
        return eventAppContext.getId();
    }
}
