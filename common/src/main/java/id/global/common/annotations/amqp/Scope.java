package id.global.common.annotations.amqp;

public enum Scope {
    INTERNAL,
    FRONTEND, //request message on websocket
    WEBSOCKET,
    SESSION,
    BROADCAST
}
