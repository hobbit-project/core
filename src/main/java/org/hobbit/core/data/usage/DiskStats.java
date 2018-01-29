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

}
