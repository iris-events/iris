
package org.iris_events.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Generated;
import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.Scope;

@IrisGenerated
@Generated("jsonschema2pojo")
@Message(name = "rpc-response", exchangeType = ExchangeType.FANOUT, routingKey = "rpc-response", scope = Scope.INTERNAL, deadLetter = "dead.dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "rpcId"
})
public class RpcResponseEvent implements Serializable
{

    @JsonProperty("rpcId")
    private String rpcId;
    private final static long serialVersionUID = -2539091077586767018L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public RpcResponseEvent() {
    }

    public RpcResponseEvent(String rpcId) {
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
        sb.append(RpcResponseEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        if ((other instanceof RpcResponseEvent) == false) {
            return false;
        }
        RpcResponseEvent rhs = ((RpcResponseEvent) other);
        return ((this.rpcId == rhs.rpcId)||((this.rpcId!= null)&&this.rpcId.equals(rhs.rpcId)));
    }

}
