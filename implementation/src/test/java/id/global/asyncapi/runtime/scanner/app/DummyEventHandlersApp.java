package id.global.asyncapi.runtime.scanner.app;

import id.global.asyncapi.spec.annotations.EventApp;
import id.global.asyncapi.spec.annotations.info.Info;

@EventApp(id = EventHandlersApp.ID, info = @Info(title = EventHandlersApp.TITLE, version = EventHandlersApp.VERSION))
public class DummyEventHandlersApp {
}
