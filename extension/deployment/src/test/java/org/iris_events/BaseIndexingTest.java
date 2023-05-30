package org.iris_events;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import jakarta.annotation.security.RolesAllowed;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.annotations.Scope;
import org.iris_events.annotations.SnapshotMessageHandler;
import org.iris_events.common.message.SnapshotRequested;

public class BaseIndexingTest {
    protected Index indexOf(Class<?>... classes) {
        Indexer indexer = new Indexer();

        for (Class<?> klazz : classes) {
            index(indexer, pathOf(klazz));
        }
        index(indexer, pathOf(Message.class));
        index(indexer, pathOf(MessageHandler.class));
        index(indexer, pathOf(RolesAllowed.class));
        index(indexer, pathOf(SnapshotMessageHandler.class));
        index(indexer, pathOf(IrisGenerated.class));
        index(indexer, pathOf(Scope.class));
        index(indexer, pathOf(ExchangeType.class));
        index(indexer, pathOf(SnapshotRequested.class));

        return indexer.complete();
    }

    protected Index basicIndexOf(Class<?>... classes) {
        Indexer indexer = new Indexer();

        for (Class<?> klazz : classes) {
            index(indexer, pathOf(klazz));
        }
        index(indexer, pathOf(Message.class));
        index(indexer, pathOf(MessageHandler.class));
        index(indexer, pathOf(IrisGenerated.class));
        index(indexer, pathOf(RolesAllowed.class));

        return indexer.complete();
    }

    private void index(Indexer indexer, String resName) {
        try {
            InputStream stream = tcclGetResourceAsStream(resName);
            indexer.index(stream);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    private InputStream tcclGetResourceAsStream(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }

    private String pathOf(Class<?> clazz) {
        return clazz.getName().replace('.', '/').concat(".class");
    }
}
