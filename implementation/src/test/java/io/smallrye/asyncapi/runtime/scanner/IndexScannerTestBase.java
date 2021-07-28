package io.smallrye.asyncapi.runtime.scanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

import io.smallrye.asyncapi.api.AsyncApiConfig;
import io.smallrye.asyncapi.api.AsyncApiConfigImpl;
import io.smallrye.asyncapi.api.AsyncApiConstants;

public class IndexScannerTestBase {

    protected static String pathOf(Class<?> clazz) {
        return clazz.getName().replace('.', '/').concat(".class");
    }

    private static InputStream tcclGetResourceAsStream(String path) {
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

    protected static void index(Indexer indexer, String resName) {
        try {
            InputStream stream = tcclGetResourceAsStream(resName);
            indexer.index(stream);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    public static AsyncApiConfig emptyConfig() {
        return dynamicConfig(Collections.emptyMap());
    }

    public static AsyncApiConfig excludeFromSchemasTestConfig(Set<String> excludePackagePrefixes) {
        Map<String, Object> properties = new HashMap<>();

        String collect = excludePackagePrefixes.stream().collect(Collectors.joining(","));

        properties.put(AsyncApiConstants.EXCLUDE_FROM_SCHEMAS, collect);
        return dynamicConfig(properties);
    }

    public static AsyncApiConfig scanDependenciesDisableConfig() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(AsyncApiConstants.SCAN_DEPENDENCIES_DISABLE, true);
        return dynamicConfig(properties);
    }

    @SuppressWarnings("unchecked")
    public static AsyncApiConfig dynamicConfig(Map<String, Object> properties) {
        return new AsyncApiConfigImpl(new Config() {
            @Override
            public <T> T getValue(String propertyName, Class<T> propertyType) {
                return (T) properties.get(propertyName);
            }

            @Override
            public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
                return (Optional<T>) Optional.ofNullable(properties.getOrDefault(propertyName, null));
            }

            @Override
            public Iterable<String> getPropertyNames() {
                return properties.keySet();
            }

            @Override
            public Iterable<ConfigSource> getConfigSources() {
                // Not needed for this test case
                return Collections.emptyList();
            }

            @Override
            public ConfigValue getConfigValue(String propertyName) {
                return new ConfigValue() {
                    @Override
                    public String getName() {
                        return propertyName;
                    }

                    @Override
                    public String getValue() {
                        return (String) properties.get(propertyName);
                    }

                    @Override
                    public String getRawValue() {
                        return getValue();
                    }

                    @Override
                    public String getSourceName() {
                        // Not needed for this test case
                        return null;
                    }

                    @Override
                    public int getSourceOrdinal() {
                        return 0;
                    }
                };
            }

            @Override
            public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
                return Optional.empty();
            }

            @Override
            public <T> T unwrap(Class<T> type) {
                throw new IllegalArgumentException();
            }
        });
    }
}
