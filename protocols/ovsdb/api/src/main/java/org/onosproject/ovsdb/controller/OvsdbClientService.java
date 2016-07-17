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
package org.onosproject.ovsdb.controller;

import com.google.common.util.concurrent.ListenableFuture;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.MirroringStatistics;
import org.onosproject.net.behaviour.MirroringName;
import org.onosproject.ovsdb.rfc.jsonrpc.OvsdbRpc;
import org.onosproject.ovsdb.rfc.message.TableUpdates;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents to provider facing side of a node.
 */
public interface OvsdbClientService extends OvsdbRpc {
    /**
     * Gets the node identifier.
     *
     * @return node identifier
     */
    OvsdbNodeId nodeId();

    /**
     * Creates a mirror port. Mirrors the traffic
     * that goes to selectDstPort or comes from
     * selectSrcPort or packets containing selectVlan
     * to mirrorPort or to all ports that trunk mirrorVlan.
     *
     * @param  bridgeName the name of the bridge
     * @param mirror the OVSDB mirror description
     * @return true if mirror creation is successful, false otherwise
     */
    boolean createMirror(String bridgeName, OvsdbMirror mirror);

    /**
     * Gets the Mirror uuid.
     *
     * @param mirrorName mirror name
     * @return mirror uuid, empty if no uuid is found
     */
    String getMirrorUuid(String mirrorName);

    /**
     * Gets mirroring statistics of the device.
     *
     * @param deviceId target device id
     * @return set of mirroring statistics; empty if no mirror is found
     */
    Set<MirroringStatistics> getMirroringStatistics(DeviceId deviceId);

    /**
     * Drops the configuration for mirror.
     *
     * @param mirroringName
     */
    void dropMirror(MirroringName mirroringName);

    /**
     * Creates a tunnel port with given options.
     *
     * @deprecated version 1.7.0 - Hummingbird
     * @param bridgeName bridge name
     * @param portName port name
     * @param tunnelType tunnel type
     * @param options tunnel options
     * @return true if tunnel creation is successful, false otherwise
     */
    @Deprecated
    boolean createTunnel(String bridgeName, String portName, String tunnelType,
                         Map<String, String> options);

    /**
     * Drops the configuration for tunnel.
     *
     * @deprecated version 1.7.0 - Hummingbird
     * @param srcIp source IP address
     * @param dstIp destination IP address
     */
    @Deprecated
    void dropTunnel(IpAddress srcIp, IpAddress dstIp);

    /**
     * Creates an interface with a given OVSDB interface description.
     *
     * @param bridgeName bridge name
     * @param ovsdbIface ovsdb interface description
     * @return true if interface creation is successful, false otherwise
     */
    boolean createInterface(String bridgeName, OvsdbInterface ovsdbIface);

    /**
     * Removes an interface with the supplied interface name.
     *
     * @param ifaceName interface name
     * @return true if interface creation is successful, false otherwise
     */
    boolean dropInterface(String ifaceName);

    /**
     * Creates a bridge.
     *
     * @deprecated version 1.7.0 - Hummingbird
     * @param bridgeName bridge name
     */
    @Deprecated
    void createBridge(String bridgeName);

    /**
     * Creates a bridge.
     *
     * @deprecated version 1.7.0 - Hummingbird
     * @param bridgeName bridge name
     * @param dpid data path id
     * @param exPortName external port name
     */
    @Deprecated
    void createBridge(String bridgeName, String dpid, String exPortName);

    /**
     * Creates a bridge with given name and dpid.
     * Sets the bridge's controller with given controllers.
     *
     * @deprecated version 1.7.0 - Hummingbird
     * @param bridgeName bridge name
     * @param dpid data path id
     * @param controllers controllers
     * @return true if bridge creation is successful, false otherwise
     */
    @Deprecated
    boolean createBridge(String bridgeName, String dpid, List<ControllerInfo> controllers);

