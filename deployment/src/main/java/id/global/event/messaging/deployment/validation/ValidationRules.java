package id.global.event.messaging.deployment.validation;

import java.util.Set;

public record ValidationRules(Integer paramCount, boolean allowExternalDependencyParams, boolean checkTopicValidity,
                              Set<String> requiredParams,
                              Set<String> kebabCaseOnlyParams) {
}
