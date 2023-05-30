
package id.global.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.Scope;

@IrisGenerated
@Message(name = "test-generated-exchange", exchangeType = ExchangeType.TOPIC, routingKey = "test-generated-exchange", scope = Scope.INTERNAL, deadLetter = "dead.dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "status"
})
@Generated("jsonschema2pojo")
public class GeneratedTestEvent implements Serializable
{

    @JsonProperty("id")
    private int id;
    @JsonProperty("status")
    private String status;
    private final static long serialVersionUID = -6088058124903146385L;

    /**
     * No args constructor for use in serialization
     *
     */
    public GeneratedTestEvent() {
    }

    public GeneratedTestEvent(int id, String status) {
        super();
        this.id = id;
        this.status = status;
    }

    @JsonProperty("id")
    public int getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(int id) {
        this.id = id;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(GeneratedTestEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(this.id);
        sb.append(',');
        sb.append("status");
        sb.append('=');
        sb.append(((this.status == null)?"<null>":this.status));
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
        result = ((result* 31)+((this.status == null)? 0 :this.status.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof GeneratedTestEvent) == false) {
            return false;
        }
        GeneratedTestEvent rhs = ((GeneratedTestEvent) other);
        return ((this.id == rhs.id)&&((this.status == rhs.status)||((this.status!= null)&&this.status.equals(rhs.status))));
    }

}
