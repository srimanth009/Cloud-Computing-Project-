package org.cloudsimplus.abc;

import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.List;

public class WeightedRoundRobinBroker extends DatacenterBrokerSimple {
    private int currentVmIndex = -1;
    private List<Integer> vmWeights = new ArrayList<>();
    private List<Integer> currentWeights = new ArrayList<>();

    public WeightedRoundRobinBroker(final CloudSim simulation) {
        super(simulation);
    }

    public WeightedRoundRobinBroker(final CloudSim simulation, final String name) {
        super(simulation, name);
    }

    protected void submitCloudletList() {
        System.out.println("Weighted Round Robin Broker: Starting load balancing...");
        initializeWeights();
        submitCloudletsWeightedRoundRobin();
    }

    private void initializeWeights() {
        vmWeights.clear();
        currentWeights.clear();
        List<Vm> vmList = getVmCreatedList();

        for (Vm vm : vmList) {
            int weight = calculateVmWeight(vm);
            vmWeights.add(weight);
            currentWeights.add(weight);
        }

        System.out.println("VM Weights: " + vmWeights);
    }

    private int calculateVmWeight(Vm vm) {
        // Calculate weight based on VM capacity - higher MIPS = higher weight
        double weight = (vm.getMips() * vm.getNumberOfPes()) / 100.0;
        return Math.max(1, (int) weight);
    }

    private void submitCloudletsWeightedRoundRobin() {
        List<Cloudlet> cloudletList = getCloudletSubmittedList();
        List<Vm> vmList = getVmCreatedList();

        if (cloudletList.isEmpty() || vmList.isEmpty()) {
            System.out.println("No cloudlets or VMs available.");
            return;
        }

        System.out.println("Submitting " + cloudletList.size() + " cloudlets using Weighted Round Robin...");

        for (Cloudlet cloudlet : cloudletList) {
            Vm selectedVm = getNextWeightedVm();
            bindCloudletToVm(cloudlet, selectedVm);
        }

        super.submitCloudletList(cloudletList);
        System.out.println("Submitted " + cloudletList.size() + " cloudlets");
    }

    private Vm getNextWeightedVm() {
        List<Vm> vmList = getVmCreatedList();

        while (true) {
            currentVmIndex = (currentVmIndex + 1) % vmList.size();

            if (currentWeights.get(currentVmIndex) > 0) {
                currentWeights.set(currentVmIndex, currentWeights.get(currentVmIndex) - 1);
                return vmList.get(currentVmIndex);
            } else {
                if (currentWeights.stream().allMatch(w -> w == 0)) {
                    for (int i = 0; i < currentWeights.size(); i++) {
                        currentWeights.set(i, vmWeights.get(i));
                    }
                }
            }
        }
    }
}
