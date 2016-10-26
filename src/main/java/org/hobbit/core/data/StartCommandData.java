package org.hobbit.core.data;

public class StartCommandData {

    public String image;
    public String type;
    /**
     * Name of the parent container
     */
    public String parent;
    public String[] environmentVariables;

    public StartCommandData(String image, String type, String parent, String[] environmentVariables) {
        this.image = image;
        this.type = type;
        this.parent = parent;
        this.environmentVariables = environmentVariables;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String[] getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(String[] environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StartCommandData [image=");
        builder.append(image);
        builder.append(", type=");
        builder.append(type);
        builder.append(", parent=");
        builder.append(parent);
        builder.append("]");
        return builder.toString();
    }
}
