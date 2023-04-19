package id.global.iris.messaging.test;

import jakarta.enterprise.context.ApplicationScoped;

import id.global.iris.common.annotations.MessageHandler;

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
