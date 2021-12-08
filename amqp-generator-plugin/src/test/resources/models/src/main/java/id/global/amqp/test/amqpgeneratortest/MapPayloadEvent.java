
package id.global.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.GlobalIdGenerated;
import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.Scope;

@GlobalIdGenerated
@Message(name = "map-payload-event", exchangeType = ExchangeType.FANOUT, routingKey = "map-payload-event", scope = Scope.INTERNAL, deadLetter = "dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "userMap"
})
@Generated("jsonschema2pojo")
public class MapPayloadEvent implements Serializable
{

    @JsonProperty("userMap")
    private Object userMap;
    private final static long serialVersionUID = 5999294610289307180L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public MapPayloadEvent() {
    }

    /**
     * 
     * @param userMap
     */
    public MapPayloadEvent(Object userMap) {
        super();
        this.userMap = userMap;
    }

    @JsonProperty("userMap")
    public Object getUserMap() {
        return userMap;
    }

    @JsonProperty("userMap")
    public void setUserMap(Object userMap) {
        this.userMap = userMap;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(MapPayloadEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("userMap");
        sb.append('=');
        sb.append(((this.userMap == null)?"<null>":this.userMap));
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
        result = ((result* 31)+((this.userMap == null)? 0 :this.userMap.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof MapPayloadEvent) == false) {
            return false;
        }
        MapPayloadEvent rhs = ((MapPayloadEvent) other);
        return ((this.userMap == rhs.userMap)||((this.userMap!= null)&&this.userMap.equals(rhs.userMap)));
    }

}
