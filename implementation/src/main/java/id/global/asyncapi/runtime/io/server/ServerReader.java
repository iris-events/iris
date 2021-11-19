package id.global.asyncapi.runtime.io.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import io.apicurio.datamodels.asyncapi.models.AaiServer;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Server;
import id.global.asyncapi.runtime.util.JandexUtil;

/**
 * Reading the Server annotation and json node
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 * @author Eric Wittmann (eric.wittmann@gmail.com)
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#serverObject">serverObject</a>
 */
public class ServerReader {

    private ServerReader() {
    }

    /**
     * Reads any Server annotations.The annotation value is an array of Server annotations.
     *
     * @param annotationValue an Array of {@literal @}Server annotations
     * @return a List of Server models
     */
    public static Optional<Map<String, AaiServer>> readServers(final AnnotationValue annotationValue) {
        if (annotationValue != null) {
            AnnotationInstance[] nestedArray = annotationValue.asNestedArray();
            Map<String, AaiServer> serverMap = new HashMap<>();
            for (AnnotationInstance serverAnno : nestedArray) {
                AaiServer server = readServer(serverAnno);
                serverMap.put(server.getName(), server);
            }
            return Optional.of(serverMap);
        }
        return Optional.empty();
    }

    /**
     * Reads a single Server annotation.
     *
     * @param annotationValue the {@literal @}Server annotation
     * @return a Server model
     */
    public static AaiServer readServer(final AnnotationValue annotationValue) {
        if (annotationValue != null) {
            return readServer(annotationValue.asNested());
        }
        return null;
    }

    /**
     * Reads a single Server annotation.
     *
     * @param annotationInstance the {@literal @}Server annotations instance
     * @return Server model
     */
    public static AaiServer readServer(final AnnotationInstance annotationInstance) {
        if (annotationInstance != null) {
            AaiServer server = new Aai20Server(JandexUtil.stringValue(annotationInstance, ServerConstant.PROP_NAME));
            server.url = JandexUtil.stringValue(annotationInstance, ServerConstant.PROP_URL);
            server.protocol = JandexUtil.stringValue(annotationInstance, ServerConstant.PROP_PROTOCOL);
            server.protocolVersion = JandexUtil.stringValue(annotationInstance, ServerConstant.PROP_PROTOCOL_VERSION);
            server.description = JandexUtil.stringValue(annotationInstance, ServerConstant.PROP_DESCRIPTION);

            server.variables = ServerVariableReader
                    .readServerVariables(annotationInstance.value(ServerConstant.PROP_VARIABLES));
            server.bindings = ServerBindingReader.readServerBindings(annotationInstance.value(ServerConstant.PROP_BINDINGS));
            server.security = ServerSecurityRequirementReader.readSecurityRequirements(annotationInstance.value(
                    ServerConstant.PROP_SECURITY))
                    .orElse(null);
            return server;
        }
        return null;
    }
}
