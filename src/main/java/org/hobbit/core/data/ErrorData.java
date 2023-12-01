package org.hobbit.core.data;

/**
 * A simple data structure that represents errors that components can report.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class ErrorData {

    /**
     * ID of the container reporting the error.
     */
    protected String containerId;
    /**
     * IRI of the error type (optional).
     */
    protected String errorType;
    /**
     * A string that can be used as short label of an error (optional, the error
     * type label will be used as default)
     */
    protected String label;
    /**
     * A string that can be used as a short description of an error (optional, the
     * error type description will be used as default).
     */
    protected String description;
    /**
     * A string that contains details about the error, e.g., a stack trace
     * (optional).
     */
    protected String details;

    /**
     * @return the containerId
     */
    public String getContainerId() {
        return containerId;
    }

    /**
     * @param containerId the containerId to set
     */
    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    /**
     * @return the errorType
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * @param errorType the errorType to set
     */
    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the details
     */
    public String getDetails() {
        return details;
    }

    /**
     * @param details the details to set
     */
    public void setDetails(String details) {
        this.details = details;
    }

}
