package id.global.models.!!!.client;

public enum Exchanges {

    //TODO: generate with data from apicurio
    //TODO: add more option
#####

    public final String exchange;
    public final String routingKey;
    public final String exchangeType;


    Exchanges(String exchange, String routingKey, String exchangeType) {
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.exchangeType = exchangeType;
    }

    public String getExchangeType() {
        return exchangeType;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

}
