package org.hobbit.core.rabbit;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;

import org.apache.commons.io.Charsets;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/**
 * Contains utility methods for working with RabbitMQ.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class RabbitMQUtils {

    public static final Lang DEFAULT_RDF_LANG = Lang.JSONLD;

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
        StringReader reader = new StringReader(readString(data, offset, length));
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
            return new String(data, Charsets.UTF_8);
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
            return new String(data, offset, length, Charsets.UTF_8);
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
            return new String(readByteArray(buffer), Charsets.UTF_8);
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
        if (string == null) {
            return new byte[0];
        } else {
            return string.getBytes(Charsets.UTF_8);
        }
    }

    /**
     * Creates a byte array containing the serialized RDF model.
     * 
     * @param model
     *            the model that should be serialized
     * @return the byte array containing the model
     */
    public static byte[] writeModel(Model model) {
        if (model == null) {
            return new byte[0];
        } else {
            StringWriter writer = new StringWriter();
            RDFDataMgr.write(writer, model, DEFAULT_RDF_LANG);
            return writeString(writer.toString());
        }
    }
}
