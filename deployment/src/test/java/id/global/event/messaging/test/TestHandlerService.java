package id.global.event.messaging.test;

import javax.enterprise.context.ApplicationScoped;

import id.global.common.annotations.amqp.MessageHandler;

@ApplicationScoped
public class TestHandlerService {

    public static final String EVENT_QUEUE_PRIORITY = "event-queue-priority";

    @MessageHandler
    public void handle(Event event) {
        System.out.println("Handling event");
    }

    @MessageHandler
    public void handlePriority(PriorityQueueEvent event) {
        System.out.println("Handling priority event");
    }

}
