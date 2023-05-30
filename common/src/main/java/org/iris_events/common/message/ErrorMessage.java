package org.iris_events.common.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.iris_events.common.ErrorType;

public record ErrorMessage(@JsonProperty("error_type") ErrorType errorType,
                           @JsonProperty("code") String code,
                           @JsonProperty("message") String message) {

}
