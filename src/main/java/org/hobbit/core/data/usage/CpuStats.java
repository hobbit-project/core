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

    public CpuStats merge(CpuStats cpuStats) {
        this.totalUsage += cpuStats.totalUsage;
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CpuStats [totalUsage=");
        builder.append(totalUsage);
        builder.append("]");
        return builder.toString();
    }
}
