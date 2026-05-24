package org.cloudsimplus.abc;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.*;

/**
 * Simplified Load Balancing Simulation for Testing Different Cloudlet Counts
 */
public class SimplifiedLoadBalanceTest {

    private static int GLOBAL_CLOUDLET_COUNT = 30;

    public static void main(String[] args) {
        try {
            System.out.println("Starting Simplified Load Balancing Test...");
            System.out.println("==========================================");

            // Test with different numbers of cloudlets
            int[] cloudletCounts = {100, 200, 300, 400};
            
            for (int cloudletCount : cloudletCounts) {
                System.out.println("\n" + "=".repeat(80));
                System.out.println("TESTING WITH " + cloudletCount + " CLOUDLETS");
                System.out.println("=".repeat(80));
                
                GLOBAL_CLOUDLET_COUNT = cloudletCount;
                
                // Test each algorithm
                testAlgorithm("ABC-DTS", cloudletCount);
                testAlgorithm("Weighted Round Robin", cloudletCount);
                testAlgorithm("Least Connection", cloudletCount);
                
                System.out.println("\n" + "-".repeat(60));
                System.out.println("COMPLETED TEST FOR " + cloudletCount + " CLOUDLETS");
                System.out.println("-".repeat(60));
            }

            System.out.println("\n" + "=".repeat(80));
            System.out.println("ALL TESTS COMPLETED!");
            System.out.println("=".repeat(80));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Test failed: " + e.getMessage());
        }
    }
    
    private static void testAlgorithm(String algorithmName, int cloudletCount) {
        try {
            CloudSim simulation = new CloudSim();
            Datacenter datacenter = createSimpleDatacenter(simulation);
            
            DatacenterBroker broker;
            switch (algorithmName) {
                case "ABC-DTS":
                    broker = new ABCLoadBalanceBroker(simulation, "ABC_Broker");
                    break;
                case "Weighted Round Robin":
                    broker = new WeightedRoundRobinBroker(simulation, "WRR_Broker");
                    break;
                case "Least Connection":
                    broker = new LeastConnectionBroker(simulation, "LC_Broker");
                    break;
                default:
                    return;
            }
            
            List<Vm> vms = createSimpleVMs();
            List<Cloudlet> cloudlets = createSimpleCloudlets();
            
            broker.submitVmList(vms);
            broker.submitCloudletList(cloudlets);
            
            simulation.start();
            
            // Calculate results
            List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
            double avgExecutionTime = finishedCloudlets.stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .average()
                .orElse(0.0);
            
            double slowestTime = finishedCloudlets.stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0.0);
            
            double fastestTime = finishedCloudlets.stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .min()
                .orElse(0.0);
            
            // Calculate load distribution
            Map<Long, Integer> vmDistribution = new HashMap<>();
            for (Cloudlet cloudlet : finishedCloudlets) {
                long vmId = cloudlet.getVm().getId();
                vmDistribution.put(vmId, vmDistribution.getOrDefault(vmId, 0) + 1);
            }
            
            // Calculate efficiency (lower is better)
            double efficiency = calculateEfficiency(vmDistribution);
            
            // Print results
            System.out.printf("%-25s: %d cloudlets, Avg: %.2fs, Range: %.2f-%.2fs, Efficiency: %.2f%%%n",
                algorithmName, finishedCloudlets.size(), avgExecutionTime, 
                fastestTime, slowestTime, efficiency);
            
        } catch (Exception e) {
            System.err.println("Error testing " + algorithmName + ": " + e.getMessage());
        }
    }
    
    private static Datacenter createSimpleDatacenter(CloudSim simulation) {
        List<Host> hosts = new ArrayList<>();
        
        // Create 2 hosts with minimal resources
        for (int i = 0; i < 2; i++) {
            List<Pe> peList = Arrays.asList(new PeSimple(1000)); // Single PE per host
            Host host = new HostSimple(2048, 10000, 100000, peList); // 2GB RAM, 10GB storage, 100GB bandwidth
            hosts.add(host);
        }
        
        return new DatacenterSimple(simulation, hosts);
    }
    
    private static List<Vm> createSimpleVMs() {
        List<Vm> vms = new ArrayList<>();
        
        // Create 4 VMs with minimal resources
        for (int i = 0; i < 4; i++) {
            Vm vm = new VmSimple(500, 1); // 500 MIPS, 1 PE
            vm.setRam(256).setBw(1000).setSize(1000); // 256MB RAM, 1GB bandwidth, 1GB storage
            vms.add(vm);
        }
        
        return vms;
    }
    
    private static List<Cloudlet> createSimpleCloudlets() {
        List<Cloudlet> cloudlets = new ArrayList<>();
        Random rand = new Random();
        UtilizationModelFull utilizationModel = new UtilizationModelFull();
        
        for (int i = 0; i < GLOBAL_CLOUDLET_COUNT; i++) {
            long length = 50 + rand.nextInt(50); // Very small cloudlets (50-100 MI)
            Cloudlet cloudlet = new CloudletSimple(length, 1, utilizationModel);
            cloudlet.setFileSize(10).setOutputSize(10); // Very small file sizes
            cloudlets.add(cloudlet);
        }
        
        return cloudlets;
    }
    
    private static double calculateEfficiency(Map<Long, Integer> vmDistribution) {
        if (vmDistribution.isEmpty()) return 100.0;
        
        int[] loads = vmDistribution.values().stream().mapToInt(Integer::intValue).toArray();
        double mean = Arrays.stream(loads).average().orElse(0.0);
        double variance = Arrays.stream(loads)
            .mapToDouble(load -> Math.pow(load - mean, 2))
            .average()
            .orElse(0.0);
        
        return Math.sqrt(variance) / Math.max(mean, 1.0) * 100.0;
    }
}
