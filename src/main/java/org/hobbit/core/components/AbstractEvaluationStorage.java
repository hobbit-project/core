/**
 * This file is part of core.
 *
 * core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with core.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.core.components;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.data.Result;
import org.hobbit.core.data.ResultPair;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * This abstract class implements basic functions that can be used to implement
 * a task generator.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractEvaluationStorage extends AbstractCommandReceivingComponent
        implements ResponseReceivingComponent, ExpectedResponseReceivingComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEvaluationStorage.class);

    /**
     * If a request contains this iterator ID, a new iterator is created and its
     * first result as well as its Id are returned.
     */
    public static final byte NEW_ITERATOR_ID = -1;
    /**
     * The empty response that is sent if an error occurs.
     */
    private static final byte[] EMPTY_RESPONSE = new byte[0];

    /**
     * Mutex used to wait for the termination signal.
     */
    private Semaphore terminationMutex = new Semaphore(0);
    /**
     * Iterators that have been started.
     */
    protected List<Iterator<ResultPair>> resultPairIterators = Lists.newArrayList();
    /**
     * The incoming queue from the task generator.
     */
    protected RabbitQueue taskGen2EvalStoreQueue;
    /**
     * The incoming queue from the system.
     */
    protected RabbitQueue system2EvalStoreQueue;
    /**
     * The incoming queue from the evaluation module.
     */
    protected RabbitQueue evalModule2EvalStoreQueue;
    /**
     * Channel on which the acknowledgements are send.
     */
    protected Channel ackChannel = null;
    
    public AbstractEvaluationStorage() {
        defaultContainerType = Constants.CONTAINER_TYPE_DATABASE;
    }

    @Override
    public void init() throws Exception {
        super.init();

        @SuppressWarnings("resource")
        ExpectedResponseReceivingComponent expReceiver = this;
        taskGen2EvalStoreQueue = createDefaultRabbitQueue(
                generateSessionQueueName(Constants.TASK_GEN_2_EVAL_STORAGE_QUEUE_NAME));
        taskGen2EvalStoreQueue.channel.basicConsume(taskGen2EvalStoreQueue.name, true,
                new DefaultConsumer(taskGen2EvalStoreQueue.channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties,
                            byte[] body) throws IOException {
                        ByteBuffer buffer = ByteBuffer.wrap(body);
                        String taskId = RabbitMQUtils.readString(buffer);
                        byte[] data = RabbitMQUtils.readByteArray(buffer);
                        long timestamp = buffer.getLong();
                        expReceiver.receiveExpectedResponseData(taskId, timestamp, data);
                        // taskGen2EvalStoreQueue.channel.basicAck(envelope.getDeliveryTag(),
                        // false);
                    }
                });

        final String ackExchangeName = generateSessionQueueName(Constants.HOBBIT_ACK_EXCHANGE_NAME);
        @SuppressWarnings("resource")
        ResponseReceivingComponent respReceiver = this;
        system2EvalStoreQueue = createDefaultRabbitQueue(
                generateSessionQueueName(Constants.SYSTEM_2_EVAL_STORAGE_QUEUE_NAME));
        system2EvalStoreQueue.channel.basicConsume(system2EvalStoreQueue.name, true,
                new DefaultConsumer(system2EvalStoreQueue.channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties,
                            byte[] body) throws IOException {
                        ByteBuffer buffer = ByteBuffer.wrap(body);
                        String taskId = RabbitMQUtils.readString(buffer);
                        byte[] data = RabbitMQUtils.readByteArray(buffer);
                        respReceiver.receiveResponseData(taskId, System.currentTimeMillis(), data);
                        if (ackChannel != null) {
                            ackChannel.basicPublish(ackExchangeName, "", null, RabbitMQUtils.writeString(taskId));
                            LOGGER.debug("Sent ack{}.", taskId);
                        }
                    }
                });

        evalModule2EvalStoreQueue = createDefaultRabbitQueue(
                generateSessionQueueName(Constants.EVAL_MODULE_2_EVAL_STORAGE_QUEUE_NAME));
        evalModule2EvalStoreQueue.channel.basicConsume(evalModule2EvalStoreQueue.name, true,
                new DefaultConsumer(evalModule2EvalStoreQueue.channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties,
                            byte[] body) throws IOException {
                        byte response[] = null;
                        // get iterator id
                        ByteBuffer buffer = ByteBuffer.wrap(body);
                        if (buffer.remaining() < 1) {
                            response = EMPTY_RESPONSE;
                            LOGGER.error("Got a request without a valid iterator Id. Returning emtpy response.");
                        } else {
                            byte iteratorId = buffer.get();

                            // get the iterator
                            Iterator<ResultPair> iterator = null;
                            if (iteratorId == NEW_ITERATOR_ID) {
                                // create and save a new iterator
                                iteratorId = (byte) resultPairIterators.size();
                                LOGGER.info("Creating new iterator #{}", iteratorId);
                                resultPairIterators.add(iterator = createIterator());
                            } else if ((iteratorId < 0) || iteratorId >= resultPairIterators.size()) {
                                response = EMPTY_RESPONSE;
                                LOGGER.error("Got a request without a valid iterator Id (" + Byte.toString(iteratorId)
                                        + "). Returning emtpy response.");
                            } else {
                                iterator = resultPairIterators.get(iteratorId);
                            }
                            if ((iterator != null) && (iterator.hasNext())) {
                                ResultPair resultPair = iterator.next();
                                // set response (iteratorId,
                                // taskSentTimestamp, expectedData,
                                // responseReceivedTimestamp, receivedData)
                                Result expected = resultPair.getExpected();
                                Result actual = resultPair.getActual();

                                response = RabbitMQUtils
                                        .writeByteArrays(
                                                new byte[] {
                                                        iteratorId },
                                                new byte[][] {
                                                        expected != null
                                                                ? RabbitMQUtils.writeLong(expected.getSentTimestamp())
                                                                : new byte[0],
                                                        expected != null ? expected.getData() : new byte[0],
                                                        actual != null
                                                                ? RabbitMQUtils.writeLong(actual.getSentTimestamp())
                                                                : new byte[0],
                                                        actual != null ? actual.getData() : new byte[0] },
                                                null);
                            } else {
                                response = new byte[] { iteratorId };
                            }
                        }
                        getChannel().basicPublish("", properties.getReplyTo(), null, response);
                    }
                });

        boolean sendAcks = false;
        if (System.getenv().containsKey(Constants.ACKNOWLEDGEMENT_FLAG_KEY)) {
            sendAcks = Boolean.parseBoolean(System.getenv().getOrDefault(Constants.ACKNOWLEDGEMENT_FLAG_KEY, "false"));
            if (sendAcks) {
                // Create channel for acknowledgements
                ackChannel = connection.createChannel();
                String queueName = ackChannel.queueDeclare().getQueue();
                ackChannel.exchangeDeclare(generateSessionQueueName(Constants.HOBBIT_ACK_EXCHANGE_NAME), "fanout",
                        false, true, null);
                ackChannel.queueBind(queueName, generateSessionQueueName(Constants.HOBBIT_ACK_EXCHANGE_NAME), "");
            }
        }
    }

    /**
     * Creates a new iterator that iterates over the response pairs.
     * 
     * @return a new iterator or null if an error occurred
     */
    protected abstract Iterator<ResultPair> createIterator();

    @Override
    public void run() throws Exception {
        sendToCmdQueue(Commands.EVAL_STORAGE_READY_SIGNAL);
        terminationMutex.acquire();
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        // If this is the signal to start the data generation
        if (command == Commands.EVAL_STORAGE_TERMINATE) {
            // release the mutex
            terminationMutex.release();
        }
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(taskGen2EvalStoreQueue);
        IOUtils.closeQuietly(system2EvalStoreQueue);
        IOUtils.closeQuietly(evalModule2EvalStoreQueue);
        super.close();
    }
}
