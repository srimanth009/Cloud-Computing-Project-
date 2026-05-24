package org.cloudsimplus.abc;

import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.stream.Collectors;

public class ABCLoadBalanceBroker extends DatacenterBrokerSimple {
    private List<FoodSource> foodSources;
    private int employedBeesCount;
    private int onlookerBeesCount;
    private int maxIterations;
    private int limit;
    private double threshold = 0.6;

    private Map<Long, List<Double>> cpuHistory;
    private Map<Long, List<Double>> memoryHistory;
    private Map<Long, List<Double>> diskHistory;
    private Map<Long, List<Double>> networkHistory;
    private Map<Long, List<Double>> loadHistory;
    private int roundRobinIndex = 0;
    private Map<Long, Integer> vmConnectionCount = new HashMap<>();
    private Map<Long, Integer> vmWeights = new HashMap<>();
    private Map<Long, Integer> currentWeights = new HashMap<>();

    public ABCLoadBalanceBroker(final CloudSim simulation) {
        super(simulation);
        initializeABCParameters();
    }

    public ABCLoadBalanceBroker(final CloudSim simulation, final String name) {
        super(simulation, name);
        initializeABCParameters();
    }

    private void initializeABCParameters() {
        this.foodSources = new ArrayList<>();
        this.cpuHistory = new HashMap<>();
        this.memoryHistory = new HashMap<>();
        this.diskHistory = new HashMap<>();
        this.networkHistory = new HashMap<>();
        this.loadHistory = new HashMap<>();
        this.employedBeesCount = 8;
        this.onlookerBeesCount = 8;
        this.maxIterations = 30;
        this.limit = 5;
    }

    protected void submitCloudlets() {
        System.out.println("ABC Broker: Starting ABC load balancing process...");
        initializeConnectionCounts();
        initializeWeights();
        initializeFoodSources();
        optimizeWithABC();
        submitCloudletsToOptimalVMs();
    }

    private void initializeConnectionCounts() {
        vmConnectionCount.clear();
        for (Vm vm : getVmCreatedList()) {
            vmConnectionCount.put(vm.getId(), 0);
        }
    }

    private void initializeWeights() {
        vmWeights.clear();
        currentWeights.clear();
        List<Vm> vmList = getVmCreatedList();

        for (Vm vm : vmList) {
            int weight = calculateVmWeight(vm);
            vmWeights.put(vm.getId(), weight);
            currentWeights.put(vm.getId(), weight);
        }

        System.out.println("VM Weights initialized: " + vmWeights);
    }

    private int calculateVmWeight(Vm vm) {
        // Calculate weight based on VM capacity - higher MIPS = higher weight
        double weight = (vm.getMips() * vm.getNumberOfPes()) / 100.0;
        return Math.max(1, (int) weight);
    }

    private void initializeFoodSources() {
        // As per research paper: Initialization stage
        // Determines the number of food sources m, the dimension of the solution space D, 
        // and the number of employed bees (equal to m)
        foodSources.clear();
        List<Vm> vmList = getVmCreatedList();

        for (int i = 0; i < vmList.size(); i++) {
            Vm vm = vmList.get(i);
            FoodSource foodSource = new FoodSource(i, vm, 4);
            foodSources.add(foodSource);

            cpuHistory.put(vm.getId(), new ArrayList<>());
            memoryHistory.put(vm.getId(), new ArrayList<>());
            diskHistory.put(vm.getId(), new ArrayList<>());
            networkHistory.put(vm.getId(), new ArrayList<>());
            loadHistory.put(vm.getId(), new ArrayList<>());
        }

        System.out.println("Initialized " + foodSources.size() + " food sources (VMs)");
        System.out.println("Number of employed bees: " + employedBeesCount);
        System.out.println("Solution space dimension: 4");
    }

    private void optimizeWithABC() {
        System.out.println("Starting ABC optimization with " + maxIterations + " iterations...");

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            updateFoodSourceMetrics();
            employedBeePhase();
            onlookerBeePhase();
            scoutBeePhase();
            trackHistoricalMetrics();

            if (iteration % 5 == 0) {
                System.out.printf("Iteration %d: Best fitness = %.4f, Worst fitness = %.4f%n",
                        iteration, getBestFitness(), getWorstFitness());
            }
        }

