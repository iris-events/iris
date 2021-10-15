package id.global.event.messaging.runtime.connection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.rabbitmq.client.Connection;

import io.quarkus.test.QuarkusUnitTest;

class ConsumerConnectionProviderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ConsumerConnectionProvider.class)
                    .addAsResource("application.properties"));

    @Inject
    Instance<ConsumerConnectionProvider> provider;

    @Test
    public void provideConnection() {
        Connection connection = provider.get().getConnection();
        assertThat(connection, is(notNullValue()));
    }
}
