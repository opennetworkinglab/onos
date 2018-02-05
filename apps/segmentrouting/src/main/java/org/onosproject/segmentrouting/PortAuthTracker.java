/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.segmentrouting;


import com.google.common.base.Objects;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.segmentrouting.config.BlockedPortsConfig;
import org.onosproject.utils.Comparators;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Keeps track of ports that have been configured for blocking,
 * and their current authentication state.
 */
public class PortAuthTracker {

    private static final Logger log = getLogger(PortAuthTracker.class);

    private Map<DeviceId, Map<PortNumber, BlockState>> blockedPorts = new HashMap<>();
    private Map<DeviceId, Map<PortNumber, BlockState>> oldMap;

    @Override
    public String toString() {
        return "PortAuthTracker{entries = " + blockedPorts.size() + "}";
    }

    /**
     * Changes the state of the given device id / port number pair to the
     * specified state.
     *
     * @param d        device identifier
     * @param p        port number
     * @param newState the updated state
     * @return true, if the state changed from what was previously mapped
     */
    private boolean changeStateTo(DeviceId d, PortNumber p, BlockState newState) {
        Map<PortNumber, BlockState> portMap =
                blockedPorts.computeIfAbsent(d, k -> new HashMap<>());
        BlockState oldState =
                portMap.computeIfAbsent(p, k -> BlockState.UNCHECKED);
        portMap.put(p, newState);
        return (oldState != newState);
    }

    /**
     * Radius has authorized the supplicant at this connect point. If
     * we are tracking this port, clear the blocking flow and mark the
     * port as authorized.
     *
     * @param connectPoint supplicant connect point
     */
    void radiusAuthorize(ConnectPoint connectPoint) {
        DeviceId d = connectPoint.deviceId();
        PortNumber p = connectPoint.port();
        if (configured(d, p)) {
            clearBlockingFlow(d, p);
            markAsAuthenticated(d, p);
        }
    }

    /**
     * Supplicant at specified connect point has logged off Radius. If
     * we are tracking this port, install a blocking flow and mark the
     * port as blocked.
     *
     * @param connectPoint supplicant connect point
     */
    void radiusLogoff(ConnectPoint connectPoint) {
        DeviceId d = connectPoint.deviceId();
        PortNumber p = connectPoint.port();
        if (configured(d, p)) {
            installBlockingFlow(d, p);
            markAsBlocked(d, p);
        }
    }

    /**
     * Marks the specified device/port as blocked.
     *
     * @param d device id
     * @param p port number
     * @return true if the state changed (was not already blocked)
     */
    private boolean markAsBlocked(DeviceId d, PortNumber p) {
        return changeStateTo(d, p, BlockState.BLOCKED);
    }

    /**
     * Marks the specified device/port as authenticated.
     *
     * @param d device id
     * @param p port number
     * @return true if the state changed (was not already authenticated)
     */
    private boolean markAsAuthenticated(DeviceId d, PortNumber p) {
        return changeStateTo(d, p, BlockState.AUTHENTICATED);
    }

    /**
     * Returns true if the given device/port are configured for blocking.
     *
     * @param d device id
     * @param p port number
     * @return true if this device/port configured for blocking
     */
    private boolean configured(DeviceId d, PortNumber p) {
        Map<PortNumber, BlockState> portMap = blockedPorts.get(d);
        return portMap != null && portMap.get(p) != null;
    }

    private BlockState whatState(DeviceId d, PortNumber p,
                                 Map<DeviceId, Map<PortNumber, BlockState>> m) {
        Map<PortNumber, BlockState> portMap = m.get(d);
        if (portMap == null) {
            return BlockState.UNCHECKED;
        }
        BlockState state = portMap.get(p);
        if (state == null) {
            return BlockState.UNCHECKED;
        }
        return state;
    }

    /**
     * Returns the current state of the given device/port.
     *
     * @param d device id
     * @param p port number
     * @return current block-state
     */
    BlockState currentState(DeviceId d, PortNumber p) {
        return whatState(d, p, blockedPorts);
    }

    /**
     * Returns the current state of the given connect point.
     *
     * @param cp connect point
     * @return current block-state
     */

    BlockState currentState(ConnectPoint cp) {
        return whatState(cp.deviceId(), cp.port(), blockedPorts);
    }

    /**
     * Returns the number of entries being tracked.
     *
     * @return the number of tracked entries
     */
    int entryCount() {
        int count = 0;
        for (Map<PortNumber, BlockState> m : blockedPorts.values()) {
            count += m.size();
        }
        return count;
    }

    /**
     * Returns the previously recorded state of the given device/port.
     *
     * @param d device id
     * @param p port number
     * @return previous block-state
     */
    private BlockState oldState(DeviceId d, PortNumber p) {
        return whatState(d, p, oldMap);
    }

