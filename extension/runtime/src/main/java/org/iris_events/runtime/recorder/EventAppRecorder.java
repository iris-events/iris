package org.iris_events.runtime.recorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.iris_events.runtime.EventAppInfoProvider;
import org.iris_events.context.EventAppContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class EventAppRecorder {

    private static final Logger log = LoggerFactory.getLogger(EventAppRecorder.class.getName());

    public void registerEventAppContext(final BeanContainer beanContainer, final EventAppContext eventAppContext) {
        log.info("Registering Iris for application with id '{}'", eventAppContext.getId());
        beanContainer.beanInstance(EventAppInfoProvider.class).addEventAppContext(eventAppContext);
    }
}
