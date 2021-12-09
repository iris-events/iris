package id.global.event.messaging.runtime;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Command;
import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.ConsumerShutdownSignalCallback;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.Method;
import com.rabbitmq.client.ReturnCallback;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

public class TestChannel implements Channel {
    @Override
    public int getChannelNumber() {
        return 0;
    }

    @Override
    public Connection getConnection() {
        return null;
    }

    @Override
    public void close() throws IOException, TimeoutException {

    }

    @Override
    public void close(int closeCode, String closeMessage) throws IOException, TimeoutException {

    }

    @Override
    public void abort() throws IOException {

    }

    @Override
    public void abort(int closeCode, String closeMessage) throws IOException {

    }

    @Override
    public void addReturnListener(ReturnListener listener) {

    }

    @Override
    public ReturnListener addReturnListener(ReturnCallback returnCallback) {
        return null;
    }

    @Override
    public boolean removeReturnListener(ReturnListener listener) {
        return false;
    }

    @Override
    public void clearReturnListeners() {

    }

    @Override
    public void addConfirmListener(ConfirmListener listener) {

    }

    @Override
    public ConfirmListener addConfirmListener(ConfirmCallback ackCallback, ConfirmCallback nackCallback) {
        return null;
    }

    @Override
    public boolean removeConfirmListener(ConfirmListener listener) {
        return false;
    }

    @Override
    public void clearConfirmListeners() {

    }

    @Override
    public Consumer getDefaultConsumer() {
        return null;
    }

    @Override
    public void setDefaultConsumer(Consumer consumer) {

    }

    @Override
    public void basicQos(int prefetchSize, int prefetchCount, boolean global) throws IOException {

    }

    @Override
    public void basicQos(int prefetchCount, boolean global) throws IOException {

    }

    @Override
    public void basicQos(int prefetchCount) throws IOException {

    }

    @Override
    public void basicPublish(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body)
            throws IOException {

    }

    @Override
    public void basicPublish(String exchange, String routingKey, boolean mandatory, AMQP.BasicProperties props,
            byte[] body) throws IOException {

    }

    @Override
    public void basicPublish(String exchange, String routingKey, boolean mandatory, boolean immediate,
            AMQP.BasicProperties props, byte[] body) throws IOException {

    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String exchange, String type) throws IOException {
        return null;
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String exchange, BuiltinExchangeType type) throws IOException {
        return null;
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String exchange, String type, boolean durable) throws IOException {
        return null;
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String exchange, BuiltinExchangeType type, boolean durable)
            throws IOException {
        return null;
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String exchange, String type, boolean durable, boolean autoDelete,
            Map<String, Object> arguments) throws IOException {
        return null;
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String exchange, BuiltinExchangeType type, boolean durable,
            boolean autoDelete, Map<String, Object> arguments) throws IOException {
        return null;
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String exchange, String type, boolean durable, boolean autoDelete,
            boolean internal, Map<String, Object> arguments) throws IOException {
        return null;
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String exchange, BuiltinExchangeType type, boolean durable,
            boolean autoDelete, boolean internal, Map<String, Object> arguments) throws IOException {
        return null;
    }

    @Override
    public void exchangeDeclareNoWait(String exchange, String type, boolean durable, boolean autoDelete,
            boolean internal, Map<String, Object> arguments) throws IOException {

    }

