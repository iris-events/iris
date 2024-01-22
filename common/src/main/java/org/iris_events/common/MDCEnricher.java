package org.iris_events.common;

import static org.iris_events.common.MessagingHeaders.Message.CLIENT_TRACE_ID;
import static org.iris_events.common.MessagingHeaders.Message.CLIENT_VERSION;
import static org.iris_events.common.MessagingHeaders.Message.CORRELATION_ID;
import static org.iris_events.common.MessagingHeaders.Message.EVENT_TYPE;
import static org.iris_events.common.MessagingHeaders.Message.SESSION_ID;
import static org.iris_events.common.MessagingHeaders.Message.USER_ID;

import java.util.Map;
import java.util.Optional;

import org.slf4j.MDC;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BasicProperties;

public class MDCEnricher {
    public static void enrichMDC(final AMQP.BasicProperties properties) {
        getStringHeader(properties, SESSION_ID).ifPresent(s -> MDC.put(MDCProperties.SESSION_ID, s));
        getStringHeader(properties, USER_ID).ifPresent(s -> MDC.put(MDCProperties.USER_ID, s));
        getStringHeader(properties, CLIENT_TRACE_ID).ifPresent(s -> MDC.put(MDCProperties.CLIENT_TRACE_ID, s));
        getStringHeader(properties, CORRELATION_ID).ifPresent(s -> MDC.put(MDCProperties.CORRELATION_ID, s));
        getStringHeader(properties, EVENT_TYPE).ifPresent(s -> MDC.put(MDCProperties.EVENT_TYPE, s));
        getStringHeader(properties, CLIENT_VERSION).ifPresent(s -> MDC.put(MDCProperties.CLIENT_VERSION, s));
    }

    public static void enrichMDC(final Map<String, String> propertyValueMap) {
        propertyValueMap.forEach(MDC::put);
    }

    public static void put(final String property, final String value) {
        MDC.put(property, value);
    }

    private static Optional<String> getStringHeader(BasicProperties props, String name) {
        return Optional.ofNullable(props.getHeaders())
                .map(headers -> headers.get(name))
                .map(Object::toString);
    }
}
