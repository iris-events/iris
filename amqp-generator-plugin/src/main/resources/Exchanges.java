package id.global.client.!!!;

public enum Exchanges {

    //TODO: generate with data from apicurio
    //TODO: add more options
    #####
//    EXCHANGE1_ROUTING_KEY1("EXCHANGE1", "ROUTING_KEY1", "direct"),
//    EXCHANGE1_ROUTING_KEY2("EXCHANGE1", "ROUTING_KEY2", "direct"),
//    EXCHANGE2_ROUTING_KEY1("EXCHANGE2", "ROUTING_KEY1", "fanout"),
//    EXCHANGE3_ROUTING_KEY2("EXCHANGE3", "ROUTING_KEY2", "topic"),
//    EXCHANGE4_ROUTING_KEY3("EXCHANGE4", "ROUTING_KEY3", "fanout");

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
