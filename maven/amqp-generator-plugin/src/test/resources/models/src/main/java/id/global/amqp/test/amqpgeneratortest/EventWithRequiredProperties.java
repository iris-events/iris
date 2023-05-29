
package id.global.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import id.global.amqp.test.amqpgeneratortest.payload.Requirement;
import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.Scope;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


/**
 * Event With Required Properties
 * <p>
 * Required properties event.
 *
 */
@IrisGenerated
@Generated("jsonschema2pojo")
@Message(name = "event-with-required-properties", exchangeType = ExchangeType.FANOUT, routingKey = "event-with-required-properties", scope = Scope.FRONTEND, deadLetter = "dead.dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "requirement",
    "state_id",
    "value"
})
public class EventWithRequiredProperties implements Serializable
{

    /**
     * Requirement to verify.
     * (Required)
     *
     */
    @JsonProperty("requirement")
    @JsonPropertyDescription("Requirement to verify.")
    @Valid
    @NotNull
    private Requirement requirement;
    /**
     * Id of the state returned by Onboard Started event.
     * (Required)
     *
     */
    @JsonProperty("state_id")
    @JsonPropertyDescription("Id of the state returned by Onboard Started event.")
    @NotNull
    private String stateId;
    /**
     * Value of the requirement.
     * (Required)
     *
     */
    @JsonProperty("value")
    @JsonPropertyDescription("Value of the requirement.")
    @NotNull
    private String value;
    private final static long serialVersionUID = 7029086880915866375L;

    /**
     * No args constructor for use in serialization
     *
     */
    public EventWithRequiredProperties() {
    }

    /**
     *
     * @param stateId
     *     Id of the state returned by Onboard Started event.
     * @param requirement
     *     Requirement to verify.
     * @param value
     *     Value of the requirement.
     */
    public EventWithRequiredProperties(Requirement requirement, String stateId, String value) {
        super();
        this.requirement = requirement;
        this.stateId = stateId;
        this.value = value;
    }

    /**
     * Requirement to verify.
     * (Required)
     *
     */
    @JsonProperty("requirement")
    public Requirement getRequirement() {
        return requirement;
    }

    /**
     * Requirement to verify.
     * (Required)
     *
     */
    @JsonProperty("requirement")
    public void setRequirement(Requirement requirement) {
        this.requirement = requirement;
    }

    /**
     * Id of the state returned by Onboard Started event.
     * (Required)
     *
     */
    @JsonProperty("state_id")
    public String getStateId() {
        return stateId;
    }

    /**
     * Id of the state returned by Onboard Started event.
     * (Required)
     *
     */
    @JsonProperty("state_id")
    public void setStateId(String stateId) {
        this.stateId = stateId;
    }

    /**
     * Value of the requirement.
     * (Required)
     *
     */
    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    /**
     * Value of the requirement.
     * (Required)
     *
     */
    @JsonProperty("value")
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EventWithRequiredProperties.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("requirement");
        sb.append('=');
        sb.append(((this.requirement == null)?"<null>":this.requirement));
        sb.append(',');
        sb.append("stateId");
        sb.append('=');
        sb.append(((this.stateId == null)?"<null>":this.stateId));
        sb.append(',');
        sb.append("value");
        sb.append('=');
        sb.append(((this.value == null)?"<null>":this.value));
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
        result = ((result* 31)+((this.requirement == null)? 0 :this.requirement.hashCode()));
        result = ((result* 31)+((this.value == null)? 0 :this.value.hashCode()));
        result = ((result* 31)+((this.stateId == null)? 0 :this.stateId.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EventWithRequiredProperties) == false) {
            return false;
        }
        EventWithRequiredProperties rhs = ((EventWithRequiredProperties) other);
        return ((((this.requirement == rhs.requirement)||((this.requirement!= null)&&this.requirement.equals(rhs.requirement)))&&((this.value == rhs.value)||((this.value!= null)&&this.value.equals(rhs.value))))&&((this.stateId == rhs.stateId)||((this.stateId!= null)&&this.stateId.equals(rhs.stateId))));
    }

}
