package id.global.asyncapi.runtime.io.channel.parameters;

import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import io.apicurio.datamodels.asyncapi.models.AaiParameter;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Parameter;
import id.global.asyncapi.runtime.io.schema.SchemaReader;
import id.global.asyncapi.runtime.util.JandexUtil;

public class ParameterReader {
    private ParameterReader() {
    }

    public static Map<String, AaiParameter> readParameters(AnnotationValue annotationValue) {
        if (annotationValue != null) {
            AnnotationInstance[] nestedArray = annotationValue.asNestedArray();
            Map<String, AaiParameter> parameterMap = new HashMap<>();

            for (AnnotationInstance parameterAnnotation : nestedArray) {
                AaiParameter parameter = readParameter(parameterAnnotation);
                parameterMap.put(parameter.getName(), parameter);
            }
            return parameterMap;
        }
        return null;
    }

    private static AaiParameter readParameter(AnnotationInstance annotationInstance) {
        if (annotationInstance != null) {
            AaiParameter parameter = new Aai20Parameter(
                    JandexUtil.stringValue(annotationInstance, ParameterConstant.PROP_NAME));

            parameter.description = JandexUtil.stringValue(annotationInstance, ParameterConstant.PROP_DESCRIPTION);
            parameter.schema = SchemaReader.readParameterSchema(annotationInstance.value(ParameterConstant.PROP_SCHEMA));

            return parameter;
        }
        return null;
    }
}
