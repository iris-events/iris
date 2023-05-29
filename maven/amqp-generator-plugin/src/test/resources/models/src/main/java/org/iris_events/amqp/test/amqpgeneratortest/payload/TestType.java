
package org.iris_events.amqp.test.amqpgeneratortest.payload;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TestType {

    FOO("FOO"),
    BAR("BAR");
    private final String value;
    private final static Map<String, TestType> CONSTANTS = new HashMap<String, TestType>();

    static {
        for (TestType c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    TestType(String value) {
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
    public static TestType fromValue(String value) {
        TestType constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
