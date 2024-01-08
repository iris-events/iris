package org.iris_events.it.rpc;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.iris_events.consumer.ConsumerContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ExtensionDisabledTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.iris.enabled", "false");

    @Inject
    Instance<ConsumerContainer> consumerContainers;

    @Test
    public void testConfig() {

        Assertions.assertTrue(consumerContainers.isUnsatisfied(), "Extension should not load any IRIS services");
    }

}
