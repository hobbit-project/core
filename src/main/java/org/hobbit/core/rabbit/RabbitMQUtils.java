package org.hobbit.core.rabbit;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.commons.io.Charsets;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains utility methods for working with RabbitMQ.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class RabbitMQUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQUtils.class);

    public static final Lang DEFAULT_RDF_LANG = Lang.JSONLD;
    public static final Charset STRING_ENCODING = Charsets.UTF_8;

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
            if (buffer.remaining() < Integer.BYTES) {
                return new byte[0];
            } else {
                int length = buffer.getInt();
                byte[] data = new byte[length];
                buffer.get(data, 0, data.length);
                return data;
            }
        }
    }

    /**
     * Reads a byte array from the given stream assuming that it is preceded by
     * an int value containing the length of the byte array.
     * 
     * @param stream
     *            the {@link InputStream} which provides {@link Integer#BYTES} =
     *            {@value Integer#BYTES} which are interpreted as the array
     *            length and additionally to that at least the number of bytes
     *            needed for the byte array
     * @throws IOException
     *             if an IO error occurs during reading from stream
     * @throws IllegalArgumentException
     *             if the stream does not provide enough bytes
     */
    public static byte[] readByteArray(InputStream stream) throws IOException, IllegalArgumentException {
        return readByteArray(stream, readInt(stream));
    }

    /**
     * Reads a byte array of the given size from the given stream.
     * 
     * @param stream
     *            the {@link InputStream} which provides at least the number of
     *            bytes needed for the byte array
     * @param size
     *            the number of bytes that should be read from the given stream
     * @throws IOException
     *             if an IO error occurs during reading from stream
     * @throws IllegalArgumentException
     *             if the stream does not provide enough bytes
     */
    public static byte[] readByteArray(InputStream stream, int size) throws IOException, IllegalArgumentException {
        // Read the first bytes containing the timestamp
        int pos = 0, length = 0;
        byte bytes[] = new byte[size];
        while ((length >= 0) && (pos < size)) {
            length = stream.read(bytes, pos, bytes.length - pos);
            pos += length;
        }
        // -1 comes from the last addition were length=-1
        if (pos < (size - 1)) {
            throw new IllegalArgumentException(
                    "The given input stream does not provide the expected " + size + " bytes.");
        }
        return bytes;
    }

    /**
     * Reads an RDF model from the given byte array.
     * 
     * @param data
     *            the byte array containing the serialized RDF model
     * @return the deserialized model
     */
    public static Model readModel(byte data[]) {
        return readModel(data, 0, data.length);
    }

    /**
     * Reads an RDF model from the given byte array.
     * 
     * @param data
     *            the byte array containing the serialized RDF model
     * @param offset
     *            position at which the parsing will start
     * @param length
     *            number of bytes that should be parsed
     * @return the deserialized model
     */
    public static Model readModel(byte data[], int offset, int length) {
        return readModel(readString(data, offset, length));
    }

    /**
     * Reads an RDF model from the given String.
     * 
     * @param string
     *            the String containing the serialized RDF model
     * @return the deserialized model
     */
    public static Model readModel(String string) {
        StringReader reader = new StringReader(string);
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, reader, "", DEFAULT_RDF_LANG);
        return model;
    }

    /**
     * Reads an RDF model from the given byte buffer assuming that the byte
     * array containing the model is preceded by an int containing its length.
     * 
     * @param buffer
     *            the buffer containing the length of the serialized model (as
     *            int) and the byte array containing the serialized model
     * @return the deserialized model
     */
    public static Model readModel(ByteBuffer buffer) {
        StringReader reader = new StringReader(readString(buffer));
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, reader, "", DEFAULT_RDF_LANG);
        return model;
    }

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
            return new String(data, STRING_ENCODING);
        }
    }

    /**
     * Transforms the given byte data into a String using the UTF-8 encoding.
     * 
     * @param data
     *            the byte array that should be transformed
     * @param offset
     *            position at which the parsing will start
     * @param length
     *            number of bytes that should be parsed
     * @return the String or null if the given byte array is null
     */
    public static String readString(byte[] data, int offset, int length) {
        if (data == null) {
            return null;
        } else {
            return new String(data, offset, length, STRING_ENCODING);
        }
    }

    /**
     * Reads a String from the given buffer assuming that it is preceded by an
     * int value containing the length of the String.
     * 
     * @param buffer
     *            the buffer that contains the byte data of the String
     * @return the String or null if the given buffer is null
     */
    public static String readString(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        } else {
            return new String(readByteArray(buffer), STRING_ENCODING);
        }
    }

    /**
     * writes the given byte arrays and puts their length in front of them.
     * Thus, they can be read using a ByteBuffer and the
     * {@link #readByteArray(ByteBuffer)} method.
     * 
     * @param arrays
     *            arrays that should be written to a single array
     * @return the byte array containing all given arrays and their lengths
     */
    public static byte[] writeByteArrays(byte[][] arrays) {
        return writeByteArrays(null, arrays, null);
    }

    /**
     * writes the given byte arrays and puts their length in front of them.
     * Thus, they can be read using a ByteBuffer and the
     * {@link #readByteArray(ByteBuffer)} method.
     * 
     * @param precedingData
     *            data that should be written before the arrays are written.
     *            <b>Note</b> that this data is not preceded with the byte array
     *            length.
     * @param arrays
     *            arrays that should be written to a single array
     * @param subsequentData
     *            data that should be written after the arrays are written.
     *            <b>Note</b> that this data is not preceded with the byte array
     *            length.
     * @return the byte array containing all given arrays and their lengths
     */
    public static byte[] writeByteArrays(byte[] precedingData, byte[][] arrays, byte[] subsequentData) {
        int length = arrays.length * 4;
        if (precedingData != null) {
            length += precedingData.length;
        }
        for (int i = 0; i < arrays.length; ++i) {
            length += arrays[i].length;
        }
        if (subsequentData != null) {
            length += subsequentData.length;
        }
        // write the data to the byte array
        ByteBuffer buffer = ByteBuffer.allocate(length);
        if (precedingData != null) {
            buffer.put(precedingData);
        }
        for (int i = 0; i < arrays.length; ++i) {
            buffer.putInt(arrays[i].length);
            buffer.put(arrays[i]);
        }
        if (subsequentData != null) {
            buffer.put(subsequentData);
        }
        return buffer.array();
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
        if (string == null) {
            return new byte[0];
        } else {
            return string.getBytes(STRING_ENCODING);
        }
    }

    /**
     * Creates a String containing the serialized RDF model using the
     * {@link RabbitMQUtils#DEFAULT_RDF_LANG}.
     * 
     * @param model
     *            the model that should be serialized
     * @return the String containing the model
     */
    public static String writeModel2String(Model model) {
        if (model == null) {
            return "";
        } else {
            StringWriter writer = new StringWriter();
            RDFDataMgr.write(writer, model, DEFAULT_RDF_LANG);
            return writer.toString();
        }
    }

    /**
     * Creates a byte array containing the serialized RDF model using the
     * {@link RabbitMQUtils#DEFAULT_RDF_LANG}.
     * 
     * @param model
     *            the model that should be serialized
     * @return the byte array containing the model
     */
    public static byte[] writeModel(Model model) {
        if (model == null) {
            return new byte[0];
        } else {
            return writeString(writeModel2String(model));
        }
    }

    /**
     * Creates a byte array representing the given long value.
     * 
     * @param value
     *            the value that should be serialized
     * @return the byte array containing the given long value
     */
    public static byte[] writeLong(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    /**
     * Reads a long value from the given byte array.
     * 
     * @param data
     *            a serialized long value
     * @return the value read from the array
     */
    public static long readLong(byte[] data) {
        return readLong(data, 0, data.length);
    }

    /**
     * Reads a long value from the given byte array.
     * 
     * @param data
     *            a serialized long value
     * @param offset
     *            position at which the parsing will start
     * @param length
     *            number of bytes that should be parsed (should equal
     *            {@link Long#BYTES})
     * @return the value read from the array
     */
    public static long readLong(byte[] data, int offset, int length) {
        if (length < Long.BYTES) {
            LOGGER.error("Cant read a long value from {} bytes. Returning 0.", length);
            return 0;
        }
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);
        return buffer.getLong();
    }

    /**
     * Reads a {@code long} value from the given {@link InputStream}.
     * 
     * @param stream
     *            the {@link InputStream} which provides the bytes of the
     *            {@code long} value
     * @return the {@code long} value that have been read from the stream
     * @throws IOException
     *             if an IO error occurs during reading from stream
     * @throws IllegalArgumentException
     *             if the stream does not provide enough bytes for a {@code long} value
     */
    public static long readLong(InputStream stream) throws IOException, IllegalArgumentException {
        return RabbitMQUtils.readLong(readByteArray(stream, Long.BYTES));
    }

    /**
     * Reads a {@code int} value from the given byte array.
     * 
     * @param data
     *            a serialized {@code int} value
     * @return the value read from the array
     */
    public static int readInt(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        return buffer.getInt();
    }

    /**
     * Reads an {@code int} value from the given {@link InputStream}.
     * 
     * @param stream
     *            the {@link InputStream} which provides the bytes of the
     *            {@code int} value
     * @return the {@code int} value that have been read from the stream
     * @throws IOException
     *             if an IO error occurs during reading from stream
     * @throws IllegalArgumentException
     *             if the stream does not provide enough bytes for a long value
     */
    public static int readInt(InputStream stream) throws IOException, IllegalArgumentException {
        return RabbitMQUtils.readInt(readByteArray(stream, Integer.BYTES));
    }
}