    private void configurePort(DeviceId d, PortNumber p) {
        boolean alreadyAuthenticated =
                oldState(d, p) == BlockState.AUTHENTICATED;

        if (alreadyAuthenticated) {
            clearBlockingFlow(d, p);
            markAsAuthenticated(d, p);
        } else {
            installBlockingFlow(d, p);
            markAsBlocked(d, p);
        }
        log.info("Configuring port {}/{} as {}", d, p,
                 alreadyAuthenticated ? "AUTHENTICATED" : "BLOCKED");
    }

    private boolean notInMap(DeviceId deviceId, PortNumber portNumber) {
        Map<PortNumber, BlockState> m = blockedPorts.get(deviceId);
        return m == null || m.get(portNumber) == null;
    }

    private void logPortsNoLongerBlocked() {
        for (Map.Entry<DeviceId, Map<PortNumber, BlockState>> entry :
                oldMap.entrySet()) {
            DeviceId d = entry.getKey();
            Map<PortNumber, BlockState> portMap = entry.getValue();

            for (PortNumber p : portMap.keySet()) {
                if (notInMap(d, p)) {
                    clearBlockingFlow(d, p);
                    log.info("De-configuring port {}/{} (UNCHECKED)", d, p);
                }
            }
        }
    }


    /**
     * Reconfigures the port tracker using the supplied configuration.
     *
     * @param cfg the new configuration
     */
    void configurePortBlocking(BlockedPortsConfig cfg) {
        // remember the old map; prepare a new map
        oldMap = blockedPorts;
        blockedPorts = new HashMap<>();

        // for each configured device, add configured ports to map
        for (String devId : cfg.deviceIds()) {
            cfg.portIterator(devId)
                    .forEachRemaining(p -> configurePort(deviceId(devId),
                                                         portNumber(p)));
        }

        // have we de-configured any ports?
        logPortsNoLongerBlocked();

        // allow old map to be garbage collected
        oldMap = null;
    }

    private List<PortAuthState> reportPortsAuthState() {
        List<PortAuthState> result = new ArrayList<>();

        for (Map.Entry<DeviceId, Map<PortNumber, BlockState>> entry :
                blockedPorts.entrySet()) {
            DeviceId d = entry.getKey();
            Map<PortNumber, BlockState> portMap = entry.getValue();

            for (PortNumber p : portMap.keySet()) {
                result.add(new PortAuthState(d, p, portMap.get(p)));
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Installs a "blocking" flow for device/port specified.
     *
     * @param d device id
     * @param p port number
     */
    void installBlockingFlow(DeviceId d, PortNumber p) {
        log.debug("Installing Blocking Flow at {}/{}", d, p);
        // TODO: invoke SegmentRoutingService.block(...) appropriately
        log.info("TODO >> Installing Blocking Flow at {}/{}", d, p);
    }

    /**
     * Removes the "blocking" flow from device/port specified.
     *
     * @param d device id
     * @param p port number
     */
    void clearBlockingFlow(DeviceId d, PortNumber p) {
        log.debug("Clearing Blocking Flow from {}/{}", d, p);
        // TODO: invoke SegmentRoutingService.block(...) appropriately
        log.info("TODO >> Clearing Blocking Flow from {}/{}", d, p);
    }


    /**
     * Designates the state of a given port. One of:
     * <ul>
     * <li> UNCHECKED: not configured for blocking </li>
     * <li> BLOCKED: configured for blocking, and not yet authenticated </li>
     * <li> AUTHENTICATED: configured for blocking, but authenticated </li>
     * </ul>
     */
    public enum BlockState {
        UNCHECKED,
        BLOCKED,
        AUTHENTICATED
    }

    /**
     * A simple DTO binding of device identifier, port number, and block state.
     */
    public static final class PortAuthState implements Comparable<PortAuthState> {
        private final DeviceId d;
        private final PortNumber p;
        private final BlockState s;

        private PortAuthState(DeviceId d, PortNumber p, BlockState s) {
            this.d = d;
            this.p = p;
            this.s = s;
        }

        @Override
        public String toString() {
            return String.valueOf(d) + "/" + p + " -- " + s;
        }

        @Override
        public int compareTo(PortAuthState o) {
            // NOTE: only compare against "deviceid/port"
            int result = Comparators.ELEMENT_ID_COMPARATOR.compare(d, o.d);
            return (result != 0) ? result : Long.signum(p.toLong() - o.p.toLong());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PortAuthTracker.PortAuthState that = (PortAuthTracker.PortAuthState) obj;

            return Comparators.ELEMENT_ID_COMPARATOR.compare(this.d, that.d) == 0 &&
                    p.toLong() == that.p.toLong();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.d, this.p);
        }
    }
}
