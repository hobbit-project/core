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
}
