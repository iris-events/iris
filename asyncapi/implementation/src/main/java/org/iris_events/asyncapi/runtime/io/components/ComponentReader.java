package org.iris_events.asyncapi.runtime.io.components;

import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Components;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26ComponentsImpl;

/**
 * Reading the Components annotation and json node
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 * @author Eric Wittmann (eric.wittmann@gmail.com)
 * @see <a href=
 *      "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#componentsObject">componentsObject</a>
 */
public class ComponentReader {

    private ComponentReader() {
    }

    /**
     * Creates components with empty schemas.
     *
     * @return Components model
     */
    public static AsyncApi26Components create() {
        return new AsyncApi26ComponentsImpl();
    }
}
