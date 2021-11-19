package id.global.asyncapi.runtime.io.contact;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import io.apicurio.datamodels.asyncapi.v2.models.Aai20Contact;
import io.apicurio.datamodels.core.models.common.Contact;
import id.global.asyncapi.runtime.util.JandexUtil;

/**
 * This reads the Contact from annotations or json
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 * @author Eric Wittmann (eric.wittmann@gmail.com)
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#contactObject">contactObject</a>
 */
public class ContactReader {

    private ContactReader() {
    }

    /**
     * Reads an Contact annotation.
     *
     * @param annotationValue the {@literal @}Contact annotation
     * @return Contact model
     */
    public static Contact readContact(final AnnotationValue annotationValue) {
        if (annotationValue == null) {
            return null;
        }
        AnnotationInstance nested = annotationValue.asNested();
        Contact contact = new Aai20Contact();

        contact.name = JandexUtil.stringValue(nested, ContactConstant.PROP_NAME);
        contact.url = JandexUtil.stringValue(nested, ContactConstant.PROP_URL);
        contact.email = JandexUtil.stringValue(nested, ContactConstant.PROP_EMAIL);

        return contact;
    }
}
