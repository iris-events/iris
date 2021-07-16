package io.smallrye.asyncapi.runtime.io.info;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import io.apicurio.datamodels.asyncapi.v2.models.Aai20Info;
import io.apicurio.datamodels.core.models.common.Info;
import io.smallrye.asyncapi.runtime.io.contact.ContactReader;
import io.smallrye.asyncapi.runtime.io.license.LicenseReader;
import io.smallrye.asyncapi.runtime.util.JandexUtil;

/**
 * This reads the Info from annotations or json
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 * @author Eric Wittmann (eric.wittmann@gmail.com)
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#infoObject">infoObject</a>
 */
public class InfoReader {

    public static final String PROP_TITLE = "title";
    public static final String PROP_DESCRIPTION = "description";
    public static final String PROP_TERMS_OF_SERVICE = "termsOfService";

    private InfoReader() {
    }

    /**
     * Annotation to Info
     *
     * @param annotationValue the {@literal @}Info annotation
     * @return Info model
     */
    public static Info readInfo(final AnnotationValue annotationValue) {
        if (annotationValue == null) {
            return null;
        }
        AnnotationInstance nested = annotationValue.asNested();

        Info info = new Aai20Info();

        info.title = JandexUtil.stringValue(nested, PROP_TITLE);
        info.description = JandexUtil.stringValue(nested, PROP_DESCRIPTION);
        info.termsOfService = JandexUtil.stringValue(nested, PROP_TERMS_OF_SERVICE);
        info.contact = ContactReader.readContact(nested.value(InfoConstant.PROP_CONTACT));
        info.license = LicenseReader.readLicense(nested.value(InfoConstant.PROP_LICENSE));
        info.version = JandexUtil.stringValue(nested, "version");

        return info;
    }
}
