package org.hobbit.core.data;

import java.util.concurrent.Semaphore;

public class ContainerTermination {

    @Deprecated
    private Semaphore isTerminated = new Semaphore(0);
    private boolean terminated = false;
    private int exitCode = 0;

    public void notifyTermination(int exitCode) {
        this.exitCode = exitCode;
        terminated = true;
        isTerminated.release();
    }

    @Deprecated
    public void waitForTermination() throws InterruptedException {
        isTerminated.acquire();
    }

    public boolean isTerminated() {
        return terminated;
    }

    public int getExitCode() {
        return exitCode;
    }
}
