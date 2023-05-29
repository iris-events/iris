package org.iris_events.asyncapi.runtime.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class StreamUtil {
    public static String toString(InputStream stream) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            for (int length; (length = stream.read(buffer)) != -1;) {
                result.write(buffer, 0, length);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not convert InputStream to String", e);
        }
        return result.toString(StandardCharsets.UTF_8);
    }
}
