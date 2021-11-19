package id.global.asyncapi.runtime.io;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.apicurio.datamodels.core.models.Document;
import io.apicurio.datamodels.core.models.Node;

public abstract class JsonMarshalMixin {
    @JsonIgnore
    public int _modelId;

    @JsonIgnore
    public Document _ownerDocument;

    @JsonIgnore
    public Node _parent;

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
