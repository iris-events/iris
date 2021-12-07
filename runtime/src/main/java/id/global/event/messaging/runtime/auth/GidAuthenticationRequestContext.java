package id.global.event.messaging.runtime.auth;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

public class GidAuthenticationRequestContext implements AuthenticationRequestContext {
    private static final Logger log = Logger.getLogger(GidAuthenticationRequestContext.class);

    private final Executor blockingExecutor = command -> ExecutorRecorder.getCurrent().execute(command);

    @Override
    public Uni<SecurityIdentity> runBlocking(Supplier<SecurityIdentity> function) {
        return Uni.createFrom().deferred(new Supplier<Uni<? extends SecurityIdentity>>() {
            @Override
            public Uni<SecurityIdentity> get() {
                if (BlockingOperationControl.isBlockingAllowed()) {
                    try {
                        SecurityIdentity result = function.get();
                        return Uni.createFrom().item(result);
                    } catch (Throwable t) {
                        return Uni.createFrom().failure(t);
                    }
                } else {
                    return Uni.createFrom().emitter(new Consumer<UniEmitter<? super SecurityIdentity>>() {
                        @Override
                        public void accept(UniEmitter<? super SecurityIdentity> uniEmitter) {
                            blockingExecutor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        uniEmitter.complete(function.get());
                                    } catch (Throwable t) {
                                        uniEmitter.fail(t);
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
    }
}
