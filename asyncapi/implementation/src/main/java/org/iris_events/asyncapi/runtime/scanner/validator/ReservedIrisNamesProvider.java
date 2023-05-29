package org.iris_events.asyncapi.runtime.scanner.validator;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.iris_events.common.constants.Exchanges;
import org.iris_events.common.constants.Queues;

public class ReservedIrisNamesProvider {
    public static List<String> getReservedNames() {
        return Stream.concat(getExchangeNamesStream(), getQueueNamesStream()).collect(Collectors.toList());
    }

    private static Stream<String> getQueueNamesStream() {
        return Stream.of(Queues.DEAD_LETTER,
                Queues.ERROR,
                Queues.RETRY,
                Queues.RETRY_WAIT_ENDED)
                .map(Queues::getValue);
    }

    private static Stream<String> getExchangeNamesStream() {
        return Stream.of(Exchanges.BROADCAST,
                Exchanges.DEAD_LETTER,
                Exchanges.ERROR,
                Exchanges.FRONTEND,
                Exchanges.RETRY,
                Exchanges.SESSION,
                Exchanges.USER)
                .map(Exchanges::getValue);
    }
}
