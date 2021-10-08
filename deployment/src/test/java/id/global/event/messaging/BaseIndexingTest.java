package id.global.event.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

public class BaseIndexingTest {
    protected Index indexOf(Class<?>... classes) {
        Indexer indexer = new Indexer();

        for (Class<?> klazz : classes) {
            index(indexer, pathOf(klazz));
        }

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
