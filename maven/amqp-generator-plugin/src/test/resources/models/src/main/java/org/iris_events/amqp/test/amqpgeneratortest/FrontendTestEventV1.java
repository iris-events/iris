
package org.iris_events.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import org.iris_events.amqp.test.amqpgeneratortest.payload.User;
import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.Scope;

@IrisGenerated
@Generated("jsonschema2pojo")
@Message(name = "frontend-test-event-v1", exchangeType = ExchangeType.DIRECT, routingKey = "fe-test-event-v1", scope = Scope.FRONTEND, deadLetter = "dead.dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "status",
    "user"
})
public class FrontendTestEventV1 implements Serializable
{

    @JsonProperty("id")
    private int id;
    @JsonProperty("status")
    private String status;
    @JsonProperty("user")
    @Valid
    private User user;
    private final static long serialVersionUID = 3302005062956659561L;

    /**
     * No args constructor for use in serialization
     *
     */
    public FrontendTestEventV1() {
    }

    public FrontendTestEventV1(int id, String status, User user) {
        super();
        this.id = id;
        this.status = status;
        this.user = user;
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
        sb.append(FrontendTestEventV1 .class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        if ((other instanceof FrontendTestEventV1) == false) {
            return false;
        }
        FrontendTestEventV1 rhs = ((FrontendTestEventV1) other);
        return (((this.id == rhs.id)&&((this.user == rhs.user)||((this.user!= null)&&this.user.equals(rhs.user))))&&((this.status == rhs.status)||((this.status!= null)&&this.status.equals(rhs.status))));
    }

}