package org.hobbit.core.data;

public class StopCommandData {
    public String containerName;

    public StopCommandData(String containerName) {
        this.containerName = containerName;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StopCommandData [containerName=");
        builder.append(containerName);
        builder.append("]");
        return builder.toString();
    }

}
