package io.smallrye.asyncapi.runtime.io.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import io.apicurio.datamodels.asyncapi.models.AaiSecurityRequirement;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20SecurityRequirement;
import io.smallrye.asyncapi.runtime.util.JandexUtil;

public class ServerSecurityRequirementReader {
    private ServerSecurityRequirementReader() {
    }

    public static Optional<List<AaiSecurityRequirement>> readSecurityRequirements(final AnnotationValue annotationValue) {
        if (annotationValue != null) {
            AnnotationInstance[] nestedArray = annotationValue.asNestedArray();
            List<AaiSecurityRequirement> securityRequirements = new ArrayList<>();

            for (AnnotationInstance secReqAnno : nestedArray) {
                if (!JandexUtil.isRef(secReqAnno)) {
                    securityRequirements.add(readSecurityRequirement(secReqAnno));
                }
            }
            return Optional.of(securityRequirements);
        }
        return Optional.empty();
    }

    public static AaiSecurityRequirement readSecurityRequirement(final AnnotationInstance annotationValue) {
        if (annotationValue != null) {
            AaiSecurityRequirement securityRequirement = new Aai20SecurityRequirement();
            securityRequirement.addSecurityRequirementItem(
                    JandexUtil.stringValue(annotationValue, "name"),
                    JandexUtil.stringListValue(annotationValue, "scopes").orElse(Collections.emptyList()));
            return securityRequirement;
        }
        return null;
    }
}
