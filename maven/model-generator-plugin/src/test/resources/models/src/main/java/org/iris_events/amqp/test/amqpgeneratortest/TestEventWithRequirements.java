
package org.iris_events.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.iris_events.amqp.test.amqpgeneratortest.payload.User;
import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.Scope;

@IrisGenerated
@Generated("jsonschema2pojo")
@Message(name = "test-event-with-requirements", exchangeType = ExchangeType.DIRECT, routingKey = "test-event-with-requirements", scope = Scope.INTERNAL, deadLetter = "dead.dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "status",
    "user"
})
public class TestEventWithRequirements implements Serializable
{

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    @NotNull
    private int id;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("status")
    @NotNull
    private String status;
    @JsonProperty("user")
    @Valid
    private User user;
    private final static long serialVersionUID = 8109739522802285529L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public TestEventWithRequirements() {
    }

    public TestEventWithRequirements(int id, String status, User user) {
        super();
        this.id = id;
        this.status = status;
        this.user = user;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    public int getId() {
        return id;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    public void setId(int id) {
        this.id = id;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("user")
    public User getUser() {
        return user;
    }

    @JsonProperty("user")
    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TestEventWithRequirements.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(this.id);
        sb.append(',');
        sb.append("status");
        sb.append('=');
        sb.append(((this.status == null)?"<null>":this.status));
        sb.append(',');
        sb.append("user");
        sb.append('=');
        sb.append(((this.user == null)?"<null>":this.user));
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
        result = ((result* 31)+((this.user == null)? 0 :this.user.hashCode()));
        result = ((result* 31)+((this.status == null)? 0 :this.status.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof TestEventWithRequirements) == false) {
            return false;
        }
        TestEventWithRequirements rhs = ((TestEventWithRequirements) other);
        return (((this.id == rhs.id)&&((this.user == rhs.user)||((this.user!= null)&&this.user.equals(rhs.user))))&&((this.status == rhs.status)||((this.status!= null)&&this.status.equals(rhs.status))));
    }

}
