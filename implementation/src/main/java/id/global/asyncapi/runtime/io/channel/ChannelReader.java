package id.global.asyncapi.runtime.io.channel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import id.global.asyncapi.runtime.io.channel.operation.OperationConstant;
import id.global.asyncapi.runtime.io.channel.operation.OperationReader;
import id.global.asyncapi.runtime.io.channel.parameters.ParameterReader;
import io.apicurio.datamodels.asyncapi.models.AaiChannelItem;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20ChannelItem;
import id.global.asyncapi.runtime.util.JandexUtil;

public class ChannelReader {

    private ChannelReader() {
    }

    public static Optional<Map<String, AaiChannelItem>> readChannels(final AnnotationValue annotationValue) {
        if (annotationValue != null) {
            AnnotationInstance[] nestedArray = annotationValue.asNestedArray();
            Map<String, AaiChannelItem> channelMap = new HashMap<>();
            for (AnnotationInstance channelAnno : nestedArray) {
                AaiChannelItem channel = readChannel(channelAnno);
                channelMap.put(channel.getName(), channel);
            }
            return Optional.of(channelMap);

        }
        return Optional.empty();
    }

    public static AaiChannelItem readChannel(AnnotationInstance annotationInstance) {
        if (annotationInstance != null) {

            AaiChannelItem channelItem = new Aai20ChannelItem(
                    JandexUtil.stringValue(annotationInstance, ChannelConstant.PROP_NAME));
            channelItem.description = JandexUtil.stringValue(annotationInstance, ChannelConstant.PROP_DESCRIPTION);

            if (annotationInstance.value(OperationConstant.PROP_SUBSCRIBE) != null) {
                channelItem.subscribe = OperationReader
                        .readOperation(annotationInstance.value(OperationConstant.PROP_SUBSCRIBE).asNested(),
                                OperationConstant.PROP_SUBSCRIBE);
            }
            if (annotationInstance.value(OperationConstant.PROP_PUBLISH) != null) {
                channelItem.publish = OperationReader
                        .readOperation(annotationInstance.value(OperationConstant.PROP_PUBLISH).asNested(),
                                OperationConstant.PROP_PUBLISH);
            }

            channelItem.parameters = ParameterReader
                    .readParameters(annotationInstance.value(OperationConstant.PROP_PARAMETERS));
            channelItem.bindings = null; // TODO BindingsReader
            return channelItem;
        }
        return null;
    }

}
