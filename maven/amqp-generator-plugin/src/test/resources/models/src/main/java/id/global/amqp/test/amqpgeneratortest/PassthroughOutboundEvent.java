
package id.global.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.Scope;
import jakarta.annotation.Generated;

@IrisGenerated
@Generated("jsonschema2pojo")
@Message(name = "passthrough-outbound-event", exchangeType = ExchangeType.FANOUT, routingKey = "passthrough-outbound-event", scope = Scope.INTERNAL, deadLetter = "dead.dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id"
})
public class PassthroughOutboundEvent implements Serializable
{

    @JsonProperty("id")
    private int id;
    private final static long serialVersionUID = -717248270109992202L;

    /**
     * No args constructor for use in serialization
     *
     */
    public PassthroughOutboundEvent() {
    }

    public PassthroughOutboundEvent(int id) {
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
        sb.append(PassthroughOutboundEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        if ((other instanceof PassthroughOutboundEvent) == false) {
            return false;
        }
        PassthroughOutboundEvent rhs = ((PassthroughOutboundEvent) other);
        return (this.id == rhs.id);
    }

}
