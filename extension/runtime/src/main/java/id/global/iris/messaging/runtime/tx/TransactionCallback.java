package id.global.iris.messaging.runtime.tx;

import java.util.List;

import id.global.iris.messaging.runtime.exception.IrisSendException;
import id.global.iris.messaging.runtime.producer.Message;

public interface TransactionCallback {
    /**
     * Executes before the transaction is committed. After this method the IrisProducer executes the publish method for all
     * messages in the list that were enqueued in the transaction.
     *
     * @param messages List of messages to be published
     */
    void beforeTxPublish(List<Message> messages) throws IrisSendException;

    /**
     * Executes after the IrisProducer executes publish for all messages in a successfully committed transaction.
     */
    void afterTxPublish() throws IrisSendException;

    /**
     * Executes after a committed OR rolled back transaction.
     *
     * @param messages List of messages that was either published or ignored
     */
    void afterTxCompletion(List<Message> messages, int status);
}
