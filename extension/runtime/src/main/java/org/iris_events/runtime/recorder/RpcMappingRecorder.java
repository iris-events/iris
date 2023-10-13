package org.iris_events.runtime.recorder;

import java.util.Map;

import org.iris_events.runtime.RpcMappingProvider;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RpcMappingRecorder {
    public void registerRpcMappings(final BeanContainer beanContainer, final Map<String, String> dotNameToRpcNameMapping) {
        beanContainer.beanInstance(RpcMappingProvider.class).addReplyToMappings(dotNameToRpcNameMapping);
    }
}
