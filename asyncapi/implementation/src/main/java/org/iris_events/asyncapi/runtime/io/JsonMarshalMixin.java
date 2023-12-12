package org.iris_events.asyncapi.runtime.io;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;

public abstract class JsonMarshalMixin {
    @JsonIgnore
    public int _modelId;

    // TODO this mapping was fixed blindly
    @JsonIgnore
    public AsyncApiDocument _ownerDocument;

    // TODO not sure what to map this to?
    //    @JsonIgnore
    //    public Node _parent;

    @JsonIgnore
    public String _name;

    @JsonIgnore
    public String _type;

    @JsonIgnore
    public boolean _isOneOfMessage;

    @JsonProperty("enum")
    public List<Object> enum_;

    @JsonIgnore
    public Boolean isExtensible() {
        return true;
    }

    @JsonIgnore
    public Boolean isAttached() {
        return true;
    }
}
