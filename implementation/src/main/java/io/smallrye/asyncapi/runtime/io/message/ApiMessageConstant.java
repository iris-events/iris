package io.smallrye.asyncapi.runtime.io.message;

import org.jboss.jandex.DotName;

import io.smallrye.asyncapi.spec.annotations.components.Message;

public class ApiMessageConstant {
    public static final DotName DOTNAME_MESSAGE = DotName.createSimple(Message.class.getName());

    public static final String PROP_NAME = "name";
    public static final String PROP_CONTENT_TYPE = "contentType";
    public static final String PROP_TITLE = "title";
    public static final String PROP_SUMMARY = "summary";
    public static final String PROP_DESCRIPTION = "description";
    public static final String PROP_PAYLOAD = "payload";
    public static final String PROP_IMPLEMENTATION = "implementation";
}
