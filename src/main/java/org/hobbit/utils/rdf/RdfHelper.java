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
package org.hobbit.utils.rdf;

import java.util.stream.Collectors;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.ext.com.google.common.collect.Streams;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDFS;
import org.hobbit.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements simple methods to get data from a given RDF Model.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class RdfHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdfHelper.class);

    /**
     * Replacement URI for the resource.
     * Currently has no effect on actual hash computation.
     */
    public static String HASH_SELF_URI = "hashedRDF.v1:self";

    /**
     * Returns the label of the given {@link Resource} if it is present in the given
     * {@link Model}.
     *
     * @param model
     *            the model that should contain the label
     * @param resource
     *            the resource for which the label is requested
     * @return the label of the resource or <code>null</code> if such a label does
     *         not exist
     */
    public static String getLabel(Model model, Resource resource) {
        return getStringValue(model, resource, RDFS.label);
    }

    /**
     * Returns the description, i.e., the value of rdfs:comment, of the given
     * {@link Resource} if it is present in the given {@link Model}.
     *
     * @param model
     *            the model that should contain the label
     * @param resource
     *            the resource for which the label is requested
     * @return the description of the resource or <code>null</code> if such a label
     *         does not exist
     */
    public static String getDescription(Model model, Resource resource) {
        return getStringValue(model, resource, RDFS.comment);
    }

    /**
     * Returns the object as String of the first triple that has the given subject
     * and predicate and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param subject
     *            the subject of the triple <code>null</code> works like a wildcard.
     * @param predicate
     *            the predicate of the triple <code>null</code> works like a
     *            wildcard.
     * @return object of the triple as String or <code>null</code> if such a triple
     *         couldn't be found
     */
    public static String getStringValue(Model model, Resource subject, Property predicate) {
        if (model == null) {
            return null;
        }
        NodeIterator nodeIterator = model.listObjectsOfProperty(subject, predicate);
        if (nodeIterator.hasNext()) {
            RDFNode node = nodeIterator.next();
            if (node.isLiteral()) {
                return node.asLiteral().getString();
            } else {
                return node.toString();
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the objects as Strings of all triples that have the given subject and
     * predicate and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triples
     * @param subject
     *            the subject of the triples. <code>null</code> works like a
     *            wildcard.
     * @param predicate
     *            the predicate of the triples. <code>null</code> works like a
     *            wildcard.
     * @return objects of the triples as Strings
     */
    public static List<String> getStringValues(Model model, Resource subject, Property predicate) {
        List<String> values = new ArrayList<>();
        if (model != null) {
            NodeIterator nodeIterator = model.listObjectsOfProperty(subject, predicate);
            while (nodeIterator.hasNext()) {
                RDFNode node = nodeIterator.next();
                if (node.isLiteral()) {
                    values.add(node.asLiteral().getString());
                } else {
                    values.add(node.toString());
                }
            }
        }
        return values;
    }
    

    /**
     * Returns the object as {@link Calendar} of the first triple that has the given
     * subject and predicate and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param subject
     *            the subject of the triple. <code>null</code> works like a
     *            wildcard.
     * @param predicate
     *            the predicate of the triple. <code>null</code> works like a
     *            wildcard.
     * @return object of the triple as {@link Calendar} or <code>null</code> if such
     *         a triple couldn't be found or the value can not be read as XSDDate
     */
    public static Calendar getDateValue(Model model, Resource subject, Property predicate) {
        Calendar result = getCalendarValue(model, subject, predicate, XSDDatatype.XSDdate);
        if (result != null) {
            result.setTimeZone(Constants.DEFAULT_TIME_ZONE);
        }
        return result;
    }

    /**
     * Returns the object as {@link Calendar} of the first triple that has the given
     * subject and predicate and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param subject
     *            the subject of the triple. <code>null</code> works like a
     *            wildcard.
     * @param predicate
     *            the predicate of the triple. <code>null</code> works like a
     *            wildcard.
     * @return object of the triple as {@link Calendar} or <code>null</code> if such
     *         a triple couldn't be found or the value can not be read as
     *         XSDDateTime
     */
    public static Calendar getDateTimeValue(Model model, Resource subject, Property predicate) {
        return getCalendarValue(model, subject, predicate, XSDDatatype.XSDdateTime);
    }

    protected static Calendar getCalendarValue(Model model, Resource subject, Property predicate,
            XSDDatatype dateType) {
        if (model == null) {
            return null;
        }
        Literal literal = getLiteral(model, subject, predicate);
        if (literal != null) {
            try {
                Object o = dateType.parse(literal.getString());
                if (o instanceof XSDDateTime) {
                    return ((XSDDateTime) o).asCalendar();
                }
            } catch (Exception e) {
                // nothing to do
                LOGGER.debug("Couldn't parse " + dateType.getURI() + ". Returning null.", e);
            }
        }
        return null;
    }

    /**
     * Returns the object as {@link Duration} of the first triple that has the given
     * subject and predicate and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param subject
     *            the subject of the triple. <code>null</code> works like a
     *            wildcard.
     * @param predicate
     *            the predicate of the triple. <code>null</code> works like a
     *            wildcard.
     * @return object of the triple as {@link Duration} or <code>null</code> if such
     *         a triple couldn't be found or the value can not be read as
     *         XSDDuration
     */
    public static Duration getDurationValue(Model model, Resource subject, Property predicate) {
        if (model == null) {
            return null;
        }
        Literal literal = getLiteral(model, subject, predicate);
        if (literal != null) {
            try {
                return Duration.parse(literal.getString());
            } catch (Exception e) {
                // nothing to do
                LOGGER.debug("Couldn't parse \"" + literal.getString() + "\" as xsd:duration. Returning null.", e);
            }
        }
        return null;
    }
    /**
     * Returns the objects as integer of all triples that have the given subject and
     * predicate and that can be found in the given model.
     * 
     * @param model
     * 				the model that should contain the triple
     * @param subject
     *            the subject of the triple. <code>null</code> works like a
     *            wildcard.
     * @param predicate
     *            the predicate of the triple. <code>null</code> works like a
     *            wildcard.
     * @return object of the triple as integer or 0 if such
     *         a triple couldn't be found	
     */
    public static int getIntValue(Model model, Resource subject, Property predicate) {
        if (model == null) {
            return 0;
        }
        Literal literal = getLiteral(model, subject, predicate);
        if (literal != null) {
            try {
                return literal.getInt();
            } catch (Exception e) {
                // nothing to do
                LOGGER.debug("Exception occured: ", e);
            }
        }
        return 0;
    }
    
    public static short getShortValue(Model model, Resource subject, Property predicate) {
        if (model == null) {
            return 0;
        }
        Literal literal = getLiteral(model, subject, predicate);
        if (literal != null) {
            try {
                return literal.getShort() ;
            } catch (Exception e) {
                // nothing to do
                LOGGER.debug("Exception occured: ", e);
            }
        }
        return 0;
    }
    
    public static long getLongValue(Model model, Resource subject, Property predicate) {
        if (model == null) {
            return 0;
        }
        Literal literal = getLiteral(model, subject, predicate);
        if (literal != null) {
            try {
                return literal.getLong() ;
            } catch (Exception e) {
                // nothing to do
                LOGGER.debug("Exception occured: ", e);
            }
        }
        return 0;
    }
    
    public static byte getByteValue(Model model, Resource subject, Property predicate) {
        if (model == null) {
            return 0;
        }
        Literal literal = getLiteral(model, subject, predicate);
        if (literal != null) {
            try {
                return literal.getByte() ;
            } catch (Exception e) {
                // nothing to do
                LOGGER.debug("Exception occured: ", e);
            }
        }
        return 0;
    }

    /**
     * Returns the objects as Boolean of all triples that have the given subject and
     * predicate and that can be found in the given model.
     *
     * @param model
     *              the model that should contain the triple
     * @param subject
     *              the subject of the triple. <code>null</code> works like a wildcard.
     * @param predicate
     *              the predicate of the triple. <code>null</code> works like a wildcard.
     * @return object of the triple as a Boolean or false
     *         if such a triple couldn't be found.
     */
    public static boolean getBooleanValue(Model model, Resource subject, Property predicate) {
        if (model == null) {
            return false;
        }
        Literal literal = getLiteral(model, subject, predicate);
        if (literal != null) {
            try {
                return literal.getBoolean();
            } catch (Exception e) {
                // nothing to do
                LOGGER.debug("Exception occured. Returning false.", e);
            }
        }
        return false;

    }

    /**
     * Returns the objects as Float of all triples that have the given subject and
     * predicate and that can be found in the given model.
     *
     * @param model
     *              the model that should contain the triple
     * @param subject
     *              the subject of the triple. <code>null</code> works like a wildcard.
     * @param predicate
     *              the predicate of the triple. <code>null</code> works like a wildcard.
     * @return object of the triple as a Float or 0F
     *         if such a triple couldn't be found.
     */
    public static Float getFloatValue(Model model, Resource subject, Property predicate) {
        if (model == null) {
            return 0F;
        }
        Literal literal = getLiteral(model, subject, predicate);
        if (literal != null) {
            try {
                return literal.getFloat();
            } catch (Exception e) {
                // nothing to do
                LOGGER.debug("Exception occured. Returning 0F.", e);
            }
        }
        return 0F;

    }


    /**
     * Returns the objects as Double of all triples that have the given subject and
     * predicate and that can be found in the given model.
     *
     * @param model
     *              the model that should contain the triple
     * @param subject
     *              the subject of the triple. <code>null</code> works like a wildcard.
     * @param predicate
     *              the predicate of the triple. <code>null</code> works like a wildcard.
     * @return object of the triple as a Double or 0D
     *         if such a triple couldn't be found.
     */
    public static Double getDoubleValue(Model model, Resource subject, Property predicate) {
        if (model == null) {
            return 0D;
        }
        Literal literal = getLiteral(model, subject, predicate);
        if (literal != null) {
            try {
                return literal.getDouble();
            } catch (Exception e) {
                // nothing to do
                LOGGER.debug("Exception occured. Returning 0D.", e);
            }
        }
        return 0D;

    }

    /**
     * Returns the objects as Character of all triples that have the given subject and
     * predicate and that can be found in the given model.
     *
     * @param model
     *              the model that should contain the triple
     * @param subject
     *              the subject of the triple. <code>null</code> works like a wildcard.
     * @param predicate
     *              the predicate of the triple. <code>null</code> works like a wildcard.
     * @return object of the triple as a Character or 0
     *         if such a triple couldn't be found.
     */
    public static char getCharValue(Model model, Resource subject, Property predicate) {
        if (model == null) {
            return 0;
        }
        Literal literal = getLiteral(model, subject, predicate);
        if (literal != null) {
            try {
                return literal.getChar();
            } catch (Exception e) {
                // nothing to do
                LOGGER.debug("Exception occured. Returning 0.", e);
            }
        }
        return 0;

    }


    /**
     * Returns the first triple literal that has the given subject and predicate and
     * that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param subject
     *            the subject of the triple. <code>null</code> works like a
     *            wildcard.
     * @param predicate
     *            the predicate of the triple. <code>null</code> works like a
     *            wildcard.
     * @return literal of the triple or <code>null</code> if such a literal couldn't
     *         be found
     */
    public static Literal getLiteral(Model model, Resource subject, Property predicate) {
        if (model == null) {
            return null;
        }
        NodeIterator nodeIterator = model.listObjectsOfProperty(subject, predicate);
        while (nodeIterator.hasNext()) {
            RDFNode node = nodeIterator.next();
            if (node.isLiteral()) {
                return node.asLiteral();
            }
        }
        return null;
    }

    /**
     * Returns the object as {@link Resource} of the first triple that has the given
     * subject and predicate and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param subject
     *            the subject of the triple
     * @param predicate
     *            the predicate of the triple
     * @return object of the triple as {@link Resource} or <code>null</code> if such
     *         a triple couldn't be found
     */
    public static Resource getObjectResource(Model model, Resource subject, Property predicate) {
        if (model != null) {
            NodeIterator nodeIterator = model.listObjectsOfProperty(subject, predicate);
            while (nodeIterator.hasNext()) {
                RDFNode node = nodeIterator.next();
                if (node.isResource()) {
                    return node.asResource();
                }
            }
        }
        return null;
    }

    /**
     * Returns the objects as {@link Resource}s of all triples that have the given
     * subject and predicate and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param subject
     *            the subject of the triple
     * @param predicate
     *            the predicate of the triple
     * @return List of object of the triples as {@link Resource}
     */
    public static List<Resource> getObjectResources(Model model, Resource subject, Property predicate) {
        List<Resource> objects = new ArrayList<>();
        if (model != null) {
            NodeIterator nodeIterator = model.listObjectsOfProperty(subject, predicate);
            while (nodeIterator.hasNext()) {
                RDFNode node = nodeIterator.next();
                if (node.isResource()) {
                    objects.add(node.asResource());
                }
            }
        }
        return objects;
    }

    /**
     * Returns a list of subjects of triples that have the given object and
     * predicate and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param predicate
     *            the predicate of the triple
     * @param object
     *            the object of the triple
     * @return List of subject of the triples as {@link Resource}
     */
    public static List<Resource> getSubjectResources(Model model, Property predicate, Resource object) {
        List<Resource> subjects = new ArrayList<>();
        if (model != null) {
            ResIterator resIterator = model.listSubjectsWithProperty(predicate, object);
            while (resIterator.hasNext()) {
                subjects.add(resIterator.next());
            }
        }
        return subjects;
    }

    

}
