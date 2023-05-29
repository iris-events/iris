package org.iris_events.deployment.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.AbstractAnnotationInstanceValidatorTest;
import org.iris_events.deployment.builditem.MessageHandlerInfoBuildItem;
import org.iris_events.deployment.MessageHandlerValidationException;

class MessageHandlerCompatibilityValidatorTest extends AbstractAnnotationInstanceValidatorTest {

    @ParameterizedTest
    @MethodSource
    void validate(List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems, Class<Exception> expectedException,
            String expectedMessage) {

        if (expectedException == null) {

            assertDoesNotThrow(() -> MessageHandlerCompatibilityValidator.validate(messageHandlerInfoBuildItems));
        } else {
            String exceptionMessage = assertThrows(expectedException,
                    () -> MessageHandlerCompatibilityValidator.validate(messageHandlerInfoBuildItems)).getMessage();
            MatcherAssert.assertThat(exceptionMessage, CoreMatchers.containsString(expectedMessage));
        }
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> validate() {
        return Stream.of(
                Arguments.of(List.of(
                        getItem("SomeClassName", Arrays.asList("foo", "bar")),
                        getItem("AnotherClassName", Arrays.asList("foo", "bar"))),
                        null, null),
                Arguments.of(List.of(
                        getItem("SomeClassName", Arrays.asList("foo", "bar")),
                        getItem("SomeClassName", Arrays.asList("fooo", "baar"))),
                        null, null),
                Arguments.of(List.of(
                        getItem("SomeClassName", null),
                        getItem("AnotherClassName", Collections.emptyList())),
                        null, null),
                Arguments.of(List.of(
                        getItem("SomeClassName", Arrays.asList("foo", "bar")),
                        getItem("SomeClassName", Arrays.asList("foo", "bar"))),
                        MessageHandlerValidationException.class,
                        "Duplicate message handler found for message SomeClassName and binding keys bar, foo"),
                Arguments.of(List.of(
                        getItem("SomeClassName", null),
                        getItem("SomeClassName", null)),
                        MessageHandlerValidationException.class,
                        "Duplicate message handler found for message SomeClassName and binding keys /"),
                Arguments.of(List.of(
                        getItem("SomeClassName", ExchangeType.TOPIC, List.of("foo")),
                        getItem("SomeClassName", ExchangeType.TOPIC, List.of("bar"))),
                        null, null),
                Arguments.of(List.of(
                        getItem("SomeClassName", ExchangeType.TOPIC, List.of("foo")),
                        getItem("SomeClassName", ExchangeType.TOPIC, List.of("bar")),
                        getItem("SomeClassName", ExchangeType.TOPIC, List.of("fooo", "bar"))),
                        MessageHandlerValidationException.class,
                        "Duplicate message handler found for the same binding key of message SomeClassName and binding keys bar, fooo"),
                Arguments.of(List.of(
                        getItem("SomeClassName", ExchangeType.FANOUT, List.of("foo")),
                        getItem("SomeClassName", ExchangeType.FANOUT, List.of("bar"))),
                        MessageHandlerValidationException.class,
                        "Duplicate message handler found for message SomeClassName and binding keys /"));
    }

    private static MessageHandlerInfoBuildItem getItem(final String someClassName, final List<String> bindingKeys) {
        return new MessageHandlerInfoBuildItem(null, Type.create(DotName.createSimple(someClassName), Type.Kind.CLASS),
                null, null, null, null, bindingKeys, null, false, false, false, 5, 5, null, null);
    }

    private static MessageHandlerInfoBuildItem getItem(final String someClassName, final ExchangeType exchangeType,
            final List<String> bindingKeys) {
        return new MessageHandlerInfoBuildItem(null, Type.create(DotName.createSimple(someClassName), Type.Kind.CLASS),
                null, null, null, exchangeType, bindingKeys, null, false, false, false, 5, 5, null, null);
    }
}
