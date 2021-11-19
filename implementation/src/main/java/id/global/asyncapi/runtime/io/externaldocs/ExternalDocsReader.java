package id.global.asyncapi.runtime.io.externaldocs;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import io.apicurio.datamodels.asyncapi.v2.models.Aai20ExternalDocumentation;
import io.apicurio.datamodels.core.models.common.ExternalDocumentation;
import id.global.asyncapi.runtime.util.JandexUtil;

/**
 * This reads annotations and json for External Documentation
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 * @author Eric Wittmann (eric.wittmann@gmail.com)
 * @see <a href=
 *      "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#externalDocumentationObject">externalDocumentationObject</a>
 */
public class ExternalDocsReader {

    private ExternalDocsReader() {
    }

    /**
     * Reads an ExternalDocumentation annotation.
     *
     * @param annotationValue the {@literal @}ExternalDocumentation annotation
     * @return ExternalDocumentation model
     */
    public static ExternalDocumentation readExternalDocs(final AnnotationValue annotationValue) {
        if (annotationValue == null) {
            return null;
        }
        return readExternalDocs(annotationValue.asNested());
    }

    /**
     * Reads an ExternalDocumentation annotation.
     *
     * @param annotationInstance the {@literal @}ExternalDocumentation annotation
     * @return ExternalDocumentation model
     */
    public static ExternalDocumentation readExternalDocs(AnnotationInstance annotationInstance) {
        if (annotationInstance == null) {
            return null;
        }
        ExternalDocumentation externalDoc = new Aai20ExternalDocumentation();

        externalDoc.description = JandexUtil.stringValue(annotationInstance, ExternalDocsConstant.PROP_DESCRIPTION);
        externalDoc.url = JandexUtil.stringValue(annotationInstance, ExternalDocsConstant.PROP_URL);
        return externalDoc;
    }
}
