package org.hobbit.core.data.usage;

public class CpuStats {

    /**
     * The sum of the overall CPU usage in ms.
     */
    private long totalUsage = 0;

    public CpuStats() {
    }

    public CpuStats(long totalUsage) {
        super();
        this.totalUsage = totalUsage;
    }

    /**
     * @return the totalUsage
     */
    public long getTotalUsage() {
        return totalUsage;
    }

    /**
     * @param totalUsage the totalUsage to set
     */
    public void setTotalUsage(long totalUsage) {
        this.totalUsage = totalUsage;
    }
}
