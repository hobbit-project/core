package org.hobbit.core.rabbit;

import java.nio.ByteBuffer;

import org.apache.commons.io.Charsets;

/**
 * Contains utility methods for working with RabbitMQ.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class RabbitMQUtils {

    /**
     * Transforms the given byte data into a String using the UTF-8 encoding.
     * 
     * @param data
     *            the byte array that should be transformed
     * @return the String or null if the given byte array is null
     */
    public static String readString(byte[] data) {
        if (data == null) {
            return null;
        } else {
            return new String(data, Charsets.UTF_8);
        }
    }

    /**
     * Transforms the given buffer data into a String using the UTF-8 encoding.
     * 
     * @param buffer
     *            the buffer that contains the byte data of the String
     * @return the String or null if the given buffer is null
     */
    public static String readString(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        } else {
            return new String(readByteArray(buffer), Charsets.UTF_8);
        }
    }

    /**
     * Reads a byte array from the given buffer assuming that it is preceded by
     * an int value containing the length of the byte array.
     * 
     * @param buffer
     *            the buffer containing an int containing the length of the byte
     *            array followed by the byte array itself
     * @return the byte array or null if the given byte array is null
     */
    public static byte[] readByteArray(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        } else {
            int length = buffer.getInt();
            byte[] data = new byte[length];
            buffer.get(data, 0, data.length);
            return data;
        }
    }

    /**
     * Creates a byte array representation of the given String using UTF-8
     * encoding.
     * 
     * @param string
     *            the String that should be transformed into a byte array
     * @return the UTF-8 byte array of the given String
     */
    public static byte[] writeString(String string) {
        return string.getBytes(Charsets.UTF_8);
    }
}