    @Override
    public void exchangeDeclareNoWait(String exchange, BuiltinExchangeType type, boolean durable, boolean autoDelete,
            boolean internal, Map<String, Object> arguments) throws IOException {

    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclarePassive(String name) throws IOException {
        return null;
    }

    @Override
    public AMQP.Exchange.DeleteOk exchangeDelete(String exchange, boolean ifUnused) throws IOException {
        return null;
    }

    @Override
    public void exchangeDeleteNoWait(String exchange, boolean ifUnused) throws IOException {

    }

    @Override
    public AMQP.Exchange.DeleteOk exchangeDelete(String exchange) throws IOException {
        return null;
    }

    @Override
    public AMQP.Exchange.BindOk exchangeBind(String destination, String source, String routingKey)
            throws IOException {
        return null;
    }

    @Override
    public AMQP.Exchange.BindOk exchangeBind(String destination, String source, String routingKey,
            Map<String, Object> arguments) throws IOException {
        return null;
    }

    @Override
    public void exchangeBindNoWait(String destination, String source, String routingKey,
            Map<String, Object> arguments) throws IOException {

    }

    @Override
    public AMQP.Exchange.UnbindOk exchangeUnbind(String destination, String source, String routingKey)
            throws IOException {
        return null;
    }

    @Override
    public AMQP.Exchange.UnbindOk exchangeUnbind(String destination, String source, String routingKey,
            Map<String, Object> arguments) throws IOException {
        return null;
    }

    @Override
    public void exchangeUnbindNoWait(String destination, String source, String routingKey,
            Map<String, Object> arguments) throws IOException {

    }

    @Override
    public AMQP.Queue.DeclareOk queueDeclare() throws IOException {
        return null;
    }

    @Override
    public AMQP.Queue.DeclareOk queueDeclare(String queue, boolean durable, boolean exclusive, boolean autoDelete,
            Map<String, Object> arguments) throws IOException {
        return null;
    }

    @Override
    public void queueDeclareNoWait(String queue, boolean durable, boolean exclusive, boolean autoDelete,
            Map<String, Object> arguments) throws IOException {

    }

    @Override
    public AMQP.Queue.DeclareOk queueDeclarePassive(String queue) throws IOException {
        return null;
    }

    @Override
    public AMQP.Queue.DeleteOk queueDelete(String queue) throws IOException {
        return null;
    }

    @Override
    public AMQP.Queue.DeleteOk queueDelete(String queue, boolean ifUnused, boolean ifEmpty) throws IOException {
        return null;
    }

    @Override
    public void queueDeleteNoWait(String queue, boolean ifUnused, boolean ifEmpty) throws IOException {

    }

    @Override
    public AMQP.Queue.BindOk queueBind(String queue, String exchange, String routingKey) throws IOException {
        return null;
    }

    @Override
    public AMQP.Queue.BindOk queueBind(String queue, String exchange, String routingKey,
            Map<String, Object> arguments) throws IOException {
        return null;
    }

    @Override
    public void queueBindNoWait(String queue, String exchange, String routingKey, Map<String, Object> arguments)
            throws IOException {

    }

    @Override
    public AMQP.Queue.UnbindOk queueUnbind(String queue, String exchange, String routingKey) throws IOException {
        return null;
    }

    @Override
    public AMQP.Queue.UnbindOk queueUnbind(String queue, String exchange, String routingKey,
            Map<String, Object> arguments) throws IOException {
        return null;
    }

    @Override
    public AMQP.Queue.PurgeOk queuePurge(String queue) throws IOException {
        return null;
    }

    @Override
    public GetResponse basicGet(String queue, boolean autoAck) throws IOException {
        return null;
    }

    @Override
    public void basicAck(long deliveryTag, boolean multiple) throws IOException {

    }

    @Override
    public void basicNack(long deliveryTag, boolean multiple, boolean requeue) throws IOException {

    }

    @Override
    public void basicReject(long deliveryTag, boolean requeue) throws IOException {

    }

    @Override
    public String basicConsume(String queue, Consumer callback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, DeliverCallback deliverCallback, CancelCallback cancelCallback)
            throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, DeliverCallback deliverCallback,
            ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, DeliverCallback deliverCallback, CancelCallback cancelCallback,
            ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, Consumer callback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, DeliverCallback deliverCallback,
            CancelCallback cancelCallback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, DeliverCallback deliverCallback,
            ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, DeliverCallback deliverCallback,
            CancelCallback cancelCallback, ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, Map<String, Object> arguments, Consumer callback)
            throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, Map<String, Object> arguments,
            DeliverCallback deliverCallback, CancelCallback cancelCallback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, Map<String, Object> arguments,
            DeliverCallback deliverCallback, ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, Map<String, Object> arguments,
            DeliverCallback deliverCallback, CancelCallback cancelCallback,
            ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, String consumerTag, Consumer callback)
            throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, String consumerTag, DeliverCallback deliverCallback,
            CancelCallback cancelCallback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, String consumerTag, DeliverCallback deliverCallback,
            ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, String consumerTag, DeliverCallback deliverCallback,
            CancelCallback cancelCallback, ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, String consumerTag, boolean noLocal, boolean exclusive,
            Map<String, Object> arguments, Consumer callback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, String consumerTag, boolean noLocal, boolean exclusive,
            Map<String, Object> arguments, DeliverCallback deliverCallback, CancelCallback cancelCallback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, String consumerTag, boolean noLocal, boolean exclusive,
            Map<String, Object> arguments, DeliverCallback deliverCallback,
            ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException {
        return null;
    }

    @Override
    public String basicConsume(String queue, boolean autoAck, String consumerTag, boolean noLocal, boolean exclusive,
            Map<String, Object> arguments, DeliverCallback deliverCallback, CancelCallback cancelCallback,
            ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException {
        return null;
    }

    @Override
    public void basicCancel(String consumerTag) throws IOException {

    }

    @Override
    public AMQP.Basic.RecoverOk basicRecover() throws IOException {
        return null;
    }

    @Override
    public AMQP.Basic.RecoverOk basicRecover(boolean requeue) throws IOException {
        return null;
    }

    @Override
    public AMQP.Tx.SelectOk txSelect() throws IOException {
        return null;
    }

    @Override
    public AMQP.Tx.CommitOk txCommit() throws IOException {
        return null;
    }

    @Override
    public AMQP.Tx.RollbackOk txRollback() throws IOException {
        return null;
    }

    @Override
    public AMQP.Confirm.SelectOk confirmSelect() throws IOException {
        return null;
    }

    @Override
    public long getNextPublishSeqNo() {
        return 0;
    }

    @Override
    public boolean waitForConfirms() throws InterruptedException {
        return false;
    }

    @Override
    public boolean waitForConfirms(long timeout) throws InterruptedException, TimeoutException {
        return false;
    }

    @Override
    public void waitForConfirmsOrDie() throws IOException, InterruptedException {

    }

    @Override
    public void waitForConfirmsOrDie(long timeout) throws IOException, InterruptedException, TimeoutException {

    }

    @Override
    public void asyncRpc(Method method) throws IOException {

    }

    @Override
    public Command rpc(Method method) throws IOException {
        return null;
    }

    @Override
    public long messageCount(String queue) throws IOException {
        return 0;
    }

    @Override
    public long consumerCount(String queue) throws IOException {
        return 0;
    }

    @Override
    public CompletableFuture<Command> asyncCompletableRpc(Method method) throws IOException {
        return null;
    }

    @Override
    public void addShutdownListener(ShutdownListener listener) {

    }

    @Override
    public void removeShutdownListener(ShutdownListener listener) {

    }

    @Override
    public ShutdownSignalException getCloseReason() {
        return null;
    }

    @Override
    public void notifyListeners() {

    }

    @Override
    public boolean isOpen() {
        return false;
    }
}