    /**
     * Creates a bridge with a given bridge description.
     *
     * @param ovsdbBridge ovsdb bridge description
     * @return true if bridge creation is successful, otherwise false
     */
    boolean createBridge(OvsdbBridge ovsdbBridge);

    /**
     * Drops a bridge.
     *
     * @param bridgeName bridge name
     */
    void dropBridge(String bridgeName);

    /**
     * Gets bridges of node.
     *
     * @return set of bridges; empty if no bridge is find
     */
    Set<OvsdbBridge> getBridges();

    /**
     * Gets controllers of node.
     *
     * @param openflowDeviceId target device id
     * @return set of controllers; empty if no controller is find
     */
    Set<ControllerInfo> getControllers(DeviceId openflowDeviceId);

    /**
     * Returns local controller information.
     * The connection is a TCP connection to the local ONOS instance's IP
     * and the default OpenFlow port.
     *
     * @return local controller
     */
    ControllerInfo localController();

    /**
     * Sets the Controllers for the specified device.
     * <p>
     * This method will replace the existing controller list with the new controller
     * list.
     *
     * @param deviceId device id (likely Openflow device)
     * @param controllers list of controllers
     */
    void setControllersWithDeviceId(DeviceId deviceId, List<ControllerInfo> controllers);

    /**
     * Creates a port.
     *
     * @param bridgeName bridge name
     * @param portName   port name
     */
    void createPort(String bridgeName, String portName);

    /**
     * Drops a port.
     *
     * @param bridgeName bridge name
     * @param portName   port name
     */
    void dropPort(String bridgeName, String portName);

    /**
     * Gets ports of bridge.
     *
     * @return set of ports; empty if no ports is find
     */
    Set<OvsdbPort> getPorts();

    /**
     * Checks if the node is still connected.
     *
     * @return true if the node is still connected
     */
    boolean isConnected();

    /**
     * Gets the Bridge uuid.
     *
     * @param bridgeName bridge name
     * @return bridge uuid, empty if no uuid is find
     */
    String getBridgeUuid(String bridgeName);

    /**
     * Gets the Port uuid.
     *
     * @param portName   port name
     * @param bridgeUuid bridge uuid
     * @return port uuid, empty if no uuid is find
     */
    String getPortUuid(String portName, String bridgeUuid);

    /**
     * Gets the OVSDB database schema.
     *
     * @param dbName database name
     * @return database schema
     */
    ListenableFuture<DatabaseSchema> getOvsdbSchema(String dbName);

    /**
     * Gets the OVSDB table updates.
     *
     * @param dbName database name
     * @param id     random uuid
     * @return table updates
     */
    ListenableFuture<TableUpdates> monitorTables(String dbName, String id);

    /**
     * Gets the OVSDB database schema from local.
     *
     * @param dbName database name
     * @return database schema
     */
    DatabaseSchema getDatabaseSchema(String dbName);

    /**
     * Gets the OVSDB row from local OVSDB store.
     *
     * @param dbName    database name
     * @param tableName table name
     * @param uuid      row uuid
     * @return row OVSDB row
     */
    Row getRow(String dbName, String tableName, String uuid);

    /**
     * Removes the OVSDB row from local OVSDB store.
     *
     * @param dbName    database name
     * @param tableName table name
     * @param uuid      row uuid
     */
    void removeRow(String dbName, String tableName, String uuid);

    /**
     * Updates the local OVSDB store.
     *
     * @param dbName    database name
     * @param tableName table name
     * @param uuid      row uuid
     * @param row       OVSDB row
     */
    void updateOvsdbStore(String dbName, String tableName, String uuid, Row row);

    /**
     * Gets OVSDB local ports.
     *
     * @param ifaceids the ifaceid that needed
     * @return OVSDB ports
     */
    Set<OvsdbPort> getLocalPorts(Iterable<String> ifaceids);

    /**
     * Disconnects the OVSDB server.
     */
    void disconnect();
}
