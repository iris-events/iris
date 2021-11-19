package id.global.event.messaging.it;

import io.smallrye.asyncapi.spec.annotations.EventApp;
import io.smallrye.asyncapi.spec.annotations.info.Info;

@SuppressWarnings("unused")
@EventApp(id = "TestEventApp", info = @Info(title = "Test dummy event app", description = "Test dummy event app"))
public class TestEventApp {
}
