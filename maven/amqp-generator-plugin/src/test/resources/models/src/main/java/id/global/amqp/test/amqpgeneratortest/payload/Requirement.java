
package id.global.amqp.test.amqpgeneratortest.payload;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Requirement {

    PHONE("PHONE"),
    EMAIL("EMAIL"),
    GID_NAME("GID_NAME");
    private final String value;
    private final static Map<String, Requirement> CONSTANTS = new HashMap<String, Requirement>();

    static {
        for (Requirement c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    Requirement(String value) {
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
    public static Requirement fromValue(String value) {
        Requirement constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
