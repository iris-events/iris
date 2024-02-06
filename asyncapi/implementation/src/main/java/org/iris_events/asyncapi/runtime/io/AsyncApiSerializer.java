package org.iris_events.asyncapi.runtime.io;

import java.io.IOException;

import org.iris_events.asyncapi.runtime.json.IrisObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Document;

/**
 * Class used to serialize an OpenAPI
 *
 * @author eric.wittmann@gmail.com
 */
@SuppressWarnings("unused")
public class AsyncApiSerializer {

    private AsyncApiSerializer() {
    }

    /**
     * Serializes the given OpenAPI object into either JSON or YAML and returns it as a string.
     *
     * @param asyncApi the AsyncAPI object
     * @param format the serialization format
     * @return OpenAPI object as a String
     * @throws IOException Errors in processing the JSON
     */
    public static String serialize(AsyncApi26Document asyncApi, Format format) throws IOException {
        try {
            ObjectMapper mapper;
            var schema = Library.writeDocument(asyncApi);
            if (format == Format.JSON) {
                mapper = IrisObjectMapper.getObjectMapper();
                return mapper.writeValueAsString(schema);
            } else {
                mapper = IrisObjectMapper.getYamlObjectMapper();
                return mapper.writer().writeValueAsString(schema);
            }
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }
}
