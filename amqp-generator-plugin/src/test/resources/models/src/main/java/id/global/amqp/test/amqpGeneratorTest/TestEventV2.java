
package id.global.amqp.test.amqpGeneratorTest;

import java.io.Serializable;
import javax.annotation.processing.Generated;
import javax.validation.Valid;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import id.global.amqp.test.amqpGeneratorTest.payload.User;
import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.GlobalIdGenerated;
import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.Scope;

@GlobalIdGenerated
@Message(name = "test-event-v2", exchangeType = ExchangeType.DIRECT, routingKey = "test-event-v2", scope = Scope.INTERNAL, deadLetter = "dead-letter", ttl = 10000)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "name",
    "payload",
    "someMap",
    "surname",
    "user"
})
@Generated("jsonschema2pojo")
public class TestEventV2 implements Serializable
{

    @JsonProperty("id")
    private int id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("payload")
    private Object payload;
    @JsonProperty("someMap")
    private Object someMap;
    @JsonProperty("surname")
    private String surname;
    @JsonProperty("user")
    @Valid
    private User user;
    private final static long serialVersionUID = -6551040547463879970L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public TestEventV2() {
    }

    /**
     * 
     * @param payload
     * @param surname
     * @param name
     * @param id
     * @param someMap
     * @param user
     */
    public TestEventV2(int id, String name, Object payload, Object someMap, String surname, User user) {
        super();
        this.id = id;
        this.name = name;
        this.payload = payload;
        this.someMap = someMap;
        this.surname = surname;
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

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("payload")
    public Object getPayload() {
        return payload;
    }

    @JsonProperty("payload")
    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @JsonProperty("someMap")
    public Object getSomeMap() {
        return someMap;
    }

    @JsonProperty("someMap")
    public void setSomeMap(Object someMap) {
        this.someMap = someMap;
    }

    @JsonProperty("surname")
    public String getSurname() {
        return surname;
    }

    @JsonProperty("surname")
    public void setSurname(String surname) {
        this.surname = surname;
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
        sb.append(TestEventV2 .class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(this.id);
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("payload");
        sb.append('=');
        sb.append(((this.payload == null)?"<null>":this.payload));
        sb.append(',');
        sb.append("someMap");
        sb.append('=');
        sb.append(((this.someMap == null)?"<null>":this.someMap));
        sb.append(',');
        sb.append("surname");
        sb.append('=');
        sb.append(((this.surname == null)?"<null>":this.surname));
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
        result = ((result* 31)+((this.payload == null)? 0 :this.payload.hashCode()));
        result = ((result* 31)+((this.surname == null)? 0 :this.surname.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+ this.id);
        result = ((result* 31)+((this.someMap == null)? 0 :this.someMap.hashCode()));
        result = ((result* 31)+((this.user == null)? 0 :this.user.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof TestEventV2) == false) {
            return false;
        }
        TestEventV2 rhs = ((TestEventV2) other);
        return (((((((this.payload == rhs.payload)||((this.payload!= null)&&this.payload.equals(rhs.payload)))&&((this.surname == rhs.surname)||((this.surname!= null)&&this.surname.equals(rhs.surname))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&(this.id == rhs.id))&&((this.someMap == rhs.someMap)||((this.someMap!= null)&&this.someMap.equals(rhs.someMap))))&&((this.user == rhs.user)||((this.user!= null)&&this.user.equals(rhs.user))));
    }

}
