package org.hobbit.core.data.usage;

public class DiskStats {
    
    private long fsSizeSum = 0;

    public DiskStats() {
    }

    public DiskStats(long fsSizeSum) {
        super();
        this.fsSizeSum = fsSizeSum;
    }

    /**
     * @return the fsSizeSum
     */
    public long getFsSizeSum() {
        return fsSizeSum;
    }

    /**
     * @param fsSizeSum the fsSizeSum to set
     */
    public void setFsSizeSum(long fsSizeSum) {
        this.fsSizeSum = fsSizeSum;
    }

    public DiskStats merge(DiskStats diskStats) {
        this.fsSizeSum += diskStats.fsSizeSum;
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DiskStats [fsSizeSum=");
        builder.append(fsSizeSum);
        builder.append("]");
        return builder.toString();
    }

}
