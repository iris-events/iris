package id.global.event.messaging.runtime.recorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.global.event.messaging.runtime.EventAppInfoProvider;
import id.global.event.messaging.runtime.context.EventAppContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class EventAppRecorder {

    private static final Logger log = LoggerFactory.getLogger(EventAppRecorder.class.getName());

    public void registerEventAppContext(final BeanContainer beanContainer, final EventAppContext eventAppContext) {
        log.info("Event app context received: {}", eventAppContext);
        beanContainer.instance(EventAppInfoProvider.class).addEventAppContext(eventAppContext);
    }
}
