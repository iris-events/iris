
package id.global.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import id.global.common.iris.annotations.ExchangeType;
import id.global.common.iris.annotations.GlobalIdGenerated;
import id.global.common.iris.annotations.Message;
import id.global.common.iris.annotations.Scope;

@GlobalIdGenerated
@Message(name = "passthrough-inbound-event", exchangeType = ExchangeType.FANOUT, routingKey = "passthrough-inbound-event", scope = Scope.INTERNAL, deadLetter = "dead.dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id"
})
@Generated("jsonschema2pojo")
public class PassthroughInboundEvent implements Serializable
{

    @JsonProperty("id")
    private int id;
    private final static long serialVersionUID = -5862589394872190986L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public PassthroughInboundEvent() {
    }

    /**
     * 
     * @param id
     */
    public PassthroughInboundEvent(int id) {
        super();
        this.id = id;
    }

    @JsonProperty("id")
    public int getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(PassthroughInboundEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(this.id);
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
        result = ((result* 31)+ this.id);
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof PassthroughInboundEvent) == false) {
            return false;
        }
        PassthroughInboundEvent rhs = ((PassthroughInboundEvent) other);
        return (this.id == rhs.id);
    }

}