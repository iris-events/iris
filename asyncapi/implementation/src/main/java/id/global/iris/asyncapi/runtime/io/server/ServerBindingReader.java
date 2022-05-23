package id.global.iris.asyncapi.runtime.io.server;

import org.jboss.jandex.AnnotationValue;

import io.apicurio.datamodels.asyncapi.models.AaiServerBindings;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20ServerBindings;

public class ServerBindingReader {

    private ServerBindingReader() {
    }

    public static AaiServerBindings readServerBindings(final AnnotationValue annotationValue) {
        if (annotationValue != null) {
            AaiServerBindings bindings = new Aai20ServerBindings();

            bindings.http = null;
            bindings.ws = null;
            bindings.kafka = null;
            bindings.amqp = null;
            bindings.amqp1 = null;
            bindings.mqtt = null;
            bindings.mqtt5 = null;
            bindings.nats = null;
            bindings.jms = null;
            bindings.sns = null;
            bindings.sqs = null;
            bindings.stomp = null;
            bindings.redis = null;

            return bindings;
        }

        return null;
    }
}
