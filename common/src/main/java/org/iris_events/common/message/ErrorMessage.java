package org.iris_events.common.message;

import org.iris_events.common.ErrorType;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorMessage(@JsonProperty("error_type") ErrorType errorType,
        @JsonProperty("code") String code,
        @JsonProperty("message") String message) {

}
