
package id.global.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import id.global.amqp.test.amqpgeneratortest.payload.User;
import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.IrisGenerated;
import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.Scope;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;

@IrisGenerated
@Generated("jsonschema2pojo")
@Message(name = "list-payload-event", exchangeType = ExchangeType.FANOUT, routingKey = "list-payload-event", scope = Scope.INTERNAL, deadLetter = "dead.dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "userList"
})
public class ListPayloadEvent implements Serializable
{

    @JsonProperty("userList")
    @Valid
    private List<User> userList = new ArrayList<User>();
    private final static long serialVersionUID = -4750990094447391276L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public ListPayloadEvent() {
    }

    public ListPayloadEvent(List<User> userList) {
        super();
        this.userList = userList;
    }

    @JsonProperty("userList")
    public List<User> getUserList() {
        return userList;
    }

    @JsonProperty("userList")
    public void setUserList(List<User> userList) {
        this.userList = userList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ListPayloadEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("userList");
        sb.append('=');
        sb.append(((this.userList == null)?"<null>":this.userList));
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
        result = ((result* 31)+((this.userList == null)? 0 :this.userList.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ListPayloadEvent) == false) {
            return false;
        }
        ListPayloadEvent rhs = ((ListPayloadEvent) other);
        return ((this.userList == rhs.userList)||((this.userList!= null)&&this.userList.equals(rhs.userList)));
    }

}
