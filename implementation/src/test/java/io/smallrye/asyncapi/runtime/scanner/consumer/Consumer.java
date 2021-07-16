package io.smallrye.asyncapi.runtime.scanner.consumer;

public interface Consumer<T> {
    void handle(final T event);
}
