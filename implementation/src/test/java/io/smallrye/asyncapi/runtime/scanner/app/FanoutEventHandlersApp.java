package io.smallrye.asyncapi.runtime.scanner.app;

import org.jboss.logging.Logger;

import id.global.asyncapi.spec.annotations.FanoutMessageHandler;
import io.smallrye.asyncapi.runtime.scanner.model.TestEventV1;
import io.smallrye.asyncapi.runtime.scanner.model.TestEventV2;
import io.smallrye.asyncapi.spec.annotations.EventApp;
import io.smallrye.asyncapi.spec.annotations.info.Info;

@EventApp(id = FanoutEventHandlersApp.ID, info = @Info(title = FanoutEventHandlersApp.TITLE, version = FanoutEventHandlersApp.VERSION))
public class FanoutEventHandlersApp {
    private static final Logger LOG = Logger.getLogger(FanoutEventHandlersApp.class);

    public static final String TITLE = "Fanout_event handlers";
    public static final String VERSION = "1.0.0";
    public static final String ID = "FanoutEventHandlersAppTest";
    public static final String EXCHANGE_V1 = "fanout_exchange_v1";
    public static final String EXCHANGE_V2 = "fanout_exchange_v2";
    public static final String EXCHANGE_V3 = "fanout_exchange_v3";

    @FanoutMessageHandler(exchange = EXCHANGE_V1)
    public void handleEventV1(TestEventV1 event) {
        LOG.info("Handle event: " + event);
    }

    @FanoutMessageHandler(exchange = EXCHANGE_V2, eventType = TestEventV2.class)
    public void handleEventV1Params(TestEventV2 event, boolean flag) {
        LOG.info("Handle event: " + event + " with flag: " + flag);
    }

    @FanoutMessageHandler(exchange = EXCHANGE_V3)
    public void handleEventV1OnExchangeV3(TestEventV1 event) {
        LOG.info("Handle event: " + event);
    }
}
