package io.smallrye.asyncapi.runtime.io.tag;

import org.jboss.jandex.DotName;

import io.smallrye.asyncapi.spec.annotations.tags.Tag;
import io.smallrye.asyncapi.spec.annotations.tags.Tags;

/**
 * Constants related to Server
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 * @author Eric Wittmann (eric.wittmann@gmail.com)
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#tagObject">tagObject</a>
 */
public class TagConstant {
    static final DotName DOTNAME_TAG = DotName.createSimple(Tag.class.getName());
    public static final DotName DOTNAME_TAGS = DotName.createSimple(Tags.class.getName());

    public static final String PROP_NAME = "name";
    public static final String PROP_DESCRIPTION = "description";

    private TagConstant() {
    }
}
