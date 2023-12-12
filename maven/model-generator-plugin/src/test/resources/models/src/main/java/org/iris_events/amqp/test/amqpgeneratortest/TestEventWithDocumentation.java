
package org.iris_events.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.iris_events.amqp.test.amqpgeneratortest.payload.User;
import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.Scope;


/**
 * Event with extensive documentation for test purposes
 * 
 */
@IrisGenerated
@Generated("jsonschema2pojo")
@Message(name = "test-event-with-documentation", exchangeType = ExchangeType.DIRECT, routingKey = "test-event-with-documentation", scope = Scope.INTERNAL, deadLetter = "dead.dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "altId",
    "id",
    "status",
    "user"
})
public class TestEventWithDocumentation implements Serializable
{

    /**
     * Alternative event id
     * 
     */
    @JsonProperty("altId")
    @JsonPropertyDescription("Alternative event id")
    @DecimalMin("18")
    @DecimalMax("1.5E+2")
    private int altId;
    @JsonProperty("id")
    @DecimalMin("5")
    private int id;
    /**
     * status of the user entity
     * 
     */
    @JsonProperty("status")
    @JsonPropertyDescription("status of the user entity")
    private TestEventWithDocumentation.Status status;
    @JsonProperty("user")
    @Valid
    private User user;
    private final static long serialVersionUID = 8977767882921483896L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public TestEventWithDocumentation() {
    }

    /**
     * 
     * @param altId
     *     Alternative event id.
     * @param status
     *     status of the user entity.
     */
    public TestEventWithDocumentation(int altId, int id, TestEventWithDocumentation.Status status, User user) {
        super();
        this.altId = altId;
        this.id = id;
        this.status = status;
        this.user = user;
    }

    /**
     * Alternative event id
     * 
     */
    @JsonProperty("altId")
    public int getAltId() {
        return altId;
    }

    /**
     * Alternative event id
     * 
     */
    @JsonProperty("altId")
    public void setAltId(int altId) {
        this.altId = altId;
    }

    @JsonProperty("id")
    public int getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(int id) {
        this.id = id;
    }

    /**
     * status of the user entity
     * 
     */
    @JsonProperty("status")
    public TestEventWithDocumentation.Status getStatus() {
        return status;
    }

    /**
     * status of the user entity
     * 
     */
    @JsonProperty("status")
    public void setStatus(TestEventWithDocumentation.Status status) {
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
        sb.append(TestEventWithDocumentation.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("altId");
        sb.append('=');
        sb.append(this.altId);
        sb.append(',');
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
        result = ((result* 31)+ this.altId);
        result = ((result* 31)+((this.user == null)? 0 :this.user.hashCode()));
        result = ((result* 31)+((this.status == null)? 0 :this.status.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof TestEventWithDocumentation) == false) {
            return false;
        }
        TestEventWithDocumentation rhs = ((TestEventWithDocumentation) other);
        return ((((this.id == rhs.id)&&(this.altId == rhs.altId))&&((this.user == rhs.user)||((this.user!= null)&&this.user.equals(rhs.user))))&&((this.status == rhs.status)||((this.status!= null)&&this.status.equals(rhs.status))));
    }


    /**
     * status of the user entity
     * 
     */
    public enum Status {

        AVAILABLE("available"),
        PENDING("pending"),
        SOLD("sold");
        private final String value;
        private final static Map<String, TestEventWithDocumentation.Status> CONSTANTS = new HashMap<String, TestEventWithDocumentation.Status>();

        static {
            for (TestEventWithDocumentation.Status c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Status(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static TestEventWithDocumentation.Status fromValue(String value) {
            TestEventWithDocumentation.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
