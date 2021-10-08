package id.global.event.messaging.deployment.validation;

import java.util.Set;

public record ValidationRules(Integer paramCount, Boolean allowExternalDependencyParams, Set<String> requiredParams,
                              Set<String> kebabCaseOnlyParams) {
}
