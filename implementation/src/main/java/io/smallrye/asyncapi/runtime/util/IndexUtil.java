package io.smallrye.asyncapi.runtime.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

public class IndexUtil {
    public static String pathOf(Class<?> clazz) {
        return clazz.getName().replace('.', '/').concat(".class");
    }

    public static InputStream tcclGetResourceAsStream(String path) {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(path);
    }

    public static Index indexOf(Class<?>... classes) {
        Indexer indexer = new Indexer();

        for (Class<?> klazz : classes) {
            index(indexer, pathOf(klazz));
        }

        return indexer.complete();
    }

    public static void index(Indexer indexer, String resName) {
        try {
            InputStream stream = tcclGetResourceAsStream(resName);
            indexer.index(stream);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }
}
