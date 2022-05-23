package id.global.iris.asyncapi.runtime.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class SchemeIdGenerator {
    private static final String HOST = "id:global:";
    private static final String PROTOCOL = "urn";

    public static String buildId(String annotationValue) throws URISyntaxException {
        String id = Optional.ofNullable(annotationValue).map(s -> s.replace(" ", "-")).orElse("");
        URI uri = new URI(PROTOCOL, HOST + id, null);
        return uri.toString();
    }

}
