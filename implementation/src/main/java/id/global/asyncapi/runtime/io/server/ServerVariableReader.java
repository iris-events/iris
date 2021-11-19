package id.global.asyncapi.runtime.io.server;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import io.apicurio.datamodels.asyncapi.v2.models.Aai20ServerVariable;
import io.apicurio.datamodels.core.models.common.ServerVariable;
import id.global.asyncapi.runtime.util.JandexUtil;

/**
 * Reading the ServerVariable annotation and json node
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 * @author Eric Wittmann (eric.wittmann@gmail.com)
 * @see <a href=
 *      "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#serverVariableObject">serverVariableObject</a>
 */
public class ServerVariableReader {

    private ServerVariableReader() {
    }

    /**
     * Reads an array of ServerVariable annotations, returning a new {@link ServerVariable} model. The
     * annotation value is an array of ServerVariable annotations.
     *
     * @param annotationValue an arrays of {@literal @}ServerVariable annotations
     * @return a Map of Variable name and ServerVariable model
     */
    public static Map<String, ServerVariable> readServerVariables(final AnnotationValue annotationValue) {
        if (annotationValue == null) {
            return null;
        }
        AnnotationInstance[] nestedArray = annotationValue.asNestedArray();
        Map<String, ServerVariable> variables = new LinkedHashMap<>();
        for (AnnotationInstance serverVariableAnno : nestedArray) {
            String name = JandexUtil.stringValue(serverVariableAnno, ServerVariableConstant.PROP_NAME);
            if (name != null) {
                variables.put(name, readServerVariable(serverVariableAnno));
            }
        }
        return variables;
    }

    /**
     * Reads a single ServerVariable annotation.
     *
     * @param annotationInstance the {@literal @}ServerVariable annotation
     * @return the ServerVariable model
     */
    private static ServerVariable readServerVariable(final AnnotationInstance annotationInstance) {
        if (annotationInstance == null) {
            return null;
        }

        ServerVariable variable = new Aai20ServerVariable(JandexUtil.stringValue(annotationInstance, "name"));
        variable.description = JandexUtil.stringValue(annotationInstance, ServerVariableConstant.PROP_DESCRIPTION);
        variable.enum_ = JandexUtil.stringListValue(annotationInstance, ServerVariableConstant.PROP_ENUMERATION)
                .orElse(null);
        variable.default_ = JandexUtil.stringValue(annotationInstance, ServerVariableConstant.PROP_DEFAULT_VALUE);
        return variable;
    }
}
