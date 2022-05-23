package id.global.iris.messaging.runtime;

import javax.enterprise.context.ApplicationScoped;

import id.global.iris.messaging.runtime.context.EventAppContext;

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
