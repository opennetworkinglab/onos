package org.onlab.onos.net.host.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.host.HostProvider;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.packet.PacketProvider;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.packet.IpPrefix;
import org.onlab.util.Timer;

public class HostMonitor implements TimerTask {

    private final HostService hostService;
    private final TopologyService topologyService;
    private final DeviceService deviceService;
    private final HostProvider hostProvider;
    private final PacketProvider packetProvider;

    private final Set<IpPrefix> monitoredAddresses;

    private final long probeRate;

    private Timeout timeout;

    public HostMonitor(HostService hostService, TopologyService topologyService,
                       DeviceService deviceService,
                       HostProvider hostProvider, PacketProvider packetProvider) {
        this.hostService = hostService;
        this.topologyService = topologyService;
        this.deviceService = deviceService;
        this.hostProvider = hostProvider;
        this.packetProvider = packetProvider;

        monitoredAddresses = new HashSet<>();

        probeRate = 30000; // milliseconds

        timeout = Timer.getTimer().newTimeout(this, 0, TimeUnit.MILLISECONDS);
    }

    public void addMonitoringFor(IpPrefix ip) {
        monitoredAddresses.add(ip);
    }

    public void stopMonitoring(IpPrefix ip) {
        monitoredAddresses.remove(ip);
    }

    public void shutdown() {
        timeout.cancel();
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        for (IpPrefix ip : monitoredAddresses) {
            Set<Host> hosts = hostService.getHostsByIp(ip);

            if (hosts.isEmpty()) {
                sendArpRequest(ip);
            } else {
                for (Host host : hosts) {
                    hostProvider.triggerProbe(host);
                }
            }
        }

        timeout = Timer.getTimer().newTimeout(this, probeRate, TimeUnit.MILLISECONDS);
    }

    /**
     * Sends an ARP request for the given IP address.
     *
     * @param targetIp IP address to ARP for
     */
    private void sendArpRequest(IpPrefix targetIp) {
        // emit ARP packet out appropriate ports

        // if ip in one of the configured (external) subnets
        //   sent out that port
        // else (ip isn't in any configured subnet)
        //   send out all non-external edge ports

        for (Device device : deviceService.getDevices()) {
            for (Port port : deviceService.getPorts(device.id())) {
                for (IpPrefix ip : port.ipAddresses()) {
                    if (ip.contains(targetIp)) {
                        sendProbe(port, targetIp);
                        continue;
                    }
                }
            }
        }

    }

    private void sendProbe(Port port, IpPrefix targetIp) {

    }
}
