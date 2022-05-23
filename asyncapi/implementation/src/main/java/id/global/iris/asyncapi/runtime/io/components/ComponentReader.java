package id.global.iris.asyncapi.runtime.io.components;

import java.util.HashMap;

import io.apicurio.datamodels.asyncapi.models.AaiComponents;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Components;

/**
 * Reading the Components annotation and json node
 *
 * @see <a href=
 *      "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#componentsObject">componentsObject</a>
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 * @author Eric Wittmann (eric.wittmann@gmail.com)
 */
public class ComponentReader {

    private ComponentReader() {
    }

    /**
     * Creates components with empty schemas.
     *
     * @return Components model
     */
    public static AaiComponents create() {

        // !!! Currently we'll just create empty components with schemas placeholder
        AaiComponents components = new Aai20Components();
        components.schemas = new HashMap<>(); // Currently we'll generate schemas with SchemaGenerator and append them later
        return components;
    }
}
