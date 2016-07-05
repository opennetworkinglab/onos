/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.provider.nil;

import com.google.common.collect.Lists;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.toHex;
import static org.onosproject.net.HostId.hostId;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.device.DeviceEvent.Type.*;
import static org.onosproject.provider.nil.NullProviders.SCHEME;

/**
 * Abstraction of a provider capable to simulate some network topology.
 */
public abstract class TopologySimulator {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected String[] topoShape;
    protected int deviceCount;
    protected int hostCount;

    protected ServiceDirectory directory;
    protected NodeId localNode;

    protected ClusterService clusterService;
    protected MastershipService mastershipService;

    protected DeviceAdminService deviceService;
    protected HostService hostService;
    protected LinkService linkService;

    protected DeviceProviderService deviceProviderService;
    protected HostProviderService hostProviderService;
    protected LinkProviderService linkProviderService;

    protected int maxWaitSeconds = 1;
    protected int infrastructurePorts = 2;
    protected CountDownLatch deviceLatch;

    protected final List<DeviceId> deviceIds = Lists.newArrayList();

    private DeviceListener deviceEventCounter = new DeviceEventCounter();

    /**
     * Initializes a new topology simulator with access to the specified service
     * directory and various provider services.
     *
     * @param topoShape             topology shape specifier
     * @param deviceCount           number of devices in the topology
     * @param hostCount             number of hosts per device
     * @param directory             service directory
     * @param deviceProviderService device provider service
     * @param hostProviderService   host provider service
     * @param linkProviderService   link provider service
     */
    protected void init(String topoShape, int deviceCount, int hostCount,
                        ServiceDirectory directory,
                        DeviceProviderService deviceProviderService,
                        HostProviderService hostProviderService,
                        LinkProviderService linkProviderService) {
        this.deviceCount = deviceCount;
        this.hostCount = hostCount;
        this.directory = directory;

        this.clusterService = directory.get(ClusterService.class);
        this.mastershipService = directory.get(MastershipService.class);

        this.deviceService = directory.get(DeviceAdminService.class);
        this.hostService = directory.get(HostService.class);
        this.linkService = directory.get(LinkService.class);
        this.deviceProviderService = deviceProviderService;
        this.hostProviderService = hostProviderService;
        this.linkProviderService = linkProviderService;

        localNode = clusterService.getLocalNode().id();

        processTopoShape(topoShape);
    }

    /**
     * Processes the topology shape specifier.
     *
     * @param shape topology shape specifier
     */
    protected void processTopoShape(String shape) {
        this.topoShape = shape.split(",");
    }

    /**
     * Sets up network topology simulation.
     */
    public void setUpTopology() {
        prepareForDeviceEvents(deviceCount);
        createDevices();
        waitForDeviceEvents();

        createLinks();
        createHosts();
    }

    /**
     * Creates simulated devices.
     */
    protected void createDevices() {
        for (int i = 0; i < deviceCount; i++) {
            createDevice(i + 1);
        }
    }

    /**
     * Creates simulated links.
     */
    protected abstract void createLinks();

    /**
     * Creates simulated hosts.
     */
    protected abstract void createHosts();

    /**
     * Creates simulated device and adds its id to the list of devices ids.
     *
     * @param i index of the device id in the list.
     */
    protected void createDevice(int i) {
        DeviceId id = DeviceId.deviceId(SCHEME + ":" + toHex(i));
        deviceIds.add(id);
        createDevice(id, i);
    }

    /**
     * Creates simulated device.
     *
     * @param id        device identifier
     * @param chassisId chassis identifier number
     */
    public void createDevice(DeviceId id, int chassisId) {
        createDevice(id, chassisId, Device.Type.SWITCH, hostCount + infrastructurePorts);
    }

    /**
     * Creates simulated device.
     *
     * @param id        device identifier
     * @param chassisId chassis identifier number
     * @param type      device type
     * @param portCount number of device ports
     */
    public void createDevice(DeviceId id, int chassisId, Device.Type type, int portCount) {
        DeviceDescription desc =
                new DefaultDeviceDescription(id.uri(), type,
                                             "ON.Lab", "0.1", "0.1", "1234",
                                             new ChassisId(chassisId));
        deviceProviderService.deviceConnected(id, desc);
        deviceProviderService.updatePorts(id, buildPorts(portCount));
    }

    /**
     * Creates simulated link between two devices.
     *
     * @param i  index of one simulated device
     * @param j  index of another simulated device
     * @param pi port number of i-th device
     * @param pj port number of j-th device
     */
    public void createLink(int i, int j, int pi, int pj) {
        ConnectPoint one = new ConnectPoint(deviceIds.get(i), PortNumber.portNumber(pi));
        ConnectPoint two = new ConnectPoint(deviceIds.get(j), PortNumber.portNumber(pj));
        createLink(one, two);
    }

    /**
     * Creates simulated link between two connection points.
     *
     * @param one one connection point
     * @param two another connection point
     */
    public void createLink(ConnectPoint one, ConnectPoint two) {
        createLink(one, two, DIRECT, true);
    }

