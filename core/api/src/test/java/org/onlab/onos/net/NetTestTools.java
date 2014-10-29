/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.net;

import org.onlab.onos.net.provider.ProviderId;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.HostId.hostId;
import static org.onlab.onos.net.PortNumber.portNumber;
import static org.onlab.packet.MacAddress.valueOf;
import static org.onlab.packet.VlanId.vlanId;

/**
 * Miscellaneous tools for testing core related to the network model.
 */
public final class NetTestTools {

    private NetTestTools() {
    }

    public static final ProviderId PID = new ProviderId("of", "foo");

    // Short-hand for producing a device id from a string
    public static DeviceId did(String id) {
        return deviceId("of:" + id);
    }


    // Short-hand for producing a host id from a string
    public static HostId hid(String id) {
        return hostId(id);
    }

    // Crates a new device with the specified id
    public static Device device(String id) {
        return new DefaultDevice(PID, did(id), Device.Type.SWITCH,
                                 "mfg", "1.0", "1.1", "1234", new ChassisId());
    }

    // Crates a new host with the specified id
    public static Host host(String id, String did) {
        return new DefaultHost(PID, hid(id), valueOf(1234), vlanId((short) 2),
                               new HostLocation(did(did), portNumber(1), 321),
                               new HashSet<IpAddress>());
    }

    // Short-hand for creating a connection point.
    public static ConnectPoint connectPoint(String id, int port) {
        return new ConnectPoint(did(id), portNumber(port));
    }

    // Short-hand for creating a link.
    public static Link link(String src, int sp, String dst, int dp) {
        return new DefaultLink(PID,
                               connectPoint(src, sp),
                               connectPoint(dst, dp),
                               Link.Type.DIRECT);
    }

    // Creates a path that leads through the given devices.
    public static Path createPath(String... ids) {
        List<Link> links = new ArrayList<>();
        for (int i = 0; i < ids.length - 1; i++) {
            links.add(link(ids[i], i, ids[i + 1], i));
        }
        return new DefaultPath(PID, links, ids.length);
    }

}
