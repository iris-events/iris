package id.global.event.messaging.runtime.requeue;

public record RetryQueue(String queueName, long ttl) {
}
