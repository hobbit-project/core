package org.hobbit.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.rabbit.RabbitMQUtils;

public class TestUtils {

    /**
     * Concatenates the given taskId and the given data in a static method to make
     * sure that everywhere in a test the same concatenation is used.
     * 
     * @param taskId
     * @param data
     * @return
     */
    public static String concat(String taskId, byte[] data) {
        return concat(taskId, 0, data, false, false);
    }

    /**
     * Concatenates the given taskId, data and timestamp in a static method to make
     * sure that everywhere in a test the same concatenation is used.
     * 
     * @param taskId
     * @param data
     * @param timestamp
     * @return
     */
    public static String concat(String taskId, byte[] data, long timestamp) {
        return concat(taskId, timestamp, data, true, true);
    }

    public static String concat(String taskId, long timestamp, byte[] data, boolean addTimeStamp, boolean addTaskId) {
        return concat(taskId, timestamp, new ByteArrayInputStream(data), addTimeStamp, addTaskId);
    }

    public static String concat(String taskId, long timestamp, InputStream stream, boolean addTimeStamp,
            boolean addTaskId) {
        StringBuilder builder = new StringBuilder();
        if (addTaskId) {
            builder.append(taskId);
            builder.append('|');
        }
        if (addTimeStamp) {
            builder.append(Long.toString(timestamp));
            builder.append('|');
        }
        try {
            builder.append(IOUtils.toString(stream, RabbitMQUtils.STRING_ENCODING));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

}
