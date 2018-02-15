package org.hobbit.core.components.stream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractPlatformConnectorComponent;
import org.hobbit.core.components.TaskReceivingComponent;
import org.hobbit.core.rabbit.DataReceiver;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.core.rabbit.DataSender;
import org.hobbit.core.rabbit.DataSenderImpl;
import org.hobbit.core.rabbit.IncomingStreamHandler;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class implements basic functions that can be used to implement
 * a system adapter.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractStreamingSystemAdapter extends AbstractPlatformConnectorComponent
        implements StreamingGeneratedDataReceivingComponent, TaskReceivingComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStreamingSystemAdapter.class);

    /**
     * Default value of the {@link #maxParallelProcessedMsgs} attribute.
     */
    private static final int DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES = 100;

    /**
     * Mutex used to wait for the terminate signal.
     */
    private Semaphore terminateMutex = new Semaphore(0);
    /**
     * The maximum number of incoming messages that are processed in parallel.
     * Additional messages have to wait.
     */
    private final int maxParallelProcessedMsgs;
    /**
     * The RDF model containing the system parameters.
     */
    protected Model systemParamModel;

    protected DataSender sender2EvalStore;
    protected DataReceiver dataReceiver;
    protected DataReceiver taskReceiver;
    /**
     * The cause for an unusual termination.
     */
    private Exception cause;
    /**
     * Mutex used to manage access to the {@link #cause} object.
     */
    private Semaphore causeMutex = new Semaphore(1);

    public AbstractStreamingSystemAdapter() {
        this(DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES);
    }

    public AbstractStreamingSystemAdapter(int maxParallelProcessedMsgs) {
        this.maxParallelProcessedMsgs = maxParallelProcessedMsgs;
        defaultContainerType = Constants.CONTAINER_TYPE_SYSTEM;
    }

    public AbstractStreamingSystemAdapter(DataSender sender2EvalStore, DataReceiver dataReceiver,
            DataReceiver taskReceiver) {
        this.sender2EvalStore = sender2EvalStore;
        this.dataReceiver = dataReceiver;
        this.taskReceiver = taskReceiver;
        defaultContainerType = Constants.CONTAINER_TYPE_SYSTEM;
        maxParallelProcessedMsgs = DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES;
    }

    @Override
    public void init() throws Exception {
        super.init();

        Map<String, String> env = System.getenv();
        // Get the benchmark parameter model
        if (env.containsKey(Constants.SYSTEM_PARAMETERS_MODEL_KEY)) {
            try {
                systemParamModel = RabbitMQUtils.readModel(env.get(Constants.SYSTEM_PARAMETERS_MODEL_KEY));
            } catch (Exception e) {
                LOGGER.warn("Couldn't deserialize the given parameter model. The parameter model will be empty.", e);
                systemParamModel = ModelFactory.createDefaultModel();
            }
        } else {
            LOGGER.warn("Couldn't get the expected parameter model from the variable "
                    + Constants.SYSTEM_PARAMETERS_MODEL_KEY + ". The parameter model will be empty.");
            systemParamModel = ModelFactory.createDefaultModel();
        }

        if (sender2EvalStore == null) {
            // We don't need to define an id generator since we will set the IDs
            // while sending data
            sender2EvalStore = DataSenderImpl.builder()
                    .queue(getFactoryForOutgoingDataQueues(), generateSessionQueueName(Constants.SYSTEM_2_EVAL_STORAGE_DEFAULT_QUEUE_NAME)).build();
        }

        if (maxParallelProcessedMsgs > 0) {
            if (dataReceiver == null) {
                dataReceiver = DataReceiverImpl.builder().dataHandler(new GeneratedDataHandler())
                        .maxParallelProcessedMsgs(maxParallelProcessedMsgs)
                        .queue(getFactoryForIncomingDataQueues(), generateSessionQueueName(Constants.DATA_GEN_2_SYSTEM_QUEUE_NAME)).build();
            } else {
                // XXX here we could set the data handler if the data receiver
                // would
                // offer such a method
            }
            if (taskReceiver == null) {
                taskReceiver = DataReceiverImpl.builder().dataHandler(new GeneratedTaskHandler())
                        .maxParallelProcessedMsgs(maxParallelProcessedMsgs)
                        .queue(getFactoryForIncomingDataQueues(), generateSessionQueueName(Constants.TASK_GEN_2_SYSTEM_QUEUE_NAME)).build();
            } else {
                // XXX here we could set the data handler if the data receiver
                // would
                // offer such a method
            }
        } else {
            throw new IllegalArgumentException("The maximum number of messages processed in parallel has to be > 0.");
        }
    }

    @Override
    public void run() throws Exception {
        sendToCmdQueue(Commands.SYSTEM_READY_SIGNAL);

        terminateMutex.acquire();
        // wait until all messages have been read from the queue
        dataReceiver.closeWhenFinished();
        taskReceiver.closeWhenFinished();
        sender2EvalStore.closeWhenFinished();
        // Check whether the system should abort
        try {
            causeMutex.acquire();
            if (cause != null) {
                throw cause;
            }
            causeMutex.release();
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting to set the termination cause.");
        }
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        // If this is the signal to start the data generation
        if (command == Commands.TASK_GENERATION_FINISHED) {
            terminate(null);
        }
        super.receiveCommand(command, data);
    }

    /**
     * Starts termination of the main thread of this system adapter. If a cause
     * is given, it will be thrown causing an abortion from the main thread
     * instead of a normal termination.
     * 
     * @param cause
     *            the cause for an abortion of the process or {code null} if the
     *            component should terminate in a normal way.
     */
    protected synchronized void terminate(Exception cause) {
        if (cause != null) {
            try {
                causeMutex.acquire();
                this.cause = cause;
                causeMutex.release();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting to set the termination cause.");
            }
        }
        terminateMutex.release();
    }

    /**
     * This method sends the given result data for the task with the given task
     * id to the evaluation storage.
     * 
     * @param taskIdString
     *            the id of the task
     * @param data
     *            the data of the task
     * @throws IOException
     *             if there is an error during the sending
     */
    protected void sendResultToEvalStorage(String taskIdString, InputStream dataStream) throws IOException {
        InputStream precedingStream = new ByteArrayInputStream(
                RabbitMQUtils.writeByteArrays(new byte[][] { RabbitMQUtils.writeString(taskIdString) }));
        SequenceInputStream concatenatedStreams = new SequenceInputStream(precedingStream, dataStream);
        sender2EvalStore.sendData(concatenatedStreams, taskIdString);
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(dataReceiver);
        IOUtils.closeQuietly(taskReceiver);
        IOUtils.closeQuietly(sender2EvalStore);
        super.close();
    }

    /**
     * A simple internal handler class that calls
     * {@link AbstractStreamingSystemAdapter#receiveGeneratedData(InputStream)}
     * with the given {@link InputStream}.
     * 
     * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
     *
     */
    protected class GeneratedDataHandler implements IncomingStreamHandler {

        @Override
        public void handleIncomingStream(String streamId, InputStream stream) {
            receiveGeneratedData(stream);
        }
    }

    /**
     * A simple internal handler class that calls
     * {@link AbstractStreamingTaskGenerator#generateTask(InputStream)} with the
     * given {@link InputStream}.
     * 
     * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
     *
     */
    protected class GeneratedTaskHandler implements IncomingStreamHandler {

        @Override
        public void handleIncomingStream(String streamId, InputStream stream) {
            try {
                String taskId;
                /*
                 * Check whether this is the old format (backwards compatibility
                 * to version 1.0.0 in which the data is preceded by its length)
                 */
                if (streamId == null) {
                    // get taskId/streamId and timestamp
                    ByteBuffer buffer;
                    buffer = ByteBuffer.wrap(IOUtils.toByteArray(stream));
                    taskId = RabbitMQUtils.readString(buffer);
                    byte[] data = RabbitMQUtils.readByteArray(buffer);
                    IOUtils.closeQuietly(stream);
                    // create a new stream containing only the data
                    stream = new ByteArrayInputStream(data);
                } else {
                    // get taskId
                    int length = RabbitMQUtils.readInt(stream);
                    taskId = RabbitMQUtils.readString(RabbitMQUtils.readByteArray(stream, length));
                }
                receiveGeneratedTask(taskId, stream);
            } catch (Exception e) {
                LOGGER.error("Exception while handling generated task. It will be ignored.", e);
            }
        }
    }
}
