package id.global.event.messaging.runtime.context;

public class MethodHandleContext {
    private Class<?> handlerClass;
    private Class<?> eventClass;
    private String methodName;

    public MethodHandleContext() {
    }

    public MethodHandleContext(Class<?> handlerClass, Class<?> eventClass, String methodName) {
        this.handlerClass = handlerClass;
        this.eventClass = eventClass;
        this.methodName = methodName;
    }

    public void setHandlerClass(Class<?> handlerClass) {
        this.handlerClass = handlerClass;
    }

    public void setEventClass(Class<?> eventClass) {
        this.eventClass = eventClass;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?> getHandlerClass() {
        return handlerClass;
    }

    public Class<?> getEventClass() {
        return eventClass;
    }

    public String getMethodName() {
        return methodName;
    }
}
