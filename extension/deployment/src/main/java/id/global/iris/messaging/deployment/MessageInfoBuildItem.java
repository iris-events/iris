package id.global.iris.messaging.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import org.jboss.jandex.DotName;

public class MessageInfoBuildItem extends MultiBuildItem {
    // this might be better as Type, it's the same in MessageHandlerInfoBuildItem
    private final DotName annotatedClass;
    // we'll also need additional fields for declaring exchange in case of a produced event

    public MessageInfoBuildItem(DotName annotatedClass) {
        this.annotatedClass = annotatedClass;
    }

    public DotName getAnnotatedClass() {
        return annotatedClass;
    }
}
