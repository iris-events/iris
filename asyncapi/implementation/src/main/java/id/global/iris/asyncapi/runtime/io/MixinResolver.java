package id.global.iris.asyncapi.runtime.io;

import com.fasterxml.jackson.databind.introspect.ClassIntrospector;

public class MixinResolver implements ClassIntrospector.MixInResolver {
    @Override
    public Class<?> findMixInClassFor(Class<?> aClass) {
        return JsonMarshalMixin.class;
    }

    @Override
    public ClassIntrospector.MixInResolver copy() {
        return new MixinResolver();
    }
}
