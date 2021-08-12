package io.smallrye.asyncapi.runtime.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;

public class InfoTitleGenerator {
    private static final String HOST = "global.id";
    private static final String PROTOCOL = "https";

    public static String buildTitle(String annotationValue) throws URISyntaxException {
        String title = Optional.ofNullable(annotationValue).orElse("");
        title = title.toLowerCase(Locale.ROOT).replace(" ", "-");

        URI uri = new URI(PROTOCOL, HOST, "/" + title, null);
        return uri.toString();
    }

}
