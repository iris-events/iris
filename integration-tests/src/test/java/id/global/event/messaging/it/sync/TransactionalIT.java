package id.global.event.messaging.it.sync;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.common.headers.amqp.MessagingHeaders;
import id.global.event.messaging.it.IsolatedEventContextTest;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.exception.AmqpSendException;
import id.global.event.messaging.runtime.exception.AmqpTransactionException;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import id.global.event.messaging.runtime.tx.TransactionCallback;
import io.quarkus.test.junit.QuarkusTest;
import io.vavr.collection.Array;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransactionalIT extends IsolatedEventContextTest {
    private static final String EVENT_QUEUE = "test-eventqueue-transactional-it";
    private static final String EXCHANGE = "test-exchange-transactional-it";

    @Inject
    TransactionalService service;

    @BeforeEach
    public void setup() {
        service.reset();
    }

    @Test
    @DisplayName("id.global.event.messaging.runtime.producer.Message send")
    void testThroughServiceTransactionSuccessful() throws Exception {
        service.sendTransactional(false);

        service.getFutures().forEach(future -> {
            try {
                TestEventWrapper testEventWrapper = future.get(1000, TimeUnit.MILLISECONDS);
                TestEvent event = testEventWrapper.event;
                long timestamp = testEventWrapper.timestamp;
                assertThat(event, is(notNullValue()));
                assertThat(event, is(instanceOf(TestEvent.class)));
                assertThat(timestamp, is(greaterThan(0L)));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                fail(e);
            }
        });

        assertThat(service.getHandledEventCount(), is(4));
    }

    @Test
    @DisplayName("id.global.event.messaging.runtime.producer.Message send in rolled back should not reach handler service")
    void testTransactionRollback() {
        assertThrows(RuntimeException.class, () -> service.sendTransactional(true));
        assertThat(service.getHandledEventCount(), is(0));
    }

    @Test
    @DisplayName("Non transactional sends should not implicitly create a transaction")
    void testNonTransactional() throws Exception {
        long sendTimestamp0 = new Date().getTime();
        service.send(0);
        TestEventWrapper testEventWrapper0 = service.getFutures().get(0).get(1000, TimeUnit.MILLISECONDS);
        TestEvent testEvent0 = testEventWrapper0.event;
        long timestamp0 = testEventWrapper0.timestamp;
        assertEventNotNullWithSeq(testEvent0, 0);
        assertThat(timestamp0, is(greaterThanOrEqualTo(sendTimestamp0)));
        assertThat(timestamp0, is(lessThan(new Date().getTime())));

        long sendTimestamp1 = new Date().getTime();
        service.send(1);
        TestEventWrapper testEventWrapper1 = service.getFutures().get(1).get(1000, TimeUnit.MILLISECONDS);
        TestEvent testEvent1 = testEventWrapper1.event;
        long timestamp1 = testEventWrapper1.timestamp;
        assertEventNotNullWithSeq(testEvent1, 1);
        assertThat(timestamp1, is(greaterThanOrEqualTo(sendTimestamp1)));
        assertThat(timestamp1, is(lessThan(new Date().getTime())));

        service.send(2);
        TestEventWrapper testEventWrapper2 = service.getFutures().get(2).get(1000, TimeUnit.MILLISECONDS);
        TestEvent testEvent2 = testEventWrapper2.event;
        long timestamp2 = testEventWrapper2.timestamp;
        assertEventNotNullWithSeq(testEvent2, 2);
        assertThat(timestamp2, is(greaterThan(0L)));

        service.send(3);
        TestEventWrapper testEventWrapper3 = service.getFutures().get(3).get(1000, TimeUnit.MILLISECONDS);
        TestEvent testEvent3 = testEventWrapper3.event;
        long timestamp3 = testEventWrapper3.timestamp;
        assertEventNotNullWithSeq(testEvent3, 3);
        assertThat(timestamp3, is(greaterThan(0L)));

        assertThat(Array.of(timestamp0, timestamp1, timestamp2).contains(timestamp3), is(false));
        assertThat(Array.of(timestamp0, timestamp1, timestamp3).contains(timestamp2), is(false));
        assertThat(Array.of(timestamp0, timestamp2, timestamp3).contains(timestamp1), is(false));
        assertThat(Array.of(timestamp1, timestamp2, timestamp3).contains(timestamp0), is(false));
    }

    @Test
    @DisplayName("Transactional send with custom callback")
    void testCustomCallback() throws Exception {

        service.getProducer().registerTransactionCallback(new TestCustomTransactionCallback());
        service.sendTransactionalCustomCallback(false);

        Boolean isBeforeTxPublishTriggered = service.getBeforeTxPublishCallback().get(2000, TimeUnit.MILLISECONDS);
        Boolean isAfterTxPublishTriggered = service.getAfterTxPublishCallback().get(2000, TimeUnit.MILLISECONDS);
        Boolean isAfterTxCompleteTriggered = service.getAfterTxCompletionCallback().get(2000, TimeUnit.MILLISECONDS);

        assertThat(isBeforeTxPublishTriggered, is(true));
        assertThat(isAfterTxPublishTriggered, is(true));
        assertThat(isAfterTxCompleteTriggered, is(true));
    }

    @Test
    @DisplayName("Transaction rollback with custom callback")
    void testCustomCallbackRollback() throws Exception {

        service.getProducer().registerTransactionCallback(new TestCustomTransactionCallback());

        assertThrows(RuntimeException.class, () -> service.sendTransactionalCustomCallback(true));

        Boolean isBeforeTxPublishTriggered = service.getBeforeTxPublishCallback().get(1000, TimeUnit.MILLISECONDS);
        Boolean isAfterTxPublishTriggered = service.getAfterTxPublishCallback().get(1000, TimeUnit.MILLISECONDS);
        Boolean isAfterTxCompleteTriggered = service.getAfterTxCompletionCallback().get(1000, TimeUnit.MILLISECONDS);
        assertThat(isBeforeTxPublishTriggered, is(false));
        assertThat(isAfterTxPublishTriggered, is(false));
        assertThat(isAfterTxCompleteTriggered, is(true));
        assertThat(service.getHandledEventCount(), is(0));
    }

    @Test
    @DisplayName("Exception during transaction callback should produce AmqpTransactionRuntimeException")
    void testTransactionRuntimeException() {

        service.getProducer().registerTransactionCallback(new TransactionCallback() {
            @Override
            public void beforeTxPublish(List<id.global.event.messaging.runtime.producer.Message> messages)
                    throws AmqpSendException {
                throw new AmqpSendException("Test exception");
            }

            @Override
            public void afterTxPublish() {

            }

            @Override
            public void afterTxCompletion(List<id.global.event.messaging.runtime.producer.Message> messages, int status) {

            }
        });
        try {
            service.sendTransactionalCustomCallback(false);
        } catch (AmqpTransactionException | AmqpSendException e) {
            fail();
        } catch (RuntimeException runtimeException) {
            List<Throwable> causes = new ArrayList<>();
            Throwable cause = runtimeException.getCause();

            while (cause != null) {
                causes.add(cause);
                cause = cause.getCause();
            }

            Optional<Throwable> optionalThrowable = causes.stream().filter(c -> c instanceof AmqpTransactionException)
                    .findFirst();

            assertThat(optionalThrowable.isPresent(), is(true));
        }
    }

    @ApplicationScoped
    public static class TransactionalService {
        private final static int EXPECTED_MESSAGES = 4;
        @Inject
        EventContext eventContext;

        @Inject
        AmqpProducer producer;

        private List<CompletableFuture<TestEventWrapper>> futures = new ArrayList<>();

        private CompletableFuture<Boolean> beforeTxPublishCallback = new CompletableFuture<>();

        private CompletableFuture<Boolean> afterTxPublishCallback = new CompletableFuture<>();
        private CompletableFuture<Boolean> afterTxCompletionCallback = new CompletableFuture<>();

        public void reset() {
            futures = new ArrayList<>();
            for (int i = 0; i < EXPECTED_MESSAGES; i++) {
                futures.add(new CompletableFuture<>());
            }

            beforeTxPublishCallback = new CompletableFuture<>();
            afterTxPublishCallback = new CompletableFuture<>();
            afterTxCompletionCallback = new CompletableFuture<>();
            producer.registerTransactionCallback(null);
        }

        @SuppressWarnings("unused")
        @MessageHandler
        public void handle(TestEvent event) {
            long timestamp = (long) eventContext.getAmqpBasicProperties().getHeaders()
                    .get(MessagingHeaders.Message.SERVER_TIMESTAMP);
            futures.get(event.seq).complete(new TestEventWrapper(event, timestamp));
        }

        public List<CompletableFuture<TestEventWrapper>> getFutures() {
            return futures;
        }

        public int getHandledEventCount() {
            return futures.stream().filter(CompletableFuture::isDone).collect(Collectors.toSet()).size();
        }

        public void send(int i) throws AmqpSendException, AmqpTransactionException {
            producer.send(new TestEvent(i));
        }

        @Transactional
        public void sendTransactional(boolean throwException) throws AmqpSendException, AmqpTransactionException {
            for (int i = 0; i < EXPECTED_MESSAGES; i++) {
                producer.send(new TestEvent(i));
            }

            if (throwException) {
                throw new RuntimeException("Cancelling transactionRolledBack with exception");
            }
        }

        @Transactional
        public void sendTransactionalCustomCallback(boolean throwException) throws AmqpTransactionException, AmqpSendException {
            for (int i = 0; i < EXPECTED_MESSAGES; i++) {
                producer.send(new TestEvent(i));
            }

            if (throwException) {
                throw new RuntimeException("Cancelling transactionRolledBack with exception");
            }
        }

        public AmqpProducer getProducer() {
            return producer;
        }

        public CompletableFuture<Boolean> getBeforeTxPublishCallback() {
            return beforeTxPublishCallback;
        }

        public CompletableFuture<Boolean> getAfterTxPublishCallback() {
            return afterTxPublishCallback;
        }

        public CompletableFuture<Boolean> getAfterTxCompletionCallback() {
            return afterTxCompletionCallback;
        }

    }

    @Message(routingKey = EVENT_QUEUE, name = EXCHANGE, exchangeType = DIRECT)
    public record TestEvent(int seq) {
    }

    public record TestEventWrapper(TestEvent event, long timestamp) {
    }

    private class TestCustomTransactionCallback implements TransactionCallback {

        @Override
        public void beforeTxPublish(List<id.global.event.messaging.runtime.producer.Message> messages) {
            service.getBeforeTxPublishCallback().complete(true);
        }

        @Override
        public void afterTxPublish() {
            service.getAfterTxPublishCallback().complete(true);
        }

        @Override
        public void afterTxCompletion(List<id.global.event.messaging.runtime.producer.Message> messages, int status) {
            service.getAfterTxCompletionCallback().complete(true);
            if (!service.getBeforeTxPublishCallback().isDone()) {
                service.getBeforeTxPublishCallback().complete(false);
                service.getAfterTxPublishCallback().complete(false);
            }
        }

    }

    private void assertEventNotNullWithSeq(TestEvent event, int seq) {
        assertThat(event, is(notNullValue()));
        assertThat(event.seq, is(seq));
        assertThat(service.getHandledEventCount(), is(seq + 1));
    }
}
