package org.hobbit.utils.rdf;

import java.util.Calendar;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

/**
 * Implements simple methods to get data from a given RDF Model.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class RdfHelper {

    /**
     * Returns the label of the given {@link Resource} if it is present in the
     * given {@link Model}.
     *
     * @param model
     *            the model that should contain the label
     * @param resource
     *            the resource for which the label is requested
     * @return the label of the resource or <code>null</code> if such a label
     *         does not exist
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
     * @return the description of the resource or <code>null</code> if such a
     *         label does not exist
     */
    public static String getDescription(Model model, Resource resource) {
        return getStringValue(model, resource, RDFS.comment);
    }

    /**
     * Returns the object as String of the first triple that has the given
     * subject and predicate and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param subject
     *            the subject of the triple <code>null</code> works like a
     *            wildcard.
     * @param predicate
     *            the predicate of the triple <code>null</code> works like a
     *            wildcard.
     * @return object of the triple as String or <code>null</code> if such a
     *         triple couldn't be found
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
     * Returns the object as {@link Calendar} of the first triple that has the
     * given subject and predicate and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param subject
     *            the subject of the triple. <code>null</code> works like a
     *            wildcard.
     * @param predicate
     *            the predicate of the triple. <code>null</code> works like a
     *            wildcard.
     * @return object of the triple as {@link Calendar} or <code>null</code> if
     *         such a triple couldn't be found or the value can not be read as
     *         {@link Calendar}
     */
    public static Calendar getDateValue(Model model, Resource subject, Property predicate) {
        if (model == null) {
            return null;
        }
        Literal literal = getLiteral(model, subject, predicate);
        if (literal != null) {
            try {
                Object o = XSDDatatype.XSDdate.parse(literal.getString());
                if (o instanceof XSDDateTime) {
                    return ((XSDDateTime) o).asCalendar();
                }
            } catch (Exception e) {
                // nothing to do
            }
        }
        return null;
    }

    /**
     * Returns the first triple literal that has the given subject and predicate
     * and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param subject
     *            the subject of the triple. <code>null</code> works like a
     *            wildcard.
     * @param predicate
     *            the predicate of the triple. <code>null</code> works like a
     *            wildcard.
     * @return literal of the triple or <code>null</code> if such a literal
     *         couldn't be found
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
}