        System.out.println("ABC optimization completed.");
        printFoodSourceStatus();
    }

    private void updateFoodSourceMetrics() {
        double currentTime = getSimulation().clock();
        for (FoodSource fs : foodSources) {
            fs.updateRealMetrics(currentTime);
        }
    }

    private void trackHistoricalMetrics() {
        for (FoodSource fs : foodSources) {
            Vm vm = fs.getVm();
            cpuHistory.get(vm.getId()).add(fs.getCpuUsage());
            memoryHistory.get(vm.getId()).add(fs.getMemoryUsage());
            diskHistory.get(vm.getId()).add(fs.getDiskIOLoad());
            networkHistory.get(vm.getId()).add(fs.getNetworkLoad());
            loadHistory.get(vm.getId()).add(fs.getLoad());
        }
    }

    private void employedBeePhase() {
        // As per research paper: Employed bee stage
        // Employed bees explore food sources and generate new candidate solutions
        for (int i = 0; i < employedBeesCount; i++) {
            int currentIndex = i % foodSources.size();
            FoodSource current = foodSources.get(currentIndex);

            // Formula (4): V_ji = X_ji + β_ji(X_ji - X_ki), j ≠ k
            int partnerIndex = (currentIndex + 1) % foodSources.size();
            FoodSource partner = foodSources.get(partnerIndex);

            double[] newPosition = generateNewSolution(current, partner);
            double newFitness = evaluateSolution(newPosition);
            double currentFitness = current.getFitness();

            // Greedy selection algorithm
            if (newFitness > currentFitness) {
                current.setPosition(newPosition);
                current.resetTrial();
                current.calculateFitness();
            } else {
                current.incrementTrial();
            }
        }
    }

    private double[] generateNewSolution(FoodSource current, FoodSource partner) {
        // Formula (4): V_ji = X_ji + β_ji(X_ji - X_ki), j ≠ k
        double[] newPosition = new double[current.getPosition().length];
        double[] currentPos = current.getPosition();
        double[] partnerPos = partner.getPosition();

        // Use deterministic dimension selection to reduce randomness
        int changingDimension = 0; // Always change first dimension for consistency

        for (int i = 0; i < currentPos.length; i++) {
            if (i == changingDimension) {
                // β_ji is a random number between -1 and 1
                double beta = 0.5; // Fixed value for consistency
                newPosition[i] = currentPos[i] + beta * (currentPos[i] - partnerPos[i]);
                // Ensure position stays within bounds [0, 1]
                if (newPosition[i] < 0) newPosition[i] = 0;
                if (newPosition[i] > 1) newPosition[i] = 1;
            } else {
                newPosition[i] = currentPos[i];
            }
        }

        return newPosition;
    }

    private void onlookerBeePhase() {
        // As per research paper: Onlooker bee stage
        // Onlooker bees exploit abundant food sources shared by employed bees
        for (int i = 0; i < onlookerBeesCount; i++) {
            FoodSource selected = selectFoodSourceByRoulette();

            if (selected != null) {
                // Use deterministic partner selection to reduce randomness
                int selectedIndex = foodSources.indexOf(selected);
                int partnerIndex = (selectedIndex + 2) % foodSources.size();
                FoodSource partner = foodSources.get(partnerIndex);
                
                double[] newPosition = generateNewSolution(selected, partner);
                double newFitness = evaluateSolution(newPosition);

                // Greedy selection algorithm
                if (newFitness > selected.getFitness()) {
                    selected.setPosition(newPosition);
                    selected.resetTrial();
                    selected.calculateFitness();
                } else {
                    selected.incrementTrial();
                }
            }
        }
    }

    private FoodSource selectFoodSourceByRoulette() {
        // Formula (5): P_j = fit_j / Σ(fit_j) (summation over j from 1 to m)
        if (foodSources.isEmpty()) return null;

        double totalFitness = foodSources.stream()
                .mapToDouble(FoodSource::getFitness)
                .sum();

        if (totalFitness == 0) return foodSources.get(0);

        double randomValue = Math.random() * totalFitness;
        double cumulative = 0;

        for (FoodSource fs : foodSources) {
            cumulative += fs.getFitness();
            if (cumulative >= randomValue) {
                return fs;
            }
        }

        return foodSources.get(foodSources.size() - 1);
    }

    private void scoutBeePhase() {
        // As per research paper: Scout bee stage
        // If a food source reaches an exploitation limit L_source without being updated, 
        // it is eliminated and a new food source is randomly selected
        for (FoodSource fs : foodSources) {
            if (fs.getTrialCount() >= limit) {
                System.out.println("Scout bee reinitializing food source for VM " + fs.getVm().getId());
                double[] newPosition = new double[fs.getPosition().length];
                // Formula (2): X_ji = X_min + Φ_ji(X_max - X_min)
                // Use deterministic initialization to reduce randomness
                for (int i = 0; i < newPosition.length; i++) {
                    newPosition[i] = 0.5; // Fixed value for consistency
                }
                fs.setPosition(newPosition);
                fs.resetTrial();
            }
        }
    }

    private double evaluateSolution(double[] position) {
        // As per paper: fit_j = 1/(1 + Load_j,t)
        // Load_j,t = Tcount,j / comput_j,t (processor load)
        double totalLoad = 0;
        for (double value : position) {
            totalLoad += value;
        }
        double avgLoad = totalLoad / position.length;
        return 1.0 / (1.0 + avgLoad);
    }

    private double getBestFitness() {
        return foodSources.stream()
                .mapToDouble(FoodSource::getFitness)
                .max()
                .orElse(0);
    }

    private double getWorstFitness() {
        return foodSources.stream()
                .mapToDouble(FoodSource::getFitness)
                .min()
                .orElse(0);
    }

    private void printFoodSourceStatus() {
        System.out.println("\n=== Final Food Source Status ===");
        List<FoodSource> sorted = foodSources.stream()
                .sorted((fs1, fs2) -> Double.compare(fs2.getFitness(), fs1.getFitness()))
                .collect(Collectors.toList());

        for (FoodSource fs : sorted) {
            System.out.println(fs);
        }
    }

    private void submitCloudletsToOptimalVMs() {
        List<Cloudlet> cloudletList = getCloudletSubmittedList();
        List<Vm> vmList = getVmCreatedList();

        if (cloudletList.isEmpty() || vmList.isEmpty()) {
            System.out.println("No cloudlets or VMs available.");
            return;
        }

        System.out.println("\nSubmitting " + cloudletList.size() + " cloudlets using ABC-DTS load balancing...");

        for (Cloudlet cloudlet : cloudletList) {
            Vm selectedVm = selectVMUsingDynamicStaticThreshold(cloudlet);
            bindCloudletToVm(cloudlet, selectedVm);
            incrementConnectionCount(selectedVm.getId());
        }

        super.submitCloudletList(cloudletList);
        System.out.println("Submitted " + cloudletList.size() + " cloudlets to VMs");
    }

    private Vm selectVMUsingDynamicStaticThreshold(Cloudlet cloudlet) {
        // As per research paper: Dynamic Static Threshold Logic
        // If fit_j <= 0.6: Use static Weighted Round Robin
        // If fit_j > 0.6: Use dynamic Least Connection
        
        // Calculate average fitness across all VMs
        double avgFitness = foodSources.stream()
                .mapToDouble(FoodSource::getFitness)
                .average()
                .orElse(0.0);
        
        System.out.println("Average fitness: " + String.format("%.4f", avgFitness) + 
                ", Threshold: " + threshold);
        
        Vm selectedVm;
        if (avgFitness <= threshold) {
            // Light load: Use static Weighted Round Robin algorithm
            selectedVm = getWeightedRoundRobinVM();
            System.out.println("Using Weighted Round Robin (static) - Light load detected");
        } else {
            // Heavy load: Use dynamic Least Connection algorithm
            selectedVm = getLeastConnectionVM();
            System.out.println("Using Least Connection (dynamic) - Heavy load detected");
        }
        
        return selectedVm;
    }


    private Vm getWeightedRoundRobinVM() {
        List<Vm> vmList = getVmCreatedList();
        
        while (true) {
            roundRobinIndex = (roundRobinIndex + 1) % vmList.size();
            Vm currentVm = vmList.get(roundRobinIndex);
            
            if (currentWeights.get(currentVm.getId()) > 0) {
                currentWeights.put(currentVm.getId(), currentWeights.get(currentVm.getId()) - 1);
                return currentVm;
            } else {
                // Reset weights if all are zero
                if (currentWeights.values().stream().allMatch(w -> w == 0)) {
                    for (Vm vm : vmList) {
                        currentWeights.put(vm.getId(), vmWeights.get(vm.getId()));
                    }
                }
            }
        }
    }

    private Vm getLeastConnectionVM() {
        List<Vm> vmList = getVmCreatedList();
        Vm selectedVm = vmList.get(0);
        int minConnections = vmConnectionCount.get(selectedVm.getId());

        for (Vm vm : vmList) {
            int connections = vmConnectionCount.get(vm.getId());
            if (connections < minConnections) {
                minConnections = connections;
                selectedVm = vm;
            }
        }
        return selectedVm;
    }

    private void incrementConnectionCount(long vmId) {
        vmConnectionCount.put(vmId, vmConnectionCount.get(vmId) + 1);
    }

    public void printPerformanceStats() {
        System.out.println("\n=== ABC-DTS Load Balancing Performance Statistics ===");
        System.out.printf("Dynamic Static Threshold: %.2f%n", threshold);
        System.out.printf("Best fitness: %.4f%n", getBestFitness());
        System.out.printf("Worst fitness: %.4f%n", getWorstFitness());
        System.out.printf("Fitness range: %.4f%n", getBestFitness() - getWorstFitness());

        double avgFitness = foodSources.stream()
                .mapToDouble(FoodSource::getFitness)
                .average()
                .orElse(0);
        System.out.printf("Average fitness: %.4f%n", avgFitness);

        // Determine current algorithm based on average fitness
        String currentAlgorithm = avgFitness <= threshold ? "Weighted Round Robin (Static)" : "Least Connection (Dynamic)";
        System.out.printf("Current Algorithm: %s%n", currentAlgorithm);
        
        long lightLoadVMs = foodSources.stream()
                .filter(fs -> fs.getFitness() <= threshold)
                .count();
        System.out.printf("VMs with light load (fit_j <= %.2f): %d/%d%n",
                threshold, lightLoadVMs, foodSources.size());

        System.out.printf("VMs with heavy load (fit_j > %.2f): %d/%d%n",
                threshold, foodSources.size() - lightLoadVMs, foodSources.size());

        printHistoricalAverages();
        printConnectionCounts();
        printWeightDistribution();
    }

    private void printConnectionCounts() {
        System.out.println("\n=== Current Connection Counts ===");
        for (Map.Entry<Long, Integer> entry : vmConnectionCount.entrySet()) {
            System.out.printf("VM %d: %d connections%n", entry.getKey(), entry.getValue());
        }
    }

    private void printWeightDistribution() {
        System.out.println("\n=== VM Weight Distribution ===");
        for (Map.Entry<Long, Integer> entry : vmWeights.entrySet()) {
            System.out.printf("VM %d: Weight=%d, Current Weight=%d%n", 
                    entry.getKey(), entry.getValue(), currentWeights.get(entry.getKey()));
        }
    }

    private void printHistoricalAverages() {
        System.out.println("\n=== Historical Metrics Averages ===");
        for (FoodSource fs : foodSources) {
            long vmId = fs.getVm().getId();
            double avgCpu = cpuHistory.get(vmId).stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double avgMemory = memoryHistory.get(vmId).stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double avgDisk = diskHistory.get(vmId).stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double avgNetwork = networkHistory.get(vmId).stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double avgLoad = loadHistory.get(vmId).stream().mapToDouble(Double::doubleValue).average().orElse(0);

            System.out.printf("VM %d: CPU=%.3f, RAM=%.3f, Disk=%.3f, Net=%.3f, Load=%.3f, Fitness=%.3f%n",
                    vmId, avgCpu, avgMemory, avgDisk, avgNetwork, avgLoad, fs.getFitness());
        }
    }
}
