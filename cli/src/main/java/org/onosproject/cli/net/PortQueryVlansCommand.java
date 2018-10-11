/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.VlanQuery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * Command to show the list of unused vlan-ids.
 */
@Service
@Command(scope = "onos", name = "port-query-vlans",
        description = "Lists all unused VLAN-IDs on port")
public class PortQueryVlansCommand extends AbstractShellCommand {

    private static final String AVAIL_VLANS = "VLAN-ID: %s";
    private static final String VLAN_NOT_AVAILABLE = "No unused VLAN-ID";
    private static final String FMT = "port=%s, state=%s, type=%s, speed=%s%s";
    private static final String NO_SUPPORT = "Device not supporting VLAN-ID retrieval";
    private static final String FAILURE = "Failed to retrieve VLAN information: ";

    @Argument(index = 0, name = "port",
            description = "Port Description",
            required = true, multiValued = true)
    @Completion(ConnectPointCompleter.class)
    private String[] ports;


    @Override
    protected void doExecute() {
        DeviceService service = get(DeviceService.class);
        for (String portStr : ports) {
            ConnectPoint connectPoint = ConnectPoint.deviceConnectPoint(portStr);
            Port port = service.getPort(connectPoint.deviceId(), connectPoint.port());
            printPort(port);
            printVlans(port);
        }
    }

    private void printPort(Port port) {
        String portName = portName(port.number());
        Object portIsEnabled = port.isEnabled() ? "enabled" : "disabled";
        String portType = port.type().toString().toLowerCase();
        String annotations = annotations(port.annotations());
        print(FMT, portName, portIsEnabled, portType, port.portSpeed(), annotations);
    }

    private String portName(PortNumber port) {
        return port.equals(PortNumber.LOCAL) ? "local" : port.toString();
    }

    private void printVlans(Port port) {
        DeviceService deviceService = get(DeviceService.class);
        DriverService driverService = get(DriverService.class);

        DeviceId deviceId = (DeviceId) port.element().id();
        Device device = deviceService.getDevice(deviceId);

        if (!device.is(VlanQuery.class)) {
            // The relevant behavior is not supported by the device.
            print(NO_SUPPORT);
            return;
        }

        DriverHandler h = driverService.createHandler(deviceId);
        VlanQuery vlanQuery = h.behaviour(VlanQuery.class);

        try {
            Set<VlanId> vlanIds = vlanQuery.queryVlanIds(port.number());

            if (vlanIds.isEmpty()) {
                print(VLAN_NOT_AVAILABLE);
            } else {
                print(AVAIL_VLANS, getRanges(vlanIds).toString());
            }
        } catch (Exception e) {
            print(FAILURE + e.getMessage());
        }
    }

    private static ArrayList getRanges(Set<VlanId> vlans) {
        short i = 0;
        short[] vlanArray = new short[vlans.size()];
        for (VlanId vlanId : vlans) {
            vlanArray[i++] = vlanId.toShort();
        }
        Arrays.sort(vlanArray);

        ArrayList ranges = new ArrayList();
        short rStart = 0;
        short rEnd = 0;

        for (i = 2; i < vlanArray.length; i++) {
            if (vlanArray[i] == vlanArray[i - 1] + 1 &&
                    vlanArray[i] == vlanArray[i - 2] + 2) {
                // Three consecutive VLAN-IDs found, so range exists.
                if (rEnd == vlanArray[i - 1]) {
                    // Range already exists, so step the end.
                    rEnd = vlanArray[i];
                } else {
                    // Setup a new range.
                    rStart = vlanArray[i - 2];
                    rEnd = vlanArray[i];
                }
            } else {
                // Not in a range.
                if (rEnd == vlanArray[i - 1]) {
                    // Previous range is discontinued and is stored.
                    ranges.add(rStart + "-" + rEnd);
                } else {
                    // No previous range.
                    if (vlanArray[i] != vlanArray[i - 1] + 1) {
                        // Current VLAN-ID is not 2nd consecutive.
                        if (vlanArray[i - 1] == vlanArray[i - 2] + 1) {
                            // The 2 previous VLAN-IDs were consequetive, so 2nd
                            // last is stored separately.
                            ranges.add(vlanArray[i - 2]);
                        }
                        // Previous is stored, when current is not consequetive.
                        ranges.add(vlanArray[i - 1]);
                    }
                }
            }
        }
        if (rEnd == vlanArray[vlanArray.length - 1]) {
            // Array finished with a range.
            ranges.add(rStart + "-" + rEnd);
        } else {
            if (vlanArray[vlanArray.length - 1] == vlanArray[vlanArray.length - 2] + 1) {
                // Previous is stored, when current is consequetive.
                ranges.add(vlanArray[vlanArray.length - 2]);
            }
            // Last item is stored
            ranges.add(vlanArray[vlanArray.length - 1]);
        }
        return ranges;
    }
}
