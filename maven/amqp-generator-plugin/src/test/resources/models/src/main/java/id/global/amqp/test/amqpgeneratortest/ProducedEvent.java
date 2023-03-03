
package id.global.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.IrisGenerated;
import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.Scope;

@IrisGenerated
@Message(name = "produced-event", exchangeType = ExchangeType.FANOUT, routingKey = "produced-event", scope = Scope.INTERNAL, deadLetter = "dead.dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id"
})
@Generated("jsonschema2pojo")
public class ProducedEvent implements Serializable
{

    @JsonProperty("id")
    private int id;
    private final static long serialVersionUID = 2707574487443489734L;

    /**
     * No args constructor for use in serialization
     *
     */
    public ProducedEvent() {
    }

    public ProducedEvent(int id) {
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
        sb.append(ProducedEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        if ((other instanceof ProducedEvent) == false) {
            return false;
        }
        ProducedEvent rhs = ((ProducedEvent) other);
        return (this.id == rhs.id);
    }

}