    /**
     * Creates simulated link between two connection points.
     *
     * @param one             one connection point
     * @param two             another connection point
     * @param type            link type
     * @param isBidirectional true if link is bidirectional
     */
    public void createLink(ConnectPoint one, ConnectPoint two, Link.Type type, boolean isBidirectional) {
        linkProviderService.linkDetected(new DefaultLinkDescription(one, two, type));
        if (isBidirectional) {
            linkProviderService.linkDetected(new DefaultLinkDescription(two, one, type));
        }
    }

    /**
     * Creates simularted hosts for the specified device.
     *
     * @param deviceId   device identifier
     * @param portOffset port offset where to start attaching hosts
     */
    public void createHosts(DeviceId deviceId, int portOffset) {
        String s = deviceId.toString();
        byte dByte = Byte.parseByte(s.substring(s.length() - 2), 16);
        // TODO: this limits the simulation to 256 devices & 256 hosts/device.
        byte[] macBytes = new byte[]{0, 0, 0, 0, dByte, 0};
        byte[] ipBytes = new byte[]{(byte) 192, (byte) 168, dByte, 0};

        for (int i = 0; i < hostCount; i++) {
            int port = portOffset + i + 1;
            macBytes[5] = (byte) (i + 1);
            ipBytes[3] = (byte) (i + 1);
            HostId id = hostId(MacAddress.valueOf(macBytes), VlanId.NONE);
            IpAddress ip = IpAddress.valueOf(IpAddress.Version.INET, ipBytes);
            hostProviderService.hostDetected(id, description(id, ip, deviceId, port), false);
        }
    }

    /**
     * Prepares to count device added/available/removed events.
     *
     * @param count number of events to count
     */
    protected void prepareForDeviceEvents(int count) {
        deviceLatch = new CountDownLatch(count);
        deviceService.addListener(deviceEventCounter);
    }

    /**
     * Waits for all expected device added/available/removed events.
     */
    protected void waitForDeviceEvents() {
        try {
            deviceLatch.await(maxWaitSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Device events did not arrive in time");
        }
        deviceService.removeListener(deviceEventCounter);
    }


    /**
     * Tears down network topology simulation.
     */
    public void tearDownTopology() {
        removeHosts();
        removeLinks();
        removeDevices();
    }

    /**
     * Removes any hosts previously advertised by this provider.
     */
    protected void removeHosts() {
        hostService.getHosts()
                .forEach(host -> hostProviderService.hostVanished(host.id()));
    }

    /**
     * Removes any links previously advertised by this provider.
     */
    protected void removeLinks() {
        linkService.getLinks()
                .forEach(link -> linkProviderService.linkVanished(description(link)));
    }

    /**
     * Removes any devices previously advertised by this provider.
     */
    protected void removeDevices() {
        prepareForDeviceEvents(deviceIds.size());
        deviceIds.forEach(deviceProviderService::deviceDisconnected);
        waitForDeviceEvents();
        deviceIds.clear();
    }


    /**
     * Produces a device description from the given device.
     *
     * @param device device to copy
     * @return device description
     */
    static DeviceDescription description(Device device) {
        return new DefaultDeviceDescription(device.id().uri(), device.type(),
                                            device.manufacturer(),
                                            device.hwVersion(), device.swVersion(),
                                            device.serialNumber(), device.chassisId());
    }

    /**
     * Produces a link description from the given link.
     *
     * @param link link to copy
     * @return link description
     */
    static DefaultLinkDescription description(Link link) {
        return new DefaultLinkDescription(link.src(), link.dst(), link.type());
    }

    /**
     * Produces a host description from the given host.
     *
     * @param host host to copy
     * @return host description
     */
    static DefaultHostDescription description(Host host) {
        return new DefaultHostDescription(host.mac(), host.vlan(), host.location(),
                                          host.ipAddresses());
    }

    /**
     * Generates a host description from the given id and location information.
     *
     * @param hostId   host identifier
     * @param ip       host IP
     * @param deviceId edge device
     * @param port     edge port
     * @return host description
     */
    static HostDescription description(HostId hostId, IpAddress ip,
                                       DeviceId deviceId, int port) {
        HostLocation location = new HostLocation(deviceId, portNumber(port), 0L);
        return new DefaultHostDescription(hostId.mac(), hostId.vlanId(), location, ip);
    }

    /**
     * Generates a list of a configured number of ports.
     *
     * @param portCount number of ports
     * @return list of ports
     */
    protected List<PortDescription> buildPorts(int portCount) {
        List<PortDescription> ports = Lists.newArrayList();
        for (int i = 1; i <= portCount; i++) {
            ports.add(new DefaultPortDescription(PortNumber.portNumber(i), true,
                                                 Port.Type.COPPER, 0));
        }
        return ports;
    }

    /**
     * Indicates whether or not the simulation deeps the device as available.
     *
     * @param deviceId device identifier
     * @return true if device is known
     */
    public boolean contains(DeviceId deviceId) {
        return deviceIds.contains(deviceId);
    }

    // Counts down number of device added/available/removed events.
    private class DeviceEventCounter implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceEvent.Type type = event.type();
            if (type == DEVICE_ADDED || type == DEVICE_REMOVED ||
                    type == DEVICE_AVAILABILITY_CHANGED) {
                deviceLatch.countDown();
            }
        }
    }
}
