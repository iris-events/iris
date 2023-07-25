
package org.iris_events.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import org.iris_events.amqp.test.amqpgeneratortest.payload.TestType;
import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.Scope;

@IrisGenerated
@Generated("jsonschema2pojo")
@Message(name = "event-with-described-enum", exchangeType = ExchangeType.FANOUT, routingKey = "event-with-described-enum", scope = Scope.INTERNAL, deadLetter = "dead.dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type"
})
public class EventWithDescribedEnum implements Serializable
{

    /**
     * Requirement to verify.
     *
     */
    @JsonProperty("type")
    @JsonPropertyDescription("Requirement to verify.")
    @Valid
    private TestType type;
    private final static long serialVersionUID = -5596360551939677335L;

    /**
     * No args constructor for use in serialization
     *
     */
    public EventWithDescribedEnum() {
    }

    /**
     *
     * @param type
     *     Requirement to verify.
     */
    public EventWithDescribedEnum(TestType type) {
        super();
        this.type = type;
    }

    /**
     * Requirement to verify.
     *
     */
    @JsonProperty("type")
    public TestType getType() {
        return type;
    }

    /**
     * Requirement to verify.
     *
     */
    @JsonProperty("type")
    public void setType(TestType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EventWithDescribedEnum.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
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
        result = ((result* 31)+((this.type == null)? 0 :this.type.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EventWithDescribedEnum) == false) {
            return false;
        }
        EventWithDescribedEnum rhs = ((EventWithDescribedEnum) other);
        return ((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type)));
    }

}