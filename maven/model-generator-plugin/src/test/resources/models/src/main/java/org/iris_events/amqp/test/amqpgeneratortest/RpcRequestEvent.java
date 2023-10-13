
package org.iris_events.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Generated;
import org.iris_events.amqp.test.amqpgeneratortest.RpcResponseEvent;
import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.Scope;

@IrisGenerated
@Generated("jsonschema2pojo")
@Message(name = "rpc-request", exchangeType = ExchangeType.FANOUT, routingKey = "rpc-request", scope = Scope.INTERNAL, deadLetter = "dead.dead-letter", ttl = -1, rpcResponse = RpcResponseEvent.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "rpcId"
})
public class RpcRequestEvent implements Serializable
{

    @JsonProperty("rpcId")
    private String rpcId;
    private final static long serialVersionUID = 2229764995690693707L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public RpcRequestEvent() {
    }

    public RpcRequestEvent(String rpcId) {
        super();
        this.rpcId = rpcId;
    }

    @JsonProperty("rpcId")
    public String getRpcId() {
        return rpcId;
    }

    @JsonProperty("rpcId")
    public void setRpcId(String rpcId) {
        this.rpcId = rpcId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RpcRequestEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("rpcId");
        sb.append('=');
        sb.append(((this.rpcId == null)?"<null>":this.rpcId));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.rpcId == null)? 0 :this.rpcId.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RpcRequestEvent) == false) {
            return false;
        }
        RpcRequestEvent rhs = ((RpcRequestEvent) other);
        return ((this.rpcId == rhs.rpcId)||((this.rpcId!= null)&&this.rpcId.equals(rhs.rpcId)));
    }

}
