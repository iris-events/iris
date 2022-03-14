package id.global.iris.messaging.runtime.context;

public class MethodHandleContext {
    private Class<?> handlerClass;
    private Class<?> eventClass;
    private Class<?> returnEventClass;
    private String methodName;

    public MethodHandleContext() {
    }

    public MethodHandleContext(Class<?> handlerClass, Class<?> eventClass, Class<?> returnEventClass,
            String methodName) {
        this.handlerClass = handlerClass;
        this.eventClass = eventClass;
        this.returnEventClass = returnEventClass;
        this.methodName = methodName;
    }

    public void setHandlerClass(Class<?> handlerClass) {
        this.handlerClass = handlerClass;
    }

    public void setEventClass(Class<?> eventClass) {
        this.eventClass = eventClass;
    }

    public void setReturnEventClass(Class<?> returnEventClass) {
        this.returnEventClass = returnEventClass;
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

    public Class<?> getReturnEventClass() {
        return returnEventClass;
    }

    public String getMethodName() {
        return methodName;
    }
}
