package id.global.iris.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;
import id.global.iris.common.annotations.Scope;
import id.global.iris.common.annotations.SnapshotMessageHandler;
import id.global.iris.common.message.SnapshotRequested;

public class BaseIndexingTest {
    protected Index indexOf(Class<?>... classes) {
        Indexer indexer = new Indexer();

        for (Class<?> klazz : classes) {
            index(indexer, pathOf(klazz));
        }
        index(indexer, pathOf(Message.class));
        index(indexer, pathOf(MessageHandler.class));
        index(indexer, pathOf(SnapshotMessageHandler.class));
        index(indexer, pathOf(Scope.class));
        index(indexer, pathOf(ExchangeType.class));
        index(indexer, pathOf(SnapshotRequested.class));

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
