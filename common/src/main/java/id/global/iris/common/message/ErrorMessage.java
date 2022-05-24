package id.global.iris.common.message;

import id.global.iris.common.error.ErrorType;

public record ErrorMessage(ErrorType errorType, String code, String message) {
}
