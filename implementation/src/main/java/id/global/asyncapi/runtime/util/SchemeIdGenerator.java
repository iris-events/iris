package id.global.asyncapi.runtime.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;

public class SchemeIdGenerator {
    private static final String HOST = "id:global:";
    private static final String PROTOCOL = "urn";

    public static String buildId(String annotationValue) throws URISyntaxException {
        String id = Optional.ofNullable(annotationValue).orElse("");
        id = id.toLowerCase(Locale.ROOT).replace(" ", "-");

        //        URI uri = new URI(PROTOCOL, HOST, "/" + id, null);
        URI uri = new URI(PROTOCOL, HOST + id, null);
        return uri.toString();
    }

}
