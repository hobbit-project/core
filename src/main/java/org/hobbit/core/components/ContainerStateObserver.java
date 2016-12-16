package org.hobbit.core.components;

public interface ContainerStateObserver {

    public void containerStopped(String containerName, int exitCode);
}
