package id.global.event.messaging.it.events;

public class LoggingEvent {

    private String log;
    private Long level;

    public LoggingEvent() {
    }

    public LoggingEvent(String log, Long level) {
        this.log = log;
        this.level = level;
    }

    public String getLog() {
        return log;
    }

    public Long getLevel() {
        return level;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public void setLevel(Long level) {
        this.level = level;
    }
}