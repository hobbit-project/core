package org.hobbit.core.data;

public class StopCommandData {
    public String containerId;

    public StopCommandData(String containerId) {
        this.containerId = containerId;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StopCommandData [containerId=");
        builder.append(containerId);
        builder.append("]");
        return builder.toString();
    }

}
