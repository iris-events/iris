package id.global.iris.messaging.runtime.context;

public final class EventAppContext {
    private String id;

    public EventAppContext() {
    }

    public EventAppContext(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "EventAppContext{" +
                "id='" + id + '\'' +
                '}';
    }
}
