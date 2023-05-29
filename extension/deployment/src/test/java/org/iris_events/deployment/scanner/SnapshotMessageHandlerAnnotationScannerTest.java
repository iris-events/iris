package org.iris_events.deployment.scanner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import id.global.common.auth.jwt.Role;
import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.Scope;
import org.iris_events.annotations.SnapshotMessageHandler;
import org.iris_events.common.message.SnapshotRequested;
import org.iris_events.BaseIndexingTest;
import org.iris_events.deployment.builditem.MessageHandlerInfoBuildItem;
import org.iris_events.deployment.validation.AnnotationInstanceValidator;

class SnapshotMessageHandlerAnnotationScannerTest extends BaseIndexingTest {

    private SnapshotMessageHandlerAnnotationScanner annotationScanner;

    @BeforeEach
    void beforeEach() {
        final var annotationInstanceValidator = Mockito.mock(AnnotationInstanceValidator.class);
        annotationScanner = new SnapshotMessageHandlerAnnotationScanner(annotationInstanceValidator);
    }

    @Test
    public void messageHandlerScannerShouldScanServiceAnnotations() {
        final var messageHandlerInfoBuildItems = scanService(MessageHandlerService.class);

        assertNotNull(messageHandlerInfoBuildItems);
        assertEquals(2, messageHandlerInfoBuildItems.size());
    }

    @Test
    public void messageHandlerScannerShouldSetAllFields() {
        final IndexView index = indexOf(SnapshotMessageHandlerService.class);
        final var messageHandlerInfoBuildItems = annotationScanner.scanHandlerAnnotations(index);
        assertThat(messageHandlerInfoBuildItems, hasSize(1));

        final var messageHandlerInfoBuildItem = messageHandlerInfoBuildItems.get(0);

        assertThat(messageHandlerInfoBuildItem.isDurable(), is(false));
        assertThat(messageHandlerInfoBuildItem.isAutoDelete(), is(true));
        assertThat(messageHandlerInfoBuildItem.isQueuePerInstance(), is(false));
        final var snapshotRequestedType = Type.create(DotName.createSimple(SnapshotRequested.class.getName()), Type.Kind.CLASS);
        assertThat(messageHandlerInfoBuildItem.getParameterType(), is(snapshotRequestedType));
        assertThat(messageHandlerInfoBuildItem.getMethodName(), is("handleSnapshotApple"));
        assertThat(messageHandlerInfoBuildItem.getName(), is("snapshot-requested"));
        assertThat(messageHandlerInfoBuildItem.getExchangeType(), is(ExchangeType.TOPIC));
        assertThat(messageHandlerInfoBuildItem.getBindingKeys(), hasSize(1));
        final var bindingKey = messageHandlerInfoBuildItem.getBindingKeys().get(0);
        assertThat(bindingKey, is("apple-resource"));
        assertThat(messageHandlerInfoBuildItem.getScope(), is(Scope.INTERNAL));
        assertThat(messageHandlerInfoBuildItem.getPrefetchCount(), is(3));
        assertThat(messageHandlerInfoBuildItem.getTtl(), is(-1L));
        assertThat(messageHandlerInfoBuildItem.getDeadLetterQueue(), is("dead.dead-letter"));
        assertThat(messageHandlerInfoBuildItem.getRolesAllowed(), hasSize(1));
        final var role = messageHandlerInfoBuildItem.getRolesAllowed().iterator().next();
        assertThat(role, is(Role.AUTHENTICATED));
    }

    private List<MessageHandlerInfoBuildItem> scanService(Class<?>... classes) {
        final IndexView index = indexOf(classes);
        return annotationScanner.scanHandlerAnnotations(index);
    }

    @SuppressWarnings("unused")
    public static class MessageHandlerService {

        @SnapshotMessageHandler(resourceType = "apple-resource")
        public void handleSnapshotApple(SnapshotRequested snapshotRequested) {
            System.out.println("Handling snapshotRequested");
        }

        @SnapshotMessageHandler(resourceType = "lemon-resource")
        public void handleSnapshotLemon(SnapshotRequested snapshotRequested) {
            System.out.println("Handling snapshotRequested");
        }

    }

    @SuppressWarnings("unused")
    public static class SnapshotMessageHandlerService {

        @SnapshotMessageHandler(resourceType = "apple-resource", rolesAllowed = { Role.AUTHENTICATED }, prefetchCount = 3)
        public void handleSnapshotApple(SnapshotRequested snapshotRequested) {
            System.out.println("Handling snapshotRequested");
        }

    }
}
