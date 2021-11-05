package id.global.event.messaging.runtime;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import id.global.event.messaging.runtime.context.EventAppContext;

@ApplicationScoped
public class EventAppInfoProvider {

    private EventAppContext eventAppContext;

    public EventAppInfoProvider() {
    }

    public void addEventAppContext(final EventAppContext eventAppContext) {
        this.eventAppContext = eventAppContext;
    }

    public Optional<EventAppContext> getEventAppContext() {
        return Optional.ofNullable(eventAppContext);
    }
}
