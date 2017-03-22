package org.hobbit.core.components.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.core.components.AbstractEvaluationStorage;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.paired.PairedConsumerImpl;
import org.hobbit.core.rabbit.paired.PairedStreamHandler;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * This abstract class implements basic functions that can be used to implement
 * a task generator.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractStreamingEvaluationModule extends AbstractCommandReceivingComponent
        implements PairedStreamHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStreamingEvaluationModule.class);

    /**
     * Queue to the evaluation storage.
     */
    protected RabbitQueue evalModule2EvalStoreQueue;
    /**
     * The URI of the experiment.
     */
    protected String experimentUri;

    protected DataReceiverImpl receiver;

    protected byte iteratorId;

    protected Semaphore requestNextResultPair = new Semaphore(0);

    protected boolean iterationFinished;

    @Override
    public void init() throws Exception {
        super.init();

        Map<String, String> env = System.getenv();
        // Get the experiment URI
        if (env.containsKey(Constants.HOBBIT_EXPERIMENT_URI_KEY)) {
            experimentUri = env.get(Constants.HOBBIT_EXPERIMENT_URI_KEY);
        } else {
            String errorMsg = "Couldn't get the experiment URI from the variable " + Constants.HOBBIT_EXPERIMENT_URI_KEY
                    + ". Aborting.";
            LOGGER.error(errorMsg);
            throw new Exception(errorMsg);
        }

        evalModule2EvalStoreQueue = createDefaultRabbitQueue(
                generateSessionQueueName(Constants.EVAL_MODULE_2_EVAL_STORAGE_QUEUE_NAME));
        receiver = DataReceiverImpl.builder().consumerBuilder(PairedConsumerImpl.builder()).dataHandler(this)
                .maxParallelProcessedMsgs(1)
                .queue(this, generateSessionQueueName(Constants.EVAL_STORAGE_2_EVAL_MODULE_QUEUE_NAME)).build();
    }

    @Override
    public void run() throws Exception {
        sendToCmdQueue(Commands.EVAL_MODULE_READY_SIGNAL);
        collectResponses();
        Model model = summarizeEvaluation();
        LOGGER.info("The result model has " + model.size() + " triples.");
        sendResultModel(model);
    }

    /**
     * This method communicates with the evaluation storage to collect all
     * response pairs. For every pair the
     * {@link #evaluateResponse(byte[], byte[], long, long)} method is called.
     * 
     * @throws Exception
     *             if a communication error occurs.
     */
    protected void collectResponses() throws Exception {
        BasicProperties props;
        iteratorId = AbstractEvaluationStorage.NEW_ITERATOR_ID;
        byte requestBody[] = new byte[1];
        iterationFinished = false;

        while (true) {
            // request next response pair
            requestBody[0] = iteratorId;
            props = new BasicProperties.Builder().deliveryMode(2).replyTo(receiver.getQueue().name).build();
            evalModule2EvalStoreQueue.channel.basicPublish("", evalModule2EvalStoreQueue.name, props, requestBody);
            // wait for the response handler to finish
            requestNextResultPair.acquire();
            // check whether this one was the last
            if (iterationFinished) {
                receiver.closeWhenFinished();
                if (receiver.getErrorCount() > 0) {
                    throw new Exception("The underlying message queues encountered " + receiver.getErrorCount()
                            + " errors while iterating over the evaluation storage.");
                }
                return;
            }
        }
    }

    @Override
    public void handleIncomingStreams(String streamId, InputStream[] streams) {
        try {
            Objects.requireNonNull(streams, "Got null instead of a stream array.");
            if (streams.length == 0) {
                throw new IllegalArgumentException(
                        "Got an empty stream array while 1 or 3 streams have been expected.");
            }

            // If there is only one stream and it is empty this was the last
            // message
            if (streams.length == 1) {
                iterationFinished = true;
            } else {
                // Get the iterator ID
                int readByte;
                try {
                    readByte = streams[AbstractEvaluationStorage.ITERATOR_ID_STREAM_ID].read();
                    if (readByte < 0) {
                        LOGGER.error("Couldn't read the iterator id from the stream. The stream is empty.");
                    } else {
                        iteratorId = (byte) readByte;
                    }
                } catch (IOException e1) {
                    LOGGER.error("Couldn't read the iterator id from the stream.", e1);
                }
                if (streams.length > 3) {
                    LOGGER.warn(
                            "Got {} streams while 1 or 3 streams have been expected. The additional streams will be ignored.",
                            streams.length);
                }
                // read timestamps
                long taskSentTimestamp = 0;
                long responseReceivedTimestamp = 0;
                try {
                    if (streams[AbstractEvaluationStorage.EXPECTED_RESPONSE_STREAM_ID].available() > 0) {
                        taskSentTimestamp = RabbitMQUtils
                                .readLong(streams[AbstractEvaluationStorage.EXPECTED_RESPONSE_STREAM_ID]);
                    }
                    if (streams[AbstractEvaluationStorage.RECEIVED_RESPONSE_STREAM_ID].available() > 0) {
                        responseReceivedTimestamp = RabbitMQUtils
                                .readLong(streams[AbstractEvaluationStorage.RECEIVED_RESPONSE_STREAM_ID]);
                    }
                } catch (Exception e) {
                    LOGGER.error("Couldn't read timestamp.");
                }
                try {
                    evaluateResponse(streams[AbstractEvaluationStorage.EXPECTED_RESPONSE_STREAM_ID],
                            streams[AbstractEvaluationStorage.RECEIVED_RESPONSE_STREAM_ID], taskSentTimestamp,
                            responseReceivedTimestamp);
                } catch (Exception e) {
                    LOGGER.error("Got an exception while evaluating a response pair. Terminating the evaluation.", e);
                    System.exit(1);
                }
            }
        } finally {
            requestNextResultPair.release();
        }
    }

    /**
     * Evaluates the given response pair.
     * 
     * @param expectedData
     *            the data that has been expected
     * @param receivedData
     *            the data that has been received from the system
     * @param taskSentTimestamp
     *            the time at which the task has been sent to the system
     * @param responseReceivedTimestamp
     *            the time at which the response has been received from the
     *            system
     * @throws Exception
     *             if an error occurs during the evaluation
     */
    protected abstract void evaluateResponse(InputStream expectedData, InputStream receivedData, long taskSentTimestamp,
            long responseReceivedTimestamp) throws Exception;

    /**
     * Summarizes the evaluation and generates an RDF model containing the
     * evaluation results.
     * 
     * @return an RDF model containing the evaluation results
     * @throws Exception
     *             if a sever error occurs
     */
    protected abstract Model summarizeEvaluation() throws Exception;

    /**
     * Sends the model to the benchmark controller.
     * 
     * @param model
     *            the model that should be sent
     * @throws IOException
     *             if an error occurs during the commmunication
     */
    private void sendResultModel(Model model) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        model.write(outputStream, "JSONLD");
        sendToCmdQueue(Commands.EVAL_MODULE_FINISHED_SIGNAL, outputStream.toByteArray());
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        // Nothing to do
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(evalModule2EvalStoreQueue);
        receiver.close();
        super.close();
    }

    protected Model createDefaultModel() {
        Model resultModel = ModelFactory.createDefaultModel();
        resultModel.add(resultModel.createResource(experimentUri), RDF.type, HOBBIT.Experiment);
        return resultModel;
    }

    public void handleIncomingStream(String streamId, InputStream stream) {
        throw new IllegalStateException(
                "handleIncomingStream has been called while multiple streams have been expected by this component.");
    }
}
