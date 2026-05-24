package org.cloudsimplus.abc;

import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeastConnectionBroker extends DatacenterBrokerSimple {
    private final Map<Long, Integer> vmConnectionCount = new HashMap<>();

    public LeastConnectionBroker(final CloudSim simulation) {
        super(simulation);
    }

    public LeastConnectionBroker(final CloudSim simulation, final String name) {
        super(simulation, name);
    }

    protected void submitCloudletList() {
        System.out.println("Least Connection Broker: Starting load balancing...");
        initializeConnectionCounts();
        submitCloudletsLeastConnection();
    }

    private void initializeConnectionCounts() {
        vmConnectionCount.clear();
        for (Vm vm : getVmCreatedList()) {
            vmConnectionCount.put(vm.getId(), 0);
        }
    }

    private void submitCloudletsLeastConnection() {
        List<Cloudlet> cloudletList = getCloudletSubmittedList();
        List<Vm> vmList = getVmCreatedList();

        if (cloudletList.isEmpty() || vmList.isEmpty()) {
            System.out.println("No cloudlets or VMs available.");
            return;
        }

        System.out.println("Submitting " + cloudletList.size() + " cloudlets using Least Connection...");

        for (Cloudlet cloudlet : cloudletList) {
            Vm selectedVm = getVmWithLeastConnections();
            bindCloudletToVm(cloudlet, selectedVm);
            incrementConnectionCount(selectedVm.getId());
        }

        super.submitCloudletList(cloudletList);
        System.out.println("Submitted " + cloudletList.size() + " cloudlets");
        System.out.println("Final connection counts: " + vmConnectionCount);
    }

    private Vm getVmWithLeastConnections() {
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
}
