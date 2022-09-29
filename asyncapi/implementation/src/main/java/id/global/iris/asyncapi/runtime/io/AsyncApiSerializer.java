package id.global.iris.asyncapi.runtime.io;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import id.global.iris.asyncapi.runtime.json.IrisObjectMapper;
import io.apicurio.datamodels.asyncapi.models.AaiDocument;

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
    public static String serialize(AaiDocument asyncApi, Format format) throws IOException {
        try {
            ObjectMapper mapper;
            if (format == Format.JSON) {
                mapper = IrisObjectMapper.getObjectMapper();
                return mapper.writeValueAsString(asyncApi);
            } else {
                mapper = IrisObjectMapper.getYamlObjectMapper();
                return mapper.writer().writeValueAsString(asyncApi);
            }
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }
}
