package org.hobbit.core.data.usage;

public class MemoryStats {

    private long usageSum = 0;

    public MemoryStats() {
    }

    public MemoryStats(long usageSum) {
        this.usageSum = usageSum;
    }

    /**
     * @return the usageSum
     */
    public long getUsageSum() {
        return usageSum;
    }

    /**
     * @param usageSum the usageSum to set
     */
    public void setUsageSum(long usageSum) {
        this.usageSum = usageSum;
    }

    public MemoryStats merge(MemoryStats memoryStats) {
        this.usageSum += memoryStats.usageSum;
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MemoryStats [usageSum=");
        builder.append(usageSum);
        builder.append("]");
        return builder.toString();
    }
}
