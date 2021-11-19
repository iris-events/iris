package id.global.asyncapi.runtime.scanner.consumer;

public interface Consumer<T> {
    void handle(final T event);
}
