
package id.global.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import javax.annotation.processing.Generated;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.IrisGenerated;
import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.Scope;


/**
 * Event With Required Properties
 * <p>
 * Required properties event.
 * 
 */
@IrisGenerated
@Message(name = "event-with-required-properties", exchangeType = ExchangeType.FANOUT, routingKey = "event-with-required-properties", scope = Scope.FRONTEND, deadLetter = "dead.dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "requirement",
    "state_id",
    "value"
})
@Generated("jsonschema2pojo")
public class EventWithRequiredProperties implements Serializable
{

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requirement")
    @NotNull
    private Object requirement;
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
    private final static long serialVersionUID = 3274290212430735327L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public EventWithRequiredProperties() {
    }

    /**
     * 
     * @param stateId
     * @param requirement
     * @param value
     */
    public EventWithRequiredProperties(Object requirement, String stateId, String value) {
        super();
        this.requirement = requirement;
        this.stateId = stateId;
        this.value = value;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requirement")
    public Object getRequirement() {
        return requirement;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requirement")
    public void setRequirement(Object requirement) {
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
