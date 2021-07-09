package io.smallrye.asyncapi.runtime.io.message;

import org.jboss.jandex.AnnotationInstance;

import io.apicurio.datamodels.asyncapi.models.AaiMessage;
import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Message;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Schema;
import io.apicurio.datamodels.core.models.common.Schema;
import io.smallrye.asyncapi.runtime.util.JandexUtil;

public class MessageReader {

    public static final String COMPONENTS_SCHEMAS_TEMPLATE = "#/components/schemas/%s";

    private MessageReader() {
    }

    public static AaiMessage readMessage(AnnotationInstance annotationInstance) {
        if (annotationInstance != null) {
            String msgName = JandexUtil.stringValue(annotationInstance, ApiMessageConstant.PROP_NAME);

            Aai20Message message = new Aai20Message(msgName);
            message.name = msgName;
            message.title = JandexUtil.stringValue(annotationInstance, ApiMessageConstant.PROP_TITLE);
            message.summary = JandexUtil.stringValue(annotationInstance, ApiMessageConstant.PROP_SUMMARY);
            message.description = JandexUtil.stringValue(annotationInstance, ApiMessageConstant.PROP_DESCRIPTION);
            message.contentType = JandexUtil.stringValue(annotationInstance, ApiMessageConstant.PROP_CONTENT_TYPE);
            message.payload = readPayload(annotationInstance.value(ApiMessageConstant.PROP_PAYLOAD).asNested());
            return message;
        }
        return null;
    }

    private static Schema readPayload(AnnotationInstance payload) {
        String implementation = JandexUtil.stringValue(payload, ApiMessageConstant.PROP_IMPLEMENTATION);
        if (implementation != null) {
            AaiSchema schemaRef = new Aai20Schema();
            schemaRef.setReference(mapToComponentsSchemas(implementation));
            return schemaRef;
        }
        return null;
    }

    private static String mapToComponentsSchemas(String implementation) {
        String[] split = implementation.split("\\.");
        String className = split[split.length - 1];
        return String.format(COMPONENTS_SCHEMAS_TEMPLATE, className);
    }

}
