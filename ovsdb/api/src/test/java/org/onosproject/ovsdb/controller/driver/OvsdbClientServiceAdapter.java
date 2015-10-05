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

package org.onosproject.ovsdb.controller.driver;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.ovsdb.controller.OvsdbBridge;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbPort;
import org.onosproject.ovsdb.controller.OvsdbTunnel;
import org.onosproject.ovsdb.rfc.message.OperationResult;
import org.onosproject.ovsdb.rfc.message.TableUpdates;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.UUID;
import org.onosproject.ovsdb.rfc.operations.Operation;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;

import java.util.List;
import java.util.Set;

/**
 * Test Adapter for OvsdbClientService.
 */
public class OvsdbClientServiceAdapter implements OvsdbClientService {

    @Override
    public OvsdbNodeId nodeId() {
        return null;
    }

    @Override
    public void createTunnel(IpAddress srcIp, IpAddress dstIp) {

    }

    @Override
    public void dropTunnel(IpAddress srcIp, IpAddress dstIp) {

    }

    @Override
    public Set<OvsdbTunnel> getTunnels() {
        return null;
    }

    @Override
    public void createBridge(String bridgeName) {

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
    public void setControllersWithUUID(UUID bridgeUuid, List<ControllerInfo> controllers) {

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
    public String getInterfaceUuid(String portUuid, String portName) {
        return null;
    }

    @Override
    public String getControllerUuid(String controllerName, String controllerTarget) {
        return null;
    }

    @Override
    public String getOvsUuid(String dbName) {
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
    public ListenableFuture<List<OperationResult>> transactConfig(String dbName, List<Operation> operations) {
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
