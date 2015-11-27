/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.ovsdb.rfc.jsonrpc.OvsdbRPC;
import org.onosproject.ovsdb.rfc.message.OperationResult;
import org.onosproject.ovsdb.rfc.message.TableUpdates;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.UUID;
import org.onosproject.ovsdb.rfc.operations.Operation;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents to provider facing side of a node.
 */
public interface OvsdbClientService extends OvsdbRPC {
    /**
     * Gets the node identifier.
     *
     * @return node identifier
     */
    OvsdbNodeId nodeId();

    /**
     * Creates the configuration for tunnel.
     *
     * @param srcIp source IP address
     * @param dstIp destination IP address
     */
    @Deprecated
    void createTunnel(IpAddress srcIp, IpAddress dstIp);

    /**
     * Creates a tunnel port with given options.
     *
     * @param bridgeName bridge name
     * @param portName port name
     * @param tunnelType tunnel type
     * @param options tunnel options
     * @return true if tunnel creation is successful, false otherwise
     */
    boolean createTunnel(String bridgeName, String portName, String tunnelType, Map<String, String> options);

    /**
     * Drops the configuration for tunnel.
     *
     * @param srcIp source IP address
     * @param dstIp destination IP address
     */
    void dropTunnel(IpAddress srcIp, IpAddress dstIp);

    /**
     * Gets tunnels of node.
     *
     * @return set of tunnels; empty if no tunnel is find
     */
    Set<OvsdbTunnel> getTunnels();

    /**
     * Creates a bridge.
     *
     * @param bridgeName bridge name
     */
    void createBridge(String bridgeName);

    /**
     * Creates a bridge.
     *
     * @param bridgeName bridge name
     * @param dpid data path id
     * @param exPortName external port name
     */
    void createBridge(String bridgeName, String dpid, String exPortName);

    /**
     * Creates a bridge with given name and dpid.
     * Sets the bridge's controller with given controllers.
     *
     * @param bridgeName bridge name
     * @param dpid data path id
     * @param controllers controllers
     * @return true if bridge creation is successful, false otherwise
     */
    boolean createBridge(String bridgeName, String dpid, List<ControllerInfo> controllers);

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
     * Sets the Controllers for the specified bridge.
     * <p>
     * This method will replace the existing controller list with the new controller
     * list.
     *
     * @param bridgeUuid bridge uuid
     * @param controllers list of controllers
     */
    void setControllersWithUUID(UUID bridgeUuid, List<ControllerInfo> controllers);

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
     * Gets the Interface uuid.
     *
     * @param portUuid port uuid
     * @param portName port name
     * @return interface uuid, empty if no uuid is find
     */
    String getInterfaceUuid(String portUuid, String portName);

    /**
     * Gets the Controller uuid.
     *
     * @param controllerName   controller name
     * @param controllerTarget controller target
     * @return controller uuid, empty if no uuid is find
     */
    String getControllerUuid(String controllerName, String controllerTarget);

    /**
     * Gets the OVS uuid.
     *
     * @param dbName database name
     * @return ovs uuid, empty if no uuid is find
     */
    String getOvsUuid(String dbName);

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
     * Gets the OVSDB config operation result.
     *
     * @param dbName     database name
     * @param operations the list of operations
     * @return operation results
     */
    ListenableFuture<List<OperationResult>> transactConfig(String dbName,
                                                           List<Operation> operations);

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
