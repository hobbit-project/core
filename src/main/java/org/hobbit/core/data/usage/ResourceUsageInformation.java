package org.hobbit.core.data.usage;

public class ResourceUsageInformation {

    private DiskStats diskStats;
    private MemoryStats memoryStats;
    private CpuStats cpuStats;
    
    public ResourceUsageInformation() {
    }
    
    public ResourceUsageInformation(DiskStats diskStats, MemoryStats memoryStats, CpuStats cpuStats) {
        super();
        this.diskStats = diskStats;
        this.memoryStats = memoryStats;
        this.cpuStats = cpuStats;
    }

    /**
     * @return the diskStats
     */
    public DiskStats getDiskStats() {
        return diskStats;
    }

    /**
     * @param diskStats the diskStats to set
     */
    public void setDiskStats(DiskStats diskStats) {
        this.diskStats = diskStats;
    }

    /**
     * @return the memoryStats
     */
    public MemoryStats getMemoryStats() {
        return memoryStats;
    }

    /**
     * @param memoryStats the memoryStats to set
     */
    public void setMemoryStats(MemoryStats memoryStats) {
        this.memoryStats = memoryStats;
    }

    /**
     * @return the cpuStats
     */
    public CpuStats getCpuStats() {
        return cpuStats;
    }

    /**
     * @param cpuStats the cpuStats to set
     */
    public void setCpuStats(CpuStats cpuStats) {
        this.cpuStats = cpuStats;
    }
}
