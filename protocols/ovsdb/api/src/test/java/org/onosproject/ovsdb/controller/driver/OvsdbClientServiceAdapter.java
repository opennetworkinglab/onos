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

package org.onosproject.ovsdb.controller.driver;

import java.util.List;
import java.util.Set;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.MirroringName;
import org.onosproject.net.behaviour.MirroringStatistics;
import org.onosproject.net.behaviour.QosId;
import org.onosproject.net.behaviour.QueueId;
import org.onosproject.ovsdb.controller.OvsdbBridge;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbInterface;
import org.onosproject.ovsdb.controller.OvsdbMirror;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbPort;
import org.onosproject.ovsdb.controller.OvsdbQos;
import org.onosproject.ovsdb.controller.OvsdbQueue;
import org.onosproject.ovsdb.rfc.message.TableUpdates;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.operations.Operation;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Test Adapter for OvsdbClientService.
 */
public class OvsdbClientServiceAdapter implements OvsdbClientService {

    @Override
    public OvsdbNodeId nodeId() {
        return null;
    }

    /**
     * Creates a mirror port. Mirrors the traffic
     * that goes to selectDstPort or comes from
     * selectSrcPort or packets containing selectVlan
     * to mirrorPort or to all ports that trunk mirrorVlan.
     *
     * @param bridgeName the name of the bridge
     * @param mirror     the OVSDB mirror description
     * @return true if mirror creation is successful, false otherwise
     */
    @Override
    public boolean createMirror(String bridgeName, OvsdbMirror mirror) {
        return true;
    }

    /**
     * Gets the Mirror uuid.
     *
     * @param mirrorName mirror name
     * @return mirror uuid, empty if no uuid is found
     */
    @Override
    public String getMirrorUuid(String mirrorName) {
        return null;
    }

    /**
     * Gets mirroring statistics of the device.
     *
     * @param deviceId target device id
     * @return set of mirroring statistics; empty if no mirror is found
     */
    @Override
    public Set<MirroringStatistics> getMirroringStatistics(DeviceId deviceId) {
        return null;
    }

    @Override
    public void applyQos(PortNumber portNumber, String qosId) {

    }

    @Override
    public void removeQos(PortNumber portNumber) {
    }

    @Override
    public boolean createQos(OvsdbQos ovsdbQos) {
        return false;
    }

    @Override
    public void dropQos(QosId qosId) {
    }

    @Override
    public OvsdbQos getQos(QosId qosId) {
        return null;
    };

    @Override
    public Set<OvsdbQos> getQoses() {
      return null;
    }

    @Override
    public boolean createQueue(OvsdbQueue queue) {
        return false;
    }

    @Override
    public void dropQueue(QueueId queueId) {
    }

    @Override
    public OvsdbQueue getQueue(QueueId queueId) {
        return null;
    };

    @Override
    public Set<OvsdbQueue> getQueues() {
        return null;
    }
    /**
     * Drops the configuration for mirror.
     *
     * @param mirroringName
     */
    @Override
    public void dropMirror(MirroringName mirroringName) {

    }

    @Override
    public boolean createInterface(String bridgeName, OvsdbInterface ovsdbIface) {
        return true;
    }

    @Override
    public boolean dropInterface(String name) {
        return true;
    }

    @Override
    public boolean createBridge(OvsdbBridge ovsdbBridge) {
        return true;
    }

    @Override
    public void dropBridge(String bridgeName) {
    }

    @Override
    public Set<OvsdbBridge> getBridges() {
        return null;
    }

    @Override
    public Set<ControllerInfo> getControllers(DeviceId openflowDeviceId) {
        return null;
    }

    @Override
    public ControllerInfo localController() {
        return null;
    }

    @Override
    public void setControllersWithDeviceId(DeviceId deviceId, List<ControllerInfo> controllers) {

    }

    @Override
    public void createPort(String bridgeName, String portName) {

    }

    @Override
    public void dropPort(String bridgeName, String portName) {

    }

    @Override
    public Set<OvsdbPort> getPorts() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public String getBridgeUuid(String bridgeName) {
        return null;
    }

    @Override
    public String getPortUuid(String portName, String bridgeUuid) {
        return null;
    }

    @Override
    public ListenableFuture<DatabaseSchema> getOvsdbSchema(String dbName) {
        return null;
    }

    @Override
    public ListenableFuture<TableUpdates> monitorTables(String dbName, String id) {
        return null;
    }

    @Override
    public DatabaseSchema getDatabaseSchema(String dbName) {
        return null;
    }

    @Override
    public Row getRow(String dbName, String tableName, String uuid) {
        return null;
    }

    @Override
    public void removeRow(String dbName, String tableName, String uuid) {

    }

    @Override
    public void updateOvsdbStore(String dbName, String tableName, String uuid, Row row) {

    }

    @Override
    public Set<OvsdbPort> getLocalPorts(Iterable<String> ifaceids) {
        return null;
    }

    @Override
    public void disconnect() {

    }

    @Override
    public ListenableFuture<JsonNode> getSchema(List<String> dbnames) {
        return null;
    }

    @Override
    public ListenableFuture<List<String>> echo() {
        return null;
    }

    @Override
    public ListenableFuture<JsonNode> monitor(DatabaseSchema dbSchema, String monitorId) {
        return null;
    }

    @Override
    public ListenableFuture<List<String>> listDbs() {
        return null;
    }

    @Override
    public ListenableFuture<List<JsonNode>> transact(DatabaseSchema dbSchema, List<Operation> operations) {
        return null;
    }
}
