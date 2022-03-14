package id.global.iris.messaging.runtime.producer;

@FunctionalInterface
public interface RouteResolvingFunction {
    RoutingDetails resolve();
}
