package id.global.asyncapi.runtime.io.channel.operation;

import org.jboss.jandex.AnnotationInstance;

import io.apicurio.datamodels.asyncapi.models.AaiOperation;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Operation;
import id.global.asyncapi.runtime.io.message.MessageReader;
import id.global.asyncapi.runtime.util.JandexUtil;

public class OperationReader {

    private OperationReader() {
    }

    public static AaiOperation readOperation(AnnotationInstance annotationInstance, String opType) {
        if (annotationInstance != null) {
            AaiOperation operation = new Aai20Operation(opType);
            operation.operationId = JandexUtil.stringValue(annotationInstance, OperationConstant.PROP_OPERATION_ID);
            operation.description = JandexUtil.stringValue(annotationInstance, OperationConstant.PROP_DESCRIPTION);
            operation.summary = JandexUtil.stringValue(annotationInstance, OperationConstant.PROP_SUMMARY);

            operation.message = MessageReader
                    .readMessage(annotationInstance.value(OperationConstant.PROP_MESSAGE).asNested());
            return operation;
        }
        return null;
    }
}
