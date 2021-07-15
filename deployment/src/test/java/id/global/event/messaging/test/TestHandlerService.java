package id.global.event.messaging.test;

import javax.enterprise.context.ApplicationScoped;

import id.global.asyncapi.spec.annotations.MessageHandler;

@ApplicationScoped
public class TestHandlerService {

    public static final String EVENT_QUEUE = "EventQueue";
    public static final String EVENT_QUEUE_PRIORITY = "EventQueue_priority";

    @MessageHandler(queue = EVENT_QUEUE)
    public void handle(Event event) {
        System.out.println("Handling event");
    }

    @MessageHandler(queue = EVENT_QUEUE_PRIORITY)
    public void handlePriority(Event event) {
        System.out.println("Handling priority event");
    }

}
