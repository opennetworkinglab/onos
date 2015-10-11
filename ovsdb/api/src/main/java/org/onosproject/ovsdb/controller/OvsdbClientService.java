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
     * Creates the configuration for the tunnel.
     *
     * @param srcIp source IP address
     * @param dstIp destination IP address
     */
    void createTunnel(IpAddress srcIp, IpAddress dstIp);

    /**
     * Drops the configuration for the tunnel.
     *
     * @param srcIp source IP address
     * @param dstIp destination IP address
     */
    void dropTunnel(IpAddress srcIp, IpAddress dstIp);

    /**
     * Gets tunnels of the node.
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
     * Drops a bridge.
     *
     * @param bridgeName bridge name
     */
    void dropBridge(String bridgeName);

    /**
     * Gets bridges of the node.
     *
     * @return set of bridges; empty if no bridge is find
     */
    Set<OvsdbBridge> getBridges();

    /**
     * Gets controllers of the node.
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
     * Gets ports of the bridge.
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
     * Gets the Ovs uuid.
     *
     * @param dbName database name
     * @return ovs uuid, empty if no uuid is find
     */
    String getOvsUuid(String dbName);

    /**
     * Gets the ovsdb database schema.
     *
     * @param dbName database name
     * @return database schema
     */
    ListenableFuture<DatabaseSchema> getOvsdbSchema(String dbName);

    /**
     * Gets the ovsdb table updates.
     *
     * @param dbName database name
     * @param id     random uuid
     * @return table updates
     */
    ListenableFuture<TableUpdates> monitorTables(String dbName, String id);

    /**
     * Gets the ovsdb config operation result.
     *
     * @param dbName     database name
     * @param operations the list of operations
     * @return operation results
     */
    ListenableFuture<List<OperationResult>> transactConfig(String dbName,
                                                           List<Operation> operations);

    /**
     * Gets the ovsdb database schema from local.
     *
     * @param dbName database name
     * @return database schema
     */
    DatabaseSchema getDatabaseSchema(String dbName);

    /**
     * Gets the ovsdb row from the local ovsdb store.
     *
     * @param dbName    database name
     * @param tableName table name
     * @param uuid      row uuid
     * @return row ovsdb row
     */
    Row getRow(String dbName, String tableName, String uuid);

    /**
     * Removes the ovsdb row from the local ovsdb store.
     *
     * @param dbName    database name
     * @param tableName table name
     * @param uuid      row uuid
     */
    void removeRow(String dbName, String tableName, String uuid);

    /**
     * Updates the local ovsdb store.
     *
     * @param dbName    database name
     * @param tableName table name
     * @param uuid      row uuid
     * @param row       ovsdb row
     */
    void updateOvsdbStore(String dbName, String tableName, String uuid, Row row);

    /**
     * Gets ovsdb local ports.
     *
     * @param ifaceids the ifaceid that needed
     * @return ovsdb ports
     */
    Set<OvsdbPort> getLocalPorts(Iterable<String> ifaceids);

    /**
     * Disconnects the ovsdb server.
     */
    void disconnect();
}
