/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.DeviceCpuStats;
import org.onosproject.net.behaviour.DeviceMemoryStats;
import org.onosproject.net.behaviour.MirroringName;
import org.onosproject.net.behaviour.MirroringStatistics;
import org.onosproject.net.behaviour.QosId;
import org.onosproject.net.behaviour.QueueDescription;
import org.onosproject.net.behaviour.QueueId;
import org.onosproject.ovsdb.rfc.jsonrpc.OvsdbRpc;
import org.onosproject.ovsdb.rfc.message.TableUpdates;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.table.Interface;
import org.onosproject.ovsdb.rfc.table.OvsdbTable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
     * @param bridgeName the name of the bridge
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
     * @param mirroringName name of mirror to drop
     */
    void dropMirror(MirroringName mirroringName);

    /**
     * apply qos to port.
     *
     * @param  portNumber port identifier
     * @param  qosName the qos name
     */
    void applyQos(PortNumber portNumber, String qosName);

    /**
     * Creates a qos port.
     *
     * @param  portNumber port identifier
     */
    void removeQos(PortNumber portNumber);

    /**
     * Creates a qos. associates with queue to
     * provide the ability of limit the rate of different flows
     * depend on itself priority.
     *
     * @param  ovsdbQos the OVSDB Qos
     * @return true if qos creation is successful, false otherwise
     */
    boolean createQos(OvsdbQos ovsdbQos);

    /**
     * Drops the configuration for qos.
     *
     * @param qosId qos identifier
     */
    void dropQos(QosId qosId);

    /**
     * Gets a qos of node.
     * @param qosId qos identifier
     * @return null if no qos is found
     */
    OvsdbQos getQos(QosId qosId);

    /**
     * Gets qoses of node.
     *
     * @return set of qoses; empty if no qos is found
     */
    Set<OvsdbQos> getQoses();

    /**
     * Bind Queue to QoS.
     *
     * @param qosId qos identifier
     * @param queues the Queue key and Queue description
     */
    void bindQueues(QosId qosId, Map<Long, QueueDescription> queues);

    /**
     * Unbind Queue from QoS.
     *
     * @param qosId qos identifier
     * @param queueKeys queue key
     */
    void unbindQueues(QosId qosId, List<Long> queueKeys);

    /**
     * Creates queues. limits the rate of each flow
     * depend on itself priority.
     *
     * @param  queue the OVSDB queue description
     * @return true if queue creation is successful, false otherwise
     */
    boolean createQueue(OvsdbQueue queue);

    /**
     * Drops the configuration for queue.
     *
     * @param queueId  queue identifier
     */
    void dropQueue(QueueId queueId);

    /**
     * Gets a queue of node.
     * @param queueId the queue identifier
     * @return null if no queue is found
     */
    OvsdbQueue getQueue(QueueId queueId);

    /**
     * Gets queues of node.
     *
     * @return set of queues; empty if no queue is found
     */
    Set<OvsdbQueue> getQueues();

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
     * @return set of bridges; empty if no bridge is found
     */
    Set<OvsdbBridge> getBridges();

    /**
     * Gets controllers of node.
     *
     * @param openflowDeviceId target device id
     * @return set of controllers; empty if no controller is found
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
     * @return set of ports; empty if no ports is found
     */
    Set<OvsdbPort> getPorts();

    /**
     * Gets interfaces of bridge.
     *
     * @return set of interfaces; empty if no interface is found
     */
    Set<Interface> getInterfaces();

    /**
     * Gets the interface with given portName.
     *
     * @param intf interface name
     * @return interface
     */
    Interface getInterface(String intf);

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
     * @return bridge uuid, empty if no uuid is found
     */
    String getBridgeUuid(String bridgeName);

    /**
     * Gets the Port uuid.
     *
     * @param portName   port name
     * @param bridgeUuid bridge uuid
     * @return port uuid, empty if no uuid is found
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
     * @return row, empty if no row is found
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

    /**
     * Gets created  ports for the particular bridgeId.
     *
     * @param portNames  the portNames which needs to checked for create
     * @param bridgeId   bridgeIdentifier
     * @return OvsdbPortNames  the created ports from port table for the bridgeId by considering input port list.
     * Considered port as created if port's interface table also gets created,irrespective
     * of ofport value(has errors or not)
     */
    List<OvsdbPortName> getPorts(List<String> portNames, DeviceId bridgeId);

    /**
     * Gets error status for the given portNames.
     *
     * @param portNames  the portNames which need to be checked for errors
     * @param bridgeId   bridgeIdentifier
     * @return errorstatus true if input port list contains error, false otherwise
     */
    boolean getPortError(List<OvsdbPortName>  portNames, DeviceId bridgeId);

    /**
     * Gets First row for the given table of given DB.
     *
     * @param dbName  db name
     * @param tblName table name
     * @return firstRow, first row of the given table from given db if present
     */
    Optional<Object> getFirstRow(String dbName, OvsdbTable tblName);

    /**
     * Gets device CPU usage in percentage.
     *
     * @return cpuStats, empty data as there is no generic way to fetch such stats
     */
    Optional<DeviceCpuStats> getDeviceCpuUsage();

    /**
     * Gets device memory usage in kilobytes.
     *
     * @return memoryStats, empty data as there is no generic way to fetch such stats
     */
    Optional<DeviceMemoryStats> getDeviceMemoryUsage();

}
