
package id.global.amqp.test.amqpgeneratortest.payload;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {

    DORMANT("dormant"),
    LIVE("live"),
    DEAD("dead");
    private final String value;
    private final static Map<String, Status> CONSTANTS = new HashMap<String, Status>();

    static {
        for (Status c: values()) {
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
    public static Status fromValue(String value) {
        Status constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
