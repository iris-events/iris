package org.iris_events.deployment.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.annotations.Scope;
import org.iris_events.AbstractAnnotationInstanceValidatorTest;
import org.iris_events.deployment.MessageHandlerValidationException;

class PerInstanceParamAnnotationValidatorTest extends AbstractAnnotationInstanceValidatorTest {

    public static final Class<MessageHandler> MESSAGE_HANDLER_CLASS = MessageHandler.class;

    @ParameterizedTest
    @ValueSource(classes = { DefaultPerInstanceParamFrontendScopeHandlerService.class,
            PerInstanceParamFalseFrontendScopeHandlerService.class,
            PerInstanceParamTrueInternalScopeHandlerService.class,
            PerInstanceParamTrueUserScopeHandlerService.class,
            PerInstanceParamTrueSessionScopeHandlerService.class,
            PerInstanceParamTrueBroadcastScopeHandlerService.class
    })
    void validate(Class<?> serviceClass) {
        final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_CLASS, serviceClass);
        final var validator = getValidatorService(serviceClass, ValidFrontendScopeEvent.class, ValidInternalScopeEvent.class,
                ValidUserScopeEvent.class, ValidSessionScopeEvent.class, ValidBroadcastScopeEvent.class);

        assertDoesNotThrow(() -> validator.validate(annotationInstance));
    }

    @Test
    void validateUnsupportedMessageScope() {
        final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_CLASS,
                PerInstanceParamTrueFrontendScopeHandlerService.class);
        final var validator = getValidatorService(PerInstanceParamTrueFrontendScopeHandlerService.class,
                ValidFrontendScopeEvent.class);

        final var exception = assertThrows(MessageHandlerValidationException.class,
                () -> validator.validate(annotationInstance));

        assertThat(exception.getMessage(), containsString("perInstance = true"));
        assertThat(exception.getMessage(), containsString("is not supported for the \"FRONTEND\" message scope"));
    }

    private PerInstanceParamAnnotationValidator getValidatorService(Class<?>... annotatedClasses) {
        final var index = indexOf(annotatedClasses);
        return new PerInstanceParamAnnotationValidator(index);
    }

    private static class DefaultPerInstanceParamFrontendScopeHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler
        public void handle(ValidFrontendScopeEvent event) {
        }

    }

    private static class PerInstanceParamFalseFrontendScopeHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(perInstance = false)
        public void handle(ValidFrontendScopeEvent event) {
        }

    }

    private static class PerInstanceParamTrueFrontendScopeHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(perInstance = true)
        public void handle(ValidFrontendScopeEvent event) {
        }

    }

    private static class PerInstanceParamTrueInternalScopeHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(perInstance = true)
        public void handle(ValidInternalScopeEvent event) {
        }

    }

    private static class PerInstanceParamTrueUserScopeHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(perInstance = true)
        public void handle(ValidUserScopeEvent event) {
        }

    }

    private static class PerInstanceParamTrueSessionScopeHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(perInstance = true)
        public void handle(ValidSessionScopeEvent event) {
        }

    }

    private static class PerInstanceParamTrueBroadcastScopeHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(perInstance = true)
        public void handle(ValidBroadcastScopeEvent event) {
        }

    }

    @Message(name = "kebab-exchange", scope = Scope.FRONTEND)
    public record ValidFrontendScopeEvent() {
    }

    @Message(name = "kebab-exchange", scope = Scope.INTERNAL)
    public record ValidInternalScopeEvent() {
    }

    @Message(name = "kebab-exchange", scope = Scope.USER)
    public record ValidUserScopeEvent() {
    }

    @Message(name = "kebab-exchange", scope = Scope.SESSION)
    public record ValidSessionScopeEvent() {
    }

    @Message(name = "kebab-exchange", scope = Scope.BROADCAST)
    public record ValidBroadcastScopeEvent() {
    }

}
