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
    
    public ResourceUsageInformation merge(ResourceUsageInformation other) {
        if(this.diskStats != null) {
            if(other.diskStats != null) {
                this.diskStats = this.diskStats.merge(other.diskStats);
            }
        } else if(other.diskStats != null) {
            this.diskStats = other.diskStats;
        }
        if(this.memoryStats != null) {
            if(other.memoryStats != null) {
                this.memoryStats = this.memoryStats.merge(other.memoryStats);
            }
        } else if(other.memoryStats != null) {
            this.memoryStats = other.memoryStats;
        }
        if(this.cpuStats != null) {
            if(other.cpuStats != null) {
                this.cpuStats = this.cpuStats.merge(other.cpuStats);
            }
        } else if(other.cpuStats != null) {
            this.cpuStats = other.cpuStats;
        }
        return this;
    }
    
    public static ResourceUsageInformation staticMerge(ResourceUsageInformation o1, ResourceUsageInformation o2) {
        if(o1 == null) {
            return o2;
        }
        if(o2 == null) {
            return o1;
        }
        return o1.merge(o2);
    }
}
