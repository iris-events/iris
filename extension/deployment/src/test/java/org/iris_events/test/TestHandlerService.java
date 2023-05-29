package org.iris_events.test;

import jakarta.enterprise.context.ApplicationScoped;

import org.iris_events.annotations.MessageHandler;

@ApplicationScoped
public class TestHandlerService {

    public static final String EVENT_QUEUE_PRIORITY = "event-queue-priority";

    @MessageHandler(bindingKeys = "event-queue")
    public void handle(Event event) {
        System.out.println("Handling event");
    }

    @MessageHandler(bindingKeys = "event-queue-priority")
    public void handlePriority(PriorityQueueEvent event) {
        System.out.println("Handling priority event");
    }

}
