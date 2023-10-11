package org.iris_events.producer;

/**
 * @param userId Used to override userId property on IRIS message.
 * @param correlationId Used to override correlationId property on IRIS message.
 */
public record SendOptions(String userId, String correlationId) {
}
