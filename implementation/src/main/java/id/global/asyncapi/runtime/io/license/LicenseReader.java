package id.global.asyncapi.runtime.io.license;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import io.apicurio.datamodels.asyncapi.v2.models.Aai20License;
import io.apicurio.datamodels.core.models.common.License;
import id.global.asyncapi.runtime.util.JandexUtil;

/**
 * This reads the License from annotations or json
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 * @author Eric Wittmann (eric.wittmann@gmail.com)
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#licenseObject">licenseObject</a>
 */
public class LicenseReader {

    private LicenseReader() {
    }

    /**
     * Reads an License annotation.
     *
     * @param annotationValue the {@literal @}License annotation
     * @return License model
     */
    public static License readLicense(final AnnotationValue annotationValue) {
        if (annotationValue == null) {
            return null;
        }
        AnnotationInstance nested = annotationValue.asNested();
        License license = new Aai20License();
        license.name = JandexUtil.stringValue(nested, LicenseConstant.PROP_NAME);
        license.url = JandexUtil.stringValue(nested, LicenseConstant.PROP_URL);
        return license;
    }
}
