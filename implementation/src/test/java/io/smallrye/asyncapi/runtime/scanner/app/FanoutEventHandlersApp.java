package io.smallrye.asyncapi.runtime.scanner.app;

import java.util.Map;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import id.global.common.annotations.amqp.ConsumedEvent;
import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.MessageHandler;
import io.smallrye.asyncapi.runtime.scanner.model.User;
import io.smallrye.asyncapi.spec.annotations.EventApp;
import io.smallrye.asyncapi.spec.annotations.info.Info;

@EventApp(id = FanoutEventHandlersApp.ID, info = @Info(title = FanoutEventHandlersApp.TITLE, version = FanoutEventHandlersApp.VERSION))
public class FanoutEventHandlersApp {
    private static final Logger LOG = Logger.getLogger(FanoutEventHandlersApp.class);

    public static final String TITLE = "Fanout_event handlers";
    public static final String VERSION = "1.0.0";
    public static final String ID = "FanoutEventHandlersAppTest";
    public static final String EXCHANGE_V1 = "fanout-exchange-v1";
    public static final String EXCHANGE_V2 = "fanout-exchange-v2";

    @MessageHandler
    public void handleEventV1(TestEventV1 event) {
        LOG.info("Handle event: " + event);
    }

    @MessageHandler
    public void handleEventV1Params(TestEventV2 event, boolean flag) {
        LOG.info("Handle event: " + event + " with flag: " + flag);
    }

    @ConsumedEvent(exchange = EXCHANGE_V1, exchangeType = ExchangeType.FANOUT)
    public record TestEventV1(int id, String status, User user){}

    @ConsumedEvent(exchange = EXCHANGE_V2, exchangeType = ExchangeType.FANOUT)
    public record TestEventV2(
            int id,
            String name,
            String surname,
            User user,
            JsonNode payload,
            Map<String, String> someMap){}
}
