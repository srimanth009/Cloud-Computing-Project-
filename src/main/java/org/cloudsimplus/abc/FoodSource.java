package org.cloudsimplus.abc;

import org.cloudbus.cloudsim.cloudlets.CloudletExecution;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.List;

public class FoodSource {
    private final int id;
    private final Vm vm;
    private final double[] position;
    private double fitness;
    private double load;
    private int trialCount;

    private double cpuUsage;
    private double memoryUsage;
    private double diskIOLoad;
    private double networkLoad;

    public FoodSource(int id, Vm vm, int dimension) {
        this.id = id;
        this.vm = vm;
        this.position = new double[dimension];
        this.trialCount = 0;
        initializePosition();
    }

    private void initializePosition() {
        // Formula (1): X_j = (X_j1, X_j2, ..., X_jD)
        // Formula (2): X_ji = X_min + Φ_ji(X_max - X_min)
        // Φ_ji is a random number between -1 and 1
        // X_min = 0, X_max = 1 for normalized position
        for (int i = 0; i < position.length; i++) {
            // Use deterministic initialization to reduce randomness
            position[i] = 0.5; // Fixed value for consistency
        }
    }

    public void updateRealMetrics(double currentTime) {
        this.cpuUsage = getActualCpuUtilization(currentTime);
        this.memoryUsage = getActualMemoryUtilization();
        this.diskIOLoad = getActualDiskIOLoad();
        this.networkLoad = getActualNetworkUtilization(currentTime);

        calculateRealLoad();
        calculateFitness();
    }

    private double getActualCpuUtilization(double currentTime) {
        if (vm.getCloudletScheduler() != null) {
            double utilization = vm.getCloudletScheduler().getRequestedCpuPercent(currentTime);
            return Math.min(1.0, Math.max(0.0, utilization));
        }
        return 0.0;
    }

    private double getActualMemoryUtilization() {
        if (vm.getCloudletScheduler() == null) return 0.0;

        int runningCloudlets = vm.getCloudletScheduler().getCloudletExecList().size();
        int baseMemoryPerCloudlet = 100;
        double memoryUtilization = (double) (runningCloudlets * baseMemoryPerCloudlet) / vm.getRam().getCapacity();
        return Math.min(1.0, Math.max(0.0, memoryUtilization));
    }

    private double getActualDiskIOLoad() {
        if (vm.getCloudletScheduler() == null) return 0.0;

        long totalFileSize = 0;
        for (CloudletExecution cl : vm.getCloudletScheduler().getCloudletExecList()) {
            totalFileSize += cl.getCloudlet().getFileSize();
        }
        double diskLoad = (double) totalFileSize / (1024 * 1024 * 1024); // GB
        return Math.min(1.0, Math.max(0.0, diskLoad));
    }

    private double getActualNetworkUtilization(double currentTime) {
        if (vm.getCloudletScheduler() == null) return 0.0;

        double totalBw = 0.0;
        List<CloudletExecution> execList = vm.getCloudletScheduler().getCloudletExecList();
        for (CloudletExecution cl : execList) {
            totalBw += cl.getCloudlet().getUtilizationModelBw().getUtilization(currentTime);
        }
        double avgBw = execList.isEmpty() ? 0.0 : totalBw / execList.size();
        return Math.min(1.0, Math.max(0.0, avgBw));
    }

    private void calculateRealLoad() {
        // Formula (6): Load_jt = T_count,j / comput_j,t
        // T_count,j represents the total number of tasks on processor j at time t
        // comput_j,t represents the processing speed of heterogeneous processor j
        
        // Calculate task count (number of running cloudlets)
        int taskCount = vm.getCloudletScheduler() != null ? 
            vm.getCloudletScheduler().getCloudletExecList().size() : 0;
        
        // Calculate processing speed (MIPS * PEs)
        double processingSpeed = vm.getMips() * vm.getNumberOfPes();
        
        // Apply the formula: Load = task_count / processing_speed
        if (processingSpeed > 0) {
            this.load = taskCount / processingSpeed;
        } else {
            this.load = 0.0;
        }
        
        // Normalize load to [0, 1] range
        this.load = Math.min(1.0, Math.max(0.0, this.load));
    }

    public double calculateFitness() {
        // Formula (7): fit_j = 1 / (1 + Load_j)
        // Higher fit_j value indicates smaller load
        this.fitness = 1.0 / (1.0 + this.load);
        return this.fitness;
    }

    // Getters and setters
    public int getId() { return id; }
    public double getFitness() { return fitness; }
    public double getLoad() { return load; }
    public Vm getVm() { return vm; }
    public int getTrialCount() { return trialCount; }
    public void incrementTrial() { trialCount++; }
    public void resetTrial() { trialCount = 0; }
    public double[] getPosition() { return position; }
    public double getCpuUsage() { return cpuUsage; }
    public double getMemoryUsage() { return memoryUsage; }
    public double getDiskIOLoad() { return diskIOLoad; }
    public double getNetworkLoad() { return networkLoad; }

    public void setPosition(double[] position) {
        System.arraycopy(position, 0, this.position, 0, position.length);
    }

    @Override
    public String toString() {
        return String.format("FoodSource{VM%d: fitness=%.4f, load=%.4f, CPU=%.2f, RAM=%.2f, Disk=%.2f, Net=%.2f, trials=%d}",
                vm.getId(), fitness, load, cpuUsage, memoryUsage, diskIOLoad, networkLoad, trialCount);
    }
}
