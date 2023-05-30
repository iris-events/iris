
package id.global.amqp.test.amqpgeneratortest;

import java.io.Serializable;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.Scope;
import org.iris_id.global.amqp.test.amqpgeneratortest.payload.MapValue;

@IrisGenerated
@Generated("jsonschema2pojo")
@Message(name = "map-payload-event", exchangeType = ExchangeType.FANOUT, routingKey = "map-payload-event", scope = Scope.INTERNAL, deadLetter = "dead.dead-letter", ttl = -1)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "userMap"
})
public class MapPayloadEvent implements Serializable
{

    @JsonProperty("userMap")
    @Valid
    private Map<String, MapValue> userMap;
    private final static long serialVersionUID = 3470087959575808259L;

    /**
     * No args constructor for use in serialization
     *
     */
    public MapPayloadEvent() {
    }

    public MapPayloadEvent(Map<String, MapValue> userMap) {
        super();
        this.userMap = userMap;
    }

    @JsonProperty("userMap")
    public Map<String, MapValue> getUserMap() {
        return userMap;
    }

    @JsonProperty("userMap")
    public void setUserMap(Map<String, MapValue> userMap) {
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
