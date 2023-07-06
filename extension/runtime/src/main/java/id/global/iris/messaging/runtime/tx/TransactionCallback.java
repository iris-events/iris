package id.global.iris.messaging.runtime.tx;

import java.util.List;

import id.global.iris.messaging.runtime.exception.IrisSendException;
import id.global.iris.messaging.runtime.producer.Message;

public interface TransactionCallback {
    /**
     * Executes before the transaction is committed.
     * After transaction will be completed Iris executes the publish method for all messages in the list that were enqueued in the transaction.
     *
     * @param messages List of messages to be published after
     */
    void beforeCompletion(final List<Message> messages);

    /**
     * Executes after a committed OR rolled back transaction.
     *
     * @param messages List of messages that was either published (transaction successfully committed) or ignored
     * @param messagesPublishedSuccessfully Indicates whether messages produced within this transaction were published successfully or not
     */
    void afterCompletion(final List<Message> messages, final int transactionStatus, final boolean messagesPublishedSuccessfully) throws IrisSendException;
}
