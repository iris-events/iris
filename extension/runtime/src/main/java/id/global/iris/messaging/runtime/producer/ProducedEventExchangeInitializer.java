package id.global.iris.messaging.runtime.producer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.Scope;

@ApplicationScoped
public class ProducedEventExchangeInitializer {
    private final static Logger log = LoggerFactory.getLogger(ProducedEventExchangeInitializer.class);

    private final ExchangeDeclarator exchangeDeclarator;
    private final List<ProducerDefinedExchange> producerDefinedExchanges;

    @Inject
    public ProducedEventExchangeInitializer(ExchangeDeclarator exchangeDeclarator) {
        this.exchangeDeclarator = exchangeDeclarator;
        this.producerDefinedExchanges = new ArrayList<>();
    }

    public void addProducerDefinedExchange(String exchange, ExchangeType exchangeType, Scope scope) {
        log.info("Adding producer defined exchange {}", exchange);
        producerDefinedExchanges.add(new ProducerDefinedExchange(exchange, exchangeType, scope));
    }

    public List<ProducerDefinedExchange> getProducerDefinedExchanges() {
        return producerDefinedExchanges;
    }

    public void initExchanges() {
        producerDefinedExchanges.forEach(exchange -> {
            try {
                exchangeDeclarator.declareExchange(exchange.exchangeName(), exchange.type(),
                        exchange.scope().equals(Scope.FRONTEND));
            } catch (IOException e) {
                log.error("Could not get or create channel while initializing producer defined exchange", e);
                throw new RuntimeException("Could not get or create channel while initializing producer defined exchange", e);
            }
        });
    }
}
