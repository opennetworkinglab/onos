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
package org.onosproject.ovsdb.controller.driver;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.Channel;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ControlProtocolVersion;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.DeviceCpuStats;
import org.onosproject.net.behaviour.DeviceMemoryStats;
import org.onosproject.net.behaviour.MirroringName;
import org.onosproject.net.behaviour.MirroringStatistics;
import org.onosproject.net.behaviour.QosId;
import org.onosproject.net.behaviour.QueueDescription;
import org.onosproject.net.behaviour.QueueId;
import org.onosproject.ovsdb.controller.OvsdbBridge;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbInterface;
import org.onosproject.ovsdb.controller.OvsdbMirror;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbPort;
import org.onosproject.ovsdb.controller.OvsdbPortName;
import org.onosproject.ovsdb.controller.OvsdbPortNumber;
import org.onosproject.ovsdb.controller.OvsdbQos;
import org.onosproject.ovsdb.controller.OvsdbQueue;
import org.onosproject.ovsdb.controller.OvsdbRowStore;
import org.onosproject.ovsdb.controller.OvsdbStore;
import org.onosproject.ovsdb.controller.OvsdbTableStore;
import org.onosproject.ovsdb.rfc.exception.ColumnSchemaNotFoundException;
import org.onosproject.ovsdb.rfc.exception.VersionMismatchException;
import org.onosproject.ovsdb.rfc.jsonrpc.Callback;
import org.onosproject.ovsdb.rfc.message.OperationResult;
import org.onosproject.ovsdb.rfc.message.TableUpdates;
import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.Condition;
import org.onosproject.ovsdb.rfc.notation.Mutation;
import org.onosproject.ovsdb.rfc.notation.OvsdbMap;
import org.onosproject.ovsdb.rfc.notation.OvsdbSet;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.Uuid;
import org.onosproject.ovsdb.rfc.operations.Delete;
import org.onosproject.ovsdb.rfc.operations.Insert;
import org.onosproject.ovsdb.rfc.operations.Mutate;
import org.onosproject.ovsdb.rfc.operations.Operation;
import org.onosproject.ovsdb.rfc.operations.Update;
import org.onosproject.ovsdb.rfc.schema.ColumnSchema;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.schema.TableSchema;
import org.onosproject.ovsdb.rfc.table.Bridge;
import org.onosproject.ovsdb.rfc.table.Controller;
import org.onosproject.ovsdb.rfc.table.Interface;
import org.onosproject.ovsdb.rfc.table.Mirror;
import org.onosproject.ovsdb.rfc.table.OvsdbTable;
import org.onosproject.ovsdb.rfc.table.Port;
import org.onosproject.ovsdb.rfc.table.Qos;
import org.onosproject.ovsdb.rfc.table.Queue;
import org.onosproject.ovsdb.rfc.table.TableGenerator;
import org.onosproject.ovsdb.rfc.utils.ConditionUtil;
import org.onosproject.ovsdb.rfc.utils.FromJsonUtil;
import org.onosproject.ovsdb.rfc.utils.JsonRpcWriterUtil;
import org.onosproject.ovsdb.rfc.utils.MutationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.onosproject.ovsdb.controller.OvsdbConstant.BRIDGE;
import static org.onosproject.ovsdb.controller.OvsdbConstant.BRIDGES;
import static org.onosproject.ovsdb.controller.OvsdbConstant.BRIDGE_CONTROLLER;
import static org.onosproject.ovsdb.controller.OvsdbConstant.CONTROLLER;
import static org.onosproject.ovsdb.controller.OvsdbConstant.DATABASENAME;
import static org.onosproject.ovsdb.controller.OvsdbConstant.EXTERNAL_ID;
import static org.onosproject.ovsdb.controller.OvsdbConstant.EXTERNAL_ID_INTERFACE_ID;
import static org.onosproject.ovsdb.controller.OvsdbConstant.INTERFACE;
import static org.onosproject.ovsdb.controller.OvsdbConstant.INTERFACES;
import static org.onosproject.ovsdb.controller.OvsdbConstant.MIRROR;
import static org.onosproject.ovsdb.controller.OvsdbConstant.MIRRORS;
import static org.onosproject.ovsdb.controller.OvsdbConstant.OFPORT;
import static org.onosproject.ovsdb.controller.OvsdbConstant.OFPORT_ERROR;
import static org.onosproject.ovsdb.controller.OvsdbConstant.PORT;
import static org.onosproject.ovsdb.controller.OvsdbConstant.PORTS;
import static org.onosproject.ovsdb.controller.OvsdbConstant.PORT_QOS;
import static org.onosproject.ovsdb.controller.OvsdbConstant.QOS;
import static org.onosproject.ovsdb.controller.OvsdbConstant.QOS_EXTERNAL_ID_KEY;
import static org.onosproject.ovsdb.controller.OvsdbConstant.QUEUE;
import static org.onosproject.ovsdb.controller.OvsdbConstant.QUEUES;
import static org.onosproject.ovsdb.controller.OvsdbConstant.QUEUE_EXTERNAL_ID_KEY;
import static org.onosproject.ovsdb.controller.OvsdbConstant.TYPEVXLAN;
import static org.onosproject.ovsdb.controller.OvsdbConstant.UUID;

/**
 * An representation of an ovsdb client.
 */
public class DefaultOvsdbClient implements OvsdbProviderService, OvsdbClientService {

    private static final int TRANSACTCONFIG_TIMEOUT = 3; //sec
    private static final int OFPORT_ERROR_COMPARISON = 0;

    private final Logger log = LoggerFactory.getLogger(DefaultOvsdbClient.class);

    private Channel channel;
    private OvsdbAgent agent;
    private boolean connected;
    private OvsdbNodeId nodeId;
    private Callback monitorCallBack;
    private OvsdbStore ovsdbStore = new OvsdbStore();

    private final Map<String, String> requestMethod = Maps.newHashMap();
    private final Map<String, SettableFuture<? extends Object>> requestResult = Maps.newHashMap();
    private final Map<String, DatabaseSchema> schema = Maps.newHashMap();


    /**
     * Creates an OvsdbClient.
     *
     * @param nodeId ovsdb node id
     */
    public DefaultOvsdbClient(OvsdbNodeId nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public OvsdbNodeId nodeId() {
        return nodeId;
    }

    @Override
    public void setAgent(OvsdbAgent agent) {
        if (this.agent == null) {
            this.agent = agent;
        }
    }

    @Override
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void setConnection(boolean connected) {
        this.connected = connected;
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public void nodeAdded() {
        this.agent.addConnectedNode(nodeId, this);
    }

    @Override
    public void nodeRemoved() {
        this.agent.removeConnectedNode(nodeId);
        channel.disconnect();
    }

    /**
     * Gets the ovsdb table store.
     *
     * @param dbName the ovsdb database name
     * @return ovsTableStore, empty if table store is find
     */
    private OvsdbTableStore getTableStore(String dbName) {
        if (ovsdbStore == null) {
            return null;
        }
        return ovsdbStore.getOvsdbTableStore(dbName);
    }

    /**
     * Gets the ovsdb row store.
     *
     * @param dbName    the ovsdb database name
     * @param tableName the ovsdb table name
     * @return ovsRowStore, empty store if no rows exist in the table
     */
    private OvsdbRowStore getRowStore(String dbName, String tableName) {
        OvsdbTableStore tableStore = getTableStore(dbName);
        if (tableStore == null) {
            return null;
        }

        OvsdbRowStore rowStore = tableStore.getRows(tableName);
        if (rowStore == null) {
            rowStore = new OvsdbRowStore();
        }
        return rowStore;
    }

    @Override
    public Row getRow(String dbName, String tableName, String uuid) {
        OvsdbTableStore tableStore = getTableStore(dbName);
        if (tableStore == null) {
            return null;
        }
        OvsdbRowStore rowStore = tableStore.getRows(tableName);
        if (rowStore == null) {
            return null;
        }
        return rowStore.getRow(uuid);
    }

    @Override
    public void removeRow(String dbName, String tableName, String uuid) {
        OvsdbTableStore tableStore = getTableStore(dbName);
        if (tableStore == null) {
            return;
        }
        OvsdbRowStore rowStore = tableStore.getRows(tableName);
        if (rowStore == null) {
            return;
        }
        rowStore.deleteRow(uuid);
    }

    @Override
    public void updateOvsdbStore(String dbName, String tableName, String uuid,
                                 Row row) {
        OvsdbTableStore tableStore = ovsdbStore.getOvsdbTableStore(dbName);
        if (tableStore == null) {
            tableStore = new OvsdbTableStore();
        }
        OvsdbRowStore rowStore = tableStore.getRows(tableName);
        if (rowStore == null) {
            rowStore = new OvsdbRowStore();
        }
        rowStore.insertRow(uuid, row);
        tableStore.createOrUpdateTable(tableName, rowStore);
        ovsdbStore.createOrUpdateOvsdbStore(dbName, tableStore);
    }

    @Override
    public String getMirrorUuid(String mirrorName) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        OvsdbRowStore rowStore = getRowStore(DATABASENAME, MIRROR);
        if (rowStore == null) {
            log.warn("The mirror uuid is null");
            return null;
        }

        ConcurrentMap<String, Row> mirrorTableRows = rowStore.getRowStore();
        if (mirrorTableRows == null) {
            log.warn("The mirror uuid is null");
            return null;
        }

        for (String uuid : mirrorTableRows.keySet()) {
            Mirror mirror = (Mirror) TableGenerator
                    .getTable(dbSchema, mirrorTableRows.get(uuid), OvsdbTable.MIRROR);
            String name = mirror.getName();
            if (name.contains(mirrorName)) {
                return uuid;
            }
        }
        log.warn("Mirroring not found");
        return null;
    }

    @Override
    public Set<MirroringStatistics> getMirroringStatistics(DeviceId deviceId) {
        Uuid bridgeUuid = getBridgeUuid(deviceId);
        if (bridgeUuid == null) {
            log.warn("Couldn't find bridge {} in {}", deviceId, nodeId.getIpAddress());
            return null;
        }

        List<MirroringStatistics> mirrorings = getMirrorings(bridgeUuid);
        if (mirrorings == null) {
            log.warn("Couldn't find mirrors in {}", nodeId.getIpAddress());
            return null;
        }
        return ImmutableSet.copyOf(mirrorings);
    }

    /**
     * Helper method which retrieves mirrorings statistics using bridge uuid.
     *
     * @param bridgeUuid the uuid of the bridge
     * @return the list of the mirrorings statistics.
     */
    private List<MirroringStatistics> getMirrorings(Uuid bridgeUuid) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        if (dbSchema == null) {
            log.warn("Unable to retrieve dbSchema {}", DATABASENAME);
            return null;
        }
        OvsdbRowStore rowStore = getRowStore(DATABASENAME, BRIDGE);
        if (rowStore == null) {
            log.warn("Unable to retrieve rowStore {} of {}", BRIDGE, DATABASENAME);
            return null;
        }

        Row bridgeRow = rowStore.getRow(bridgeUuid.value());
        Bridge bridge = (Bridge) TableGenerator.
                getTable(dbSchema, bridgeRow, OvsdbTable.BRIDGE);

        Set<Uuid> mirroringsUuids = (Set<Uuid>) ((OvsdbSet) bridge
                .getMirrorsColumn().data()).set();

        OvsdbRowStore mirrorRowStore = getRowStore(DATABASENAME, MIRROR);
        if (mirrorRowStore == null) {
            log.warn("Unable to retrieve rowStore {} of {}", MIRROR, DATABASENAME);
            return null;
        }

        List<MirroringStatistics> mirroringStatistics = new ArrayList<>();
        ConcurrentMap<String, Row> mirrorTableRows = mirrorRowStore.getRowStore();
        mirrorTableRows.forEach((key, row) -> {
            if (!mirroringsUuids.contains(Uuid.uuid(key))) {
                return;
            }
            Mirror mirror = (Mirror) TableGenerator
                    .getTable(dbSchema, row, OvsdbTable.MIRROR);
            mirroringStatistics.add(MirroringStatistics.mirroringStatistics(mirror.getName(),
                                                                      (Map<String, Integer>) ((OvsdbMap) mirror
                                                                   .getStatisticsColumn().data()).map()));
        });
        return ImmutableList.copyOf(mirroringStatistics);
    }

    @Override
    public String getPortUuid(String portName, String bridgeUuid) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);

        Row bridgeRow = getRow(DATABASENAME, BRIDGE, bridgeUuid);
        Bridge bridge = (Bridge) TableGenerator.getTable(dbSchema, bridgeRow,
                                                         OvsdbTable.BRIDGE);
        if (bridge != null) {
            OvsdbSet setPorts = (OvsdbSet) bridge.getPortsColumn().data();
            @SuppressWarnings("unchecked")
            Set<Uuid> ports = setPorts.set();
            if (ports == null || ports.isEmpty()) {
                log.warn("The port uuid is null");
                return null;
            }

            for (Uuid uuid : ports) {
                Row portRow = getRow(DATABASENAME, PORT, uuid.value());
                Port port = (Port) TableGenerator.getTable(dbSchema, portRow,
                                                           OvsdbTable.PORT);
                if (port != null && portName.equalsIgnoreCase(port.getName())) {
                    return uuid.value();
                }
            }
        }
        return null;
    }

    @Override
    public String getBridgeUuid(String bridgeName) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        OvsdbRowStore rowStore = getRowStore(DATABASENAME, BRIDGE);
        if (rowStore == null) {
            log.debug("The bridge uuid is null");
            return null;
        }

        ConcurrentMap<String, Row> bridgeTableRows = rowStore.getRowStore();
        if (bridgeTableRows == null) {
            log.debug("The bridge uuid is null");
            return null;
        }

        for (String uuid : bridgeTableRows.keySet()) {
            Bridge bridge = (Bridge) TableGenerator
                    .getTable(dbSchema, bridgeTableRows.get(uuid), OvsdbTable.BRIDGE);
            if (bridge.getName().equals(bridgeName)) {
                return uuid;
            }
        }
        return null;
    }

    private String getOvsUuid(String dbName) {
        OvsdbRowStore rowStore = getRowStore(DATABASENAME, DATABASENAME);
        if (rowStore == null) {
            log.debug("The bridge uuid is null");
            return null;
        }
        ConcurrentMap<String, Row> ovsTableRows = rowStore.getRowStore();
        if (ovsTableRows != null) {
            for (String uuid : ovsTableRows.keySet()) {
                Row row = ovsTableRows.get(uuid);
                String tableName = row.tableName();
                if (tableName.equals(dbName)) {
                    return uuid;
                }
            }
        }
        return null;
    }

    @Override
    public void createPort(String bridgeName, String portName) {
        String bridgeUuid = getBridgeUuid(bridgeName);
        if (bridgeUuid == null) {
            log.error("Can't find bridge {} in {}", bridgeName, nodeId.getIpAddress());
            return;
        }

        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        String portUuid = getPortUuid(portName, bridgeUuid);
        Port port = (Port) TableGenerator.createTable(dbSchema, OvsdbTable.PORT);
        port.setName(portName);
        if (portUuid == null) {
            insertConfig(PORT, UUID, BRIDGE, PORTS, bridgeUuid, port.getRow());
        }
    }

    @Override
    public void dropPort(String bridgeName, String portName) {
        String bridgeUuid = getBridgeUuid(bridgeName);
        if (bridgeUuid == null) {
            log.error("Could not find Bridge {} in {}", bridgeName, nodeId);
            return;
        }

        String portUuid = getPortUuid(portName, bridgeUuid);
        if (portUuid != null) {
            log.info("Port {} delete", portName);
            deleteConfig(PORT, UUID, portUuid, BRIDGE, PORTS, Uuid.uuid(portUuid));
        }
    }

    @Override
    public boolean createBridge(OvsdbBridge ovsdbBridge) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        String ovsUuid = getOvsUuid(DATABASENAME);

        if (dbSchema == null || ovsUuid == null) {
            log.error("Can't find database Open_vSwitch");
            return false;
        }

        Bridge bridge = (Bridge) TableGenerator.createTable(dbSchema, OvsdbTable.BRIDGE);
        bridge.setOtherConfig(ovsdbBridge.otherConfigs());

        if (ovsdbBridge.failMode().isPresent()) {
            String failMode = ovsdbBridge.failMode().get().name().toLowerCase();
            bridge.setFailMode(Sets.newHashSet(failMode));
        }

        if (ovsdbBridge.datapathType().isPresent()) {
            String datapathType = ovsdbBridge.datapathType().get();
            bridge.setDatapathType(datapathType);
        }

        if (ovsdbBridge.controlProtocols().isPresent()) {
            bridge.setProtocols(ovsdbBridge.controlProtocols().get().stream()
                    .map(ControlProtocolVersion::toString)
                    .collect(Collectors.toCollection(HashSet::new)));
        }

        if (ovsdbBridge.mcastSnoopingEnable().isPresent()) {
            boolean mcastSnoopingFlag = ovsdbBridge.mcastSnoopingEnable().get();
            bridge.setMcastSnoopingEnable(mcastSnoopingFlag);
        }

        String bridgeUuid = getBridgeUuid(ovsdbBridge.name());
        if (bridgeUuid == null) {
            bridge.setName(ovsdbBridge.name());
            bridgeUuid = insertConfig(
                    BRIDGE, UUID, DATABASENAME, BRIDGES,
                    ovsUuid, bridge.getRow());
        } else {
            // update the bridge if it's already existing
            updateConfig(BRIDGE, UUID, bridgeUuid, bridge.getRow());
        }

        if (bridgeUuid == null) {
            log.warn("Failed to create bridge {} on {}", ovsdbBridge.name(), nodeId);
            return false;
        }

        createPort(ovsdbBridge.name(), ovsdbBridge.name());
        setControllersWithUuid(Uuid.uuid(bridgeUuid), ovsdbBridge.controllers());

        log.info("Created bridge {}", ovsdbBridge.name());
        return true;
    }

    @Override
    public ControllerInfo localController() {
        IpAddress ipAddress = IpAddress.valueOf(((InetSocketAddress)
                channel.localAddress()).getAddress());
        return new ControllerInfo(ipAddress, OFPORT, "tcp");
    }

    private void setControllersWithUuid(Uuid bridgeUuid, List<ControllerInfo> controllers) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        if (dbSchema == null) {
            log.debug("There is no schema");
            return;
        }
        List<Controller> oldControllers = getControllers(bridgeUuid);
        if (oldControllers == null) {
            log.warn("There are no controllers");
            return;
        }

        Set<ControllerInfo> newControllers = new HashSet<>(controllers);
        List<Controller> removeControllers = new ArrayList<>();
        oldControllers.forEach(controller -> {
            ControllerInfo controllerInfo = new ControllerInfo((String) controller.getTargetColumn().data());
            if (newControllers.contains(controllerInfo)) {
                newControllers.remove(controllerInfo);
            } else {
                removeControllers.add(controller);
            }
        });
        OvsdbRowStore controllerRowStore = getRowStore(DATABASENAME, CONTROLLER);
        if (controllerRowStore == null) {
            log.debug("There is no controller table");
            return;
        }

        newControllers.stream().map(c -> {
            Controller controller = (Controller) TableGenerator
                    .createTable(dbSchema, OvsdbTable.CONTROLLER);
            controller.setTarget(c.target());
            return controller;
        }).forEach(c -> insertConfig(CONTROLLER, UUID, BRIDGE, BRIDGE_CONTROLLER,
                                    bridgeUuid.value(),
                                    c.getRow()));

        // Controller removal is extremely dangerous operation, because with
        // empty controller list, all existing flow rules will be wiped out.
        // To harden the setController operation, we need to double check whether
        // the updated controller list size is bigger than the remove controller list size
        List<Controller> updatedControllers = getControllers(bridgeUuid);
        if (updatedControllers != null && updatedControllers.size() > removeControllers.size()) {
            removeControllers.forEach(c ->
                    deleteConfig(CONTROLLER, UUID, c.getRow().uuid().value(),
                    BRIDGE, BRIDGE_CONTROLLER, c.getRow().uuid()));
        } else {
            log.warn("New controllers were not properly configured to OVS " +
                    "bridge {} or failed to retrieve controller list from OVS " +
                    "bridge {}", bridgeUuid, bridgeUuid);
        }
    }

    @Override
    public void setControllersWithDeviceId(DeviceId deviceId, List<ControllerInfo> controllers) {
        setControllersWithUuid(getBridgeUuid(deviceId), controllers);
    }

    @Override
    public void dropBridge(String bridgeName) {
        String bridgeUuid = getBridgeUuid(bridgeName);
        if (bridgeUuid == null) {
            log.warn("Could not find bridge in node", nodeId.getIpAddress());
            return;
        }
        deleteConfig(BRIDGE, UUID, bridgeUuid, DATABASENAME, BRIDGES, Uuid.uuid(bridgeUuid));
    }

    @Override
    public void applyQos(PortNumber portNumber, String qosName) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        OvsdbRowStore portRowStore = getRowStore(DATABASENAME, PORT);
        if (portRowStore == null) {
            log.debug("The port uuid is null");
            return;
        }
        OvsdbRowStore qosRowStore = getRowStore(DATABASENAME, QOS);
        if (qosRowStore == null) {
            log.debug("The qos uuid is null");
            return;
        }

        // Due to Qos Table doesn't have a unique identifier except uuid, unlike
        // Bridge or Port Table has a name column,in order to make the api more
        // general, put qos name in external_ids column of Qos Table if this qos
        // created by onos.
        ConcurrentMap<String, Row> qosTableRows = qosRowStore.getRowStore();
        ConcurrentMap<String, Row> portTableRows = portRowStore.getRowStore();
        Row qosRow = qosTableRows.values().stream().filter(r -> {
            OvsdbMap ovsdbMap = (OvsdbMap) (r.getColumn(EXTERNAL_ID).data());
            return qosName.equals(ovsdbMap.map().get(QOS_EXTERNAL_ID_KEY));
        }).findFirst().orElse(null);

        Row portRow = portTableRows.values().stream()
                .filter(r -> r.getColumn("name").data().equals(portNumber.name()))
                .findFirst().orElse(null);
        if (portRow != null && qosRow != null) {
            String qosId = qosRow.uuid().value();
            Uuid portUuid = portRow.uuid();
            Map<String, Column> columns = new HashMap<>();
            Row newPortRow = new Row(PORT, portUuid, columns);
            Port newport = new Port(dbSchema, newPortRow);
            columns.put(Port.PortColumn.QOS.columnName(), newport.getQosColumn());
            newport.setQos(Uuid.uuid(qosId));
            updateConfig(PORT, UUID, portUuid.value(), newport.getRow());
        }
    }

    @Override
    public void removeQos(PortNumber portNumber) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        OvsdbRowStore rowStore = getRowStore(DATABASENAME, PORT);
        if (rowStore == null) {
            log.debug("The qos uuid is null");
            return;
        }

        ConcurrentMap<String, Row> ovsTableRows = rowStore.getRowStore();
        Row portRow = ovsTableRows.values().stream()
                .filter(r -> r.getColumn("name").data().equals(portNumber.name()))
                .findFirst().orElse(null);
        if (portRow == null) {
            log.warn("Couldn't find port {} in ovsdb port table.", portNumber.name());
            return;
        }

        OvsdbSet ovsdbSet = ((OvsdbSet) portRow.getColumn(PORT_QOS).data());
        @SuppressWarnings("unchecked")
        Set<Uuid> qosIdSet = ovsdbSet.set();
        if (qosIdSet == null || qosIdSet.isEmpty()) {
            return;
        }
        Uuid qosUuid = (Uuid) qosIdSet.toArray()[0];
        Condition condition = ConditionUtil.isEqual(UUID, portRow.uuid());
        List<Condition> conditions = Lists.newArrayList(condition);
        Mutation mutation = MutationUtil.delete(PORT_QOS, qosUuid);
        List<Mutation> mutations = Lists.newArrayList(mutation);

        ArrayList<Operation> operations = Lists.newArrayList();
        Mutate mutate = new Mutate(dbSchema.getTableSchema(PORT), conditions, mutations);
        operations.add(mutate);
        transactConfig(DATABASENAME, operations);
    }

    @Override
    public boolean createQos(OvsdbQos ovsdbQos) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        Qos qos = (Qos) TableGenerator.createTable(dbSchema, OvsdbTable.QOS);
        OvsdbRowStore rowStore = getRowStore(DATABASENAME, QOS);
        if (rowStore == null) {
            log.debug("The qos uuid is null");
            return false;
        }

        ArrayList<Operation> operations = Lists.newArrayList();
        Set<String> types = Sets.newHashSet();
        Map<Long, Uuid> queues = Maps.newHashMap();

        types.add(ovsdbQos.qosType());
        qos.setOtherConfig(ovsdbQos.otherConfigs());
        qos.setExternalIds(ovsdbQos.externalIds());
        qos.setType(types);
        if (ovsdbQos.qosQueues().isPresent()) {
            for (Map.Entry<Long, String> entry : ovsdbQos.qosQueues().get().entrySet()) {
                OvsdbRowStore queueRowStore = getRowStore(DATABASENAME, QUEUE);
                if (queueRowStore != null) {
                    ConcurrentMap<String, Row> queueTableRows = queueRowStore.getRowStore();
                    Row queueRow = queueTableRows.values().stream().filter(r -> {
                        OvsdbMap ovsdbMap = (OvsdbMap) (r.getColumn(EXTERNAL_ID).data());
                        return entry.getValue().equals(ovsdbMap.map().get(QUEUE_EXTERNAL_ID_KEY));
                    }).findFirst().orElse(null);
                    if (queueRow != null) {
                        queues.put(entry.getKey(), queueRow.uuid());
                    }
                }
            }
            qos.setQueues(queues);
        }

        Insert qosInsert = new Insert(dbSchema.getTableSchema(QOS), QOS, qos.getRow());
        operations.add(qosInsert);
        try {
            transactConfig(DATABASENAME, operations).get();
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
        return true;
    }

    @Override
    public void dropQos(QosId qosId) {
        OvsdbRowStore rowStore = getRowStore(DATABASENAME, QOS);
        if (rowStore != null) {
            ConcurrentMap<String, Row> qosTableRows = rowStore.getRowStore();
            Row qosRow = qosTableRows.values().stream().filter(r -> {
                        OvsdbMap ovsdbMap = (OvsdbMap) (r.getColumn(EXTERNAL_ID).data());
                        return qosId.name().equals(ovsdbMap.map().get(QOS_EXTERNAL_ID_KEY));
                    }).findFirst().orElse(null);
            if (qosRow != null) {
                deleteConfig(QOS, UUID, qosRow.uuid().value(), PORT, PORT_QOS, qosRow.uuid());
            }
        }
    }
    @Override
    public OvsdbQos getQos(QosId qosId) {
        Set<OvsdbQos> ovsdbQoses = getQoses();
        return ovsdbQoses.stream().filter(r ->
                qosId.name().equals(r.externalIds().get(QOS_EXTERNAL_ID_KEY))).
                findFirst().orElse(null);
    }

    @Override
    public Set<OvsdbQos> getQoses() {
        Set<OvsdbQos> ovsdbQoses = new HashSet<>();
        OvsdbRowStore rowStore = getRowStore(DATABASENAME, QOS);
        if (rowStore == null) {
            log.debug("The qos uuid is null");
            return ovsdbQoses;
        }
        ConcurrentMap<String, Row> rows = rowStore.getRowStore();
        ovsdbQoses = rows.keySet().stream()
                .map(uuid -> getRow(DATABASENAME, QOS, uuid))
                .map(this::getOvsdbQos)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return ovsdbQoses;
    }

    @Override
    public void bindQueues(QosId qosId, Map<Long, QueueDescription> queues) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        OvsdbRowStore qosRowStore = getRowStore(DATABASENAME, QOS);
        if (qosRowStore == null) {
            log.debug("The qos uuid is null");
            return;
        }
        OvsdbRowStore queueRowStore = getRowStore(DATABASENAME, QUEUE);
        if (queueRowStore == null) {
            log.debug("The queue uuid is null");
            return;
        }

        ConcurrentMap<String, Row> qosTableRows = qosRowStore.getRowStore();
        ConcurrentMap<String, Row> queueTableRows = queueRowStore.getRowStore();

        Row qosRow = qosTableRows.values().stream().filter(r -> {
            OvsdbMap ovsdbMap = (OvsdbMap) (r.getColumn(EXTERNAL_ID).data());
            return qosId.name().equals(ovsdbMap.map().get(QOS_EXTERNAL_ID_KEY));
        }).findFirst().orElse(null);

        if (qosRow == null) {
            log.warn("Can't find QoS {}", qosId);
            return;
        }

        Uuid qosUuid = qosRow.uuid();

        Map<Long, Uuid> newQueues = new HashMap<>();
        for (Map.Entry<Long, QueueDescription> entry : queues.entrySet()) {
            Row queueRow = queueTableRows.values().stream().filter(r -> {
                OvsdbMap ovsdbMap = (OvsdbMap) (r.getColumn(EXTERNAL_ID).data());
                return entry.getValue().queueId().name().equals(ovsdbMap.map().get(QUEUE_EXTERNAL_ID_KEY));
            }).findFirst().orElse(null);
            if (queueRow != null) {
                newQueues.put(entry.getKey(), queueRow.uuid());
            }
        }

        // update the qos table
        ArrayList<Operation> operations = Lists.newArrayList();
        Condition condition = ConditionUtil.isEqual(UUID, qosUuid);
        Mutation mutation = MutationUtil.insert(QUEUES, newQueues);
        List<Condition> conditions = Collections.singletonList(condition);
        List<Mutation> mutations = Collections.singletonList(mutation);
        operations.add(new Mutate(dbSchema.getTableSchema(QOS), conditions, mutations));

        transactConfig(DATABASENAME, operations);
    }


    @SuppressWarnings("unchecked")
    @Override
    public void unbindQueues(QosId qosId, List<Long> queueKeys) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        OvsdbRowStore qosRowStore = getRowStore(DATABASENAME, QOS);
        if (qosRowStore == null) {
            return;
        }

        ConcurrentMap<String, Row> qosTableRows = qosRowStore.getRowStore();

        Row qosRow = qosTableRows.values().stream().filter(r -> {
            OvsdbMap ovsdbMap = (OvsdbMap) (r.getColumn(EXTERNAL_ID).data());
            return qosId.name().equals(ovsdbMap.map().get(QOS_EXTERNAL_ID_KEY));
        }).findFirst().orElse(null);

        if (qosRow == null) {
            log.warn("Can't find QoS {}", qosId);
            return;
        }

        Map<Long, Uuid> deleteQueuesMap;
        Map<Integer, Uuid> queuesMap = ((OvsdbMap) qosRow.getColumn(QUEUES).data()).map();

        deleteQueuesMap = queueKeys.stream()
                .filter(key -> queuesMap.containsKey(key.intValue()))
                .collect(Collectors.toMap(key -> key, key -> queuesMap.get(key.intValue()), (a, b) -> b));

        if (deleteQueuesMap.size() != 0) {
            TableSchema parentTableSchema = dbSchema
                    .getTableSchema(QOS);
            ColumnSchema parentColumnSchema = parentTableSchema
                    .getColumnSchema(QUEUES);

            Mutation mutation = MutationUtil.delete(parentColumnSchema.name(), OvsdbMap.ovsdbMap(deleteQueuesMap));
            List<Mutation> mutations = Collections.singletonList(mutation);

            Condition condition = ConditionUtil.isEqual(UUID, qosRow.uuid());
            List<Condition> conditionList = Collections.singletonList(condition);
            List<Operation> operations = Collections.singletonList(
                    new Mutate(parentTableSchema, conditionList, mutations));

            transactConfig(DATABASENAME, operations);
        }
    }


    @Override
    public boolean createQueue(OvsdbQueue ovsdbQueue) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        Queue queue = (Queue) TableGenerator.createTable(dbSchema, OvsdbTable.QUEUE);
        ArrayList<Operation> operations = Lists.newArrayList();
        OvsdbRowStore rowStore = getRowStore(DATABASENAME, QUEUE);
        if (rowStore == null) {
            log.debug("The queue uuid is null");
            return false;
        }

        if (ovsdbQueue.dscp().isPresent()) {
            queue.setDscp(ImmutableSet.of(ovsdbQueue.dscp().get()));
        }
        queue.setOtherConfig(ovsdbQueue.otherConfigs());
        queue.setExternalIds(ovsdbQueue.externalIds());
        Insert queueInsert = new Insert(dbSchema.getTableSchema(QUEUE), QUEUE, queue.getRow());
        operations.add(queueInsert);

        try {
            transactConfig(DATABASENAME, operations).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("createQueue transactConfig get exception !");
        }
        return true;
    }

    @Override
    public void dropQueue(QueueId queueId) {
        OvsdbRowStore queueRowStore = getRowStore(DATABASENAME, QUEUE);
        if (queueRowStore == null) {
            return;
        }

        ConcurrentMap<String, Row> queueTableRows = queueRowStore.getRowStore();
        Row queueRow = queueTableRows.values().stream().filter(r -> {
            OvsdbMap ovsdbMap = (OvsdbMap) (r.getColumn(EXTERNAL_ID).data());
            return queueId.name().equals(ovsdbMap.map().get(QUEUE_EXTERNAL_ID_KEY));
        }).findFirst().orElse(null);
        if (queueRow == null) {
            return;
        }

        String queueUuid = queueRow.uuid().value();
        OvsdbRowStore qosRowStore = getRowStore(DATABASENAME, QOS);
        if (qosRowStore != null) {
            Map<Long, Uuid> queueMap = new HashMap<>();
            ConcurrentMap<String, Row> qosTableRows = qosRowStore.getRowStore();
            qosTableRows.values().stream().filter(r -> {
                Map<Integer, Uuid> ovsdbMap = ((OvsdbMap) r.getColumn(QUEUES).data()).map();
                Set<Integer> keySet = ovsdbMap.keySet();
                for (Integer keyId : keySet) {
                    if (ovsdbMap.get(keyId).equals(Uuid.uuid(queueUuid))) {
                        queueMap.put(keyId.longValue(), Uuid.uuid(queueUuid));
                        return true;
                    }
                }
                return false;
            }).findFirst().orElse(null);
            deleteConfig(QUEUE, UUID, queueUuid, QOS, QUEUES, OvsdbMap.ovsdbMap(queueMap));
        } else {
            deleteConfig(QUEUE, UUID, queueUuid, null, null, null);
        }
    }
    @Override
    public OvsdbQueue getQueue(QueueId queueId) {
        Set<OvsdbQueue> ovsdbQueues = getQueues();
        return ovsdbQueues.stream().filter(r ->
                queueId.name().equals(r.externalIds().get(QUEUE_EXTERNAL_ID_KEY))).
                findFirst().orElse(null);
    }

    @Override
    public Set<OvsdbQueue> getQueues() {
        Set<OvsdbQueue> ovsdbqueues = new HashSet<>();
        OvsdbRowStore rowStore = getRowStore(DATABASENAME, QUEUE);
        if (rowStore == null) {
            log.debug("The queue uuid is null");
            return ovsdbqueues;
        }
        ConcurrentMap<String, Row> rows = rowStore.getRowStore();
        ovsdbqueues = rows.keySet()
                .stream()
                .map(uuid -> getRow(DATABASENAME, QUEUE, uuid))
                .map(this::getOvsdbQueue)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return ovsdbqueues;
    }

    @Override
    public boolean createMirror(String bridgeName, OvsdbMirror mirror) {

        /**
         * Retrieves bridge's uuid. It is necessary to update
         * Bridge table.
         */
        String bridgeUuid  = getBridgeUuid(bridgeName);
        if (bridgeUuid == null) {
            log.warn("Couldn't find bridge {} in {}", bridgeName, nodeId.getIpAddress());
            return false;
        }

        OvsdbMirror.Builder mirrorBuilder = OvsdbMirror.builder();

        mirrorBuilder.mirroringName(mirror.mirroringName());
        mirrorBuilder.selectAll(mirror.selectAll());

        /**
         * Retrieves the uuid of the monitored dst ports.
         */
        mirrorBuilder.monitorDstPorts(mirror.monitorDstPorts().parallelStream()
                                              .map(dstPort -> {
                                                  String dstPortUuid = getPortUuid(dstPort.value(), bridgeUuid);
                                                  if (dstPortUuid != null) {
                                                      return Uuid.uuid(dstPortUuid);
                                                  }
                                                  log.warn("Couldn't find port {} in {}",
                                                           dstPort.value(), nodeId.getIpAddress());
                                                  return null;
                                              })
                                              .filter(Objects::nonNull)
                                              .collect(Collectors.toSet())
        );

        /**
         * Retrieves the uuid of the monitored src ports.
         */
        mirrorBuilder.monitorSrcPorts(mirror.monitorSrcPorts().parallelStream()
                                              .map(srcPort -> {
                                                  String srcPortUuid = getPortUuid(srcPort.value(), bridgeUuid);
                                                  if (srcPortUuid != null) {
                                                      return Uuid.uuid(srcPortUuid);
                                                  }
                                                  log.warn("Couldn't find port {} in {}",
                                                           srcPort.value(), nodeId.getIpAddress());
                                                  return null;
                                              }).filter(Objects::nonNull)
                                              .collect(Collectors.toSet())
        );

        mirrorBuilder.monitorVlans(mirror.monitorVlans());
        mirrorBuilder.mirrorPort(mirror.mirrorPort());
        mirrorBuilder.mirrorVlan(mirror.mirrorVlan());
        mirrorBuilder.externalIds(mirror.externalIds());
        mirror = mirrorBuilder.build();

        if (mirror.monitorDstPorts().isEmpty() &&
                mirror.monitorSrcPorts().isEmpty() &&
                mirror.monitorVlans().isEmpty()) {
            log.warn("Invalid monitoring data");
            return false;
        }

        DatabaseSchema dbSchema = schema.get(DATABASENAME);

        Mirror mirrorEntry = (Mirror) TableGenerator.createTable(dbSchema, OvsdbTable.MIRROR);
        mirrorEntry.setName(mirror.mirroringName());
        mirrorEntry.setSelectDstPort(mirror.monitorDstPorts());
        mirrorEntry.setSelectSrcPort(mirror.monitorSrcPorts());
        mirrorEntry.setSelectVlan(mirror.monitorVlans());
        mirrorEntry.setExternalIds(mirror.externalIds());

        /**
         * If mirror port, retrieves the uuid of the mirror port.
         */
        if (mirror.mirrorPort() != null) {

            String outputPortUuid = getPortUuid(mirror.mirrorPort().value(), bridgeUuid);
            if (outputPortUuid == null) {
                log.warn("Couldn't find port {} in {}", mirror.mirrorPort().value(), nodeId.getIpAddress());
                return false;
            }

            mirrorEntry.setOutputPort(Uuid.uuid(outputPortUuid));

        } else if (mirror.mirrorVlan() != null) {

            mirrorEntry.setOutputVlan(mirror.mirrorVlan());

        } else {
            log.warn("Invalid mirror, no mirror port and no mirror vlan");
            return false;
        }

        ArrayList<Operation> operations = Lists.newArrayList();
        Insert mirrorInsert = new Insert(dbSchema.getTableSchema("Mirror"), "Mirror", mirrorEntry.getRow());
        operations.add(mirrorInsert);

        // update the bridge table
        Condition condition = ConditionUtil.isEqual(UUID, Uuid.uuid(bridgeUuid));
        Mutation mutation = MutationUtil.insert(MIRRORS, Uuid.uuid("Mirror"));
        List<Condition> conditions = Lists.newArrayList(condition);
        List<Mutation> mutations = Lists.newArrayList(mutation);
        operations.add(new Mutate(dbSchema.getTableSchema("Bridge"), conditions, mutations));

        transactConfig(DATABASENAME, operations);
        log.info("Created mirror {}", mirror.mirroringName());
        return true;
    }

    @Override
    public void dropMirror(MirroringName mirroringName) {
        String mirrorUuid = getMirrorUuid(mirroringName.name());
        if (mirrorUuid != null) {
            log.info("Deleted mirror {}", mirroringName.name());
            deleteConfig(MIRROR, UUID, mirrorUuid, BRIDGE, MIRRORS, Uuid.uuid(mirrorUuid));
        }
        log.warn("Unable to delete {}", mirroringName.name());
        return;
    }

    @Override
    public boolean createInterface(String bridgeName, OvsdbInterface ovsdbIface) {
        String bridgeUuid  = getBridgeUuid(bridgeName);
        if (bridgeUuid == null) {
            log.warn("Couldn't find bridge {} in {}", bridgeName, nodeId.getIpAddress());
            return false;
        }

        if (getPortUuid(ovsdbIface.name(), bridgeUuid) != null) {
            log.warn("Interface {} already exists", ovsdbIface.name());
            return false;
        }

        ArrayList<Operation> operations = Lists.newArrayList();
        DatabaseSchema dbSchema = schema.get(DATABASENAME);

        // insert a new port with the interface name
        Port port = (Port) TableGenerator.createTable(dbSchema, OvsdbTable.PORT);
        port.setName(ovsdbIface.name());
        Insert portInsert = new Insert(dbSchema.getTableSchema(PORT), PORT, port.getRow());
        portInsert.getRow().put(INTERFACES, Uuid.uuid(INTERFACE));
        operations.add(portInsert);

        // update the bridge table with the new port
        Condition condition = ConditionUtil.isEqual(UUID, Uuid.uuid(bridgeUuid));
        Mutation mutation = MutationUtil.insert(PORTS, Uuid.uuid(PORT));
        List<Condition> conditions = Lists.newArrayList(condition);
        List<Mutation> mutations = Lists.newArrayList(mutation);
        operations.add(new Mutate(dbSchema.getTableSchema(BRIDGE), conditions, mutations));

        Interface intf = (Interface) TableGenerator.createTable(dbSchema, OvsdbTable.INTERFACE);
        intf.setName(ovsdbIface.name());

        if (ovsdbIface.type() != null) {
            intf.setType(ovsdbIface.typeToString());
        }

        if (ovsdbIface.mtu().isPresent()) {
            Set<Long> mtuSet = Sets.newConcurrentHashSet();
            mtuSet.add(ovsdbIface.mtu().get());
            intf.setMtu(mtuSet);
            intf.setMtuRequest(mtuSet);
        }

        intf.setOptions(ovsdbIface.options());

        ovsdbIface.data().forEach((k, v) -> {
            if (k == Interface.InterfaceColumn.EXTERNALIDS) {
                intf.setExternalIds(v);
            }
        });

        Insert intfInsert = new Insert(dbSchema.getTableSchema(INTERFACE), INTERFACE, intf.getRow());
        operations.add(intfInsert);

        transactConfig(DATABASENAME, operations);
        log.info("Created interface {}", ovsdbIface);
        return true;
    }

    @Override
    public boolean dropInterface(String ifaceName) {
        OvsdbRowStore rowStore = getRowStore(DATABASENAME, BRIDGE);
        if (rowStore == null) {
            log.warn("Failed to get BRIDGE table");
            return false;
        }

        ConcurrentMap<String, Row> bridgeTableRows = rowStore.getRowStore();
        if (bridgeTableRows == null) {
            log.warn("Failed to get BRIDGE table rows");
            return false;
        }

        // interface name is unique
        Optional<String> bridgeId = bridgeTableRows.keySet().stream()
                .filter(uuid -> getPortUuid(ifaceName, uuid) != null)
                .findFirst();

        if (bridgeId.isPresent()) {
            String portId = getPortUuid(ifaceName, bridgeId.get());
            deleteConfig(PORT, UUID, portId, BRIDGE, PORTS, Uuid.uuid(portId));
            return true;
        } else {
            log.warn("Unable to find the interface with name {}", ifaceName);
            return false;
        }
    }

    /**
     * Delete transact config.
     *
     * @param childTableName   child table name
     * @param childColumnName  child column name
     * @param childUuid        child row uuid
     * @param parentTableName  parent table name
     * @param parentColumnName parent column
     * @param referencedValue  referenced value
     */
    private void deleteConfig(String childTableName, String childColumnName,
                              String childUuid, String parentTableName,
                              String parentColumnName, Object referencedValue) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        TableSchema childTableSchema = dbSchema.getTableSchema(childTableName);

        ArrayList<Operation> operations = Lists.newArrayList();
        if (parentTableName != null && parentColumnName != null && referencedValue != null) {
            TableSchema parentTableSchema = dbSchema
                    .getTableSchema(parentTableName);
            ColumnSchema parentColumnSchema = parentTableSchema
                    .getColumnSchema(parentColumnName);
            List<Mutation> mutations = Lists.newArrayList();
            Mutation mutation = MutationUtil.delete(parentColumnSchema.name(), referencedValue);
            mutations.add(mutation);
            List<Condition> conditions = Lists.newArrayList();
            Condition condition = ConditionUtil.includes(parentColumnName, referencedValue);
            conditions.add(condition);
            Mutate op = new Mutate(parentTableSchema, conditions, mutations);
            operations.add(op);
        }

        List<Condition> conditions = Lists.newArrayList();
        Condition condition = ConditionUtil.isEqual(childColumnName, Uuid.uuid(childUuid));
        conditions.add(condition);
        Delete del = new Delete(childTableSchema, conditions);
        operations.add(del);
        transactConfig(DATABASENAME, operations);
    }

    /**
     * Update transact config.
     *
     * @param tableName  table name
     * @param columnName column name
     * @param uuid       uuid
     * @param row        the config data
     */
    private void updateConfig(String tableName, String columnName, String uuid,
                              Row row) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        TableSchema tableSchema = dbSchema.getTableSchema(tableName);

        List<Condition> conditions = Lists.newArrayList();
        Condition condition = ConditionUtil.isEqual(columnName, Uuid.uuid(uuid));
        conditions.add(condition);

        Update update = new Update(tableSchema, row, conditions);

        ArrayList<Operation> operations = Lists.newArrayList();
        operations.add(update);

        transactConfig(DATABASENAME, operations);
    }

    /**
     * Insert transact config.
     *
     * @param childTableName   child table name
     * @param childColumnName  child column name
     * @param parentTableName  parent table name
     * @param parentColumnName parent column
     * @param parentUuid       parent uuid
     * @param row              the config data
     * @return uuid, empty if no uuid is find
     */
    private String insertConfig(String childTableName, String childColumnName,
                                String parentTableName, String parentColumnName,
                                String parentUuid, Row row) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        TableSchema tableSchema = dbSchema.getTableSchema(childTableName);

        Insert insert = new Insert(tableSchema, childTableName, row);

        ArrayList<Operation> operations = Lists.newArrayList();
        operations.add(insert);

        if (parentTableName != null && parentColumnName != null) {
            TableSchema parentTableSchema = dbSchema
                    .getTableSchema(parentTableName);
            ColumnSchema parentColumnSchema = parentTableSchema
                    .getColumnSchema(parentColumnName);

            List<Mutation> mutations = Lists.newArrayList();
            Mutation mutation = MutationUtil.insert(parentColumnSchema.name(),
                                                    Uuid.uuid(childTableName));
            mutations.add(mutation);

            List<Condition> conditions = Lists.newArrayList();
            Condition condition = ConditionUtil.isEqual(UUID, Uuid.uuid(parentUuid));
            conditions.add(condition);

            Mutate op = new Mutate(parentTableSchema, conditions, mutations);
            operations.add(op);
        }
        if (childTableName.equalsIgnoreCase(PORT)) {
            log.debug("Handle port insert");
            Insert intfInsert = handlePortInsertTable(row);

            if (intfInsert != null) {
                operations.add(intfInsert);
            }

            Insert ins = (Insert) operations.get(0);
            ins.getRow().put("interfaces", Uuid.uuid(INTERFACE));
        }

        List<OperationResult> results;
        try {
            results = transactConfig(DATABASENAME, operations)
                    .get(TRANSACTCONFIG_TIMEOUT, TimeUnit.SECONDS);
            return results.get(0).getUuid().value();
        } catch (TimeoutException e) {
            log.warn("TimeoutException thrown while to get result");
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting to get result");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Exception thrown while to get result");
        }

        return null;
    }


    /**
     * Handles port insert.
     *
     * @param portRow   row of port
     * @return insert, empty if null
     */
    private Insert handlePortInsertTable(Row portRow) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);

        TableSchema portTableSchema = dbSchema.getTableSchema(PORT);
        ColumnSchema portColumnSchema = portTableSchema.getColumnSchema("name");

        String portName = (String) portRow.getColumn(portColumnSchema.name()).data();
        Interface inf = (Interface) TableGenerator.createTable(dbSchema, OvsdbTable.INTERFACE);
        inf.setName(portName);

        TableSchema intfTableSchema = dbSchema.getTableSchema(INTERFACE);
        return new Insert(intfTableSchema, INTERFACE, inf.getRow());
    }

    @Override
    public ListenableFuture<DatabaseSchema> getOvsdbSchema(String dbName) {
        if (dbName == null) {
            return null;
        }
        DatabaseSchema databaseSchema = schema.get(dbName);
        if (databaseSchema == null) {
            List<String> dbNames = new ArrayList<>();
            dbNames.add(dbName);
            Function<JsonNode, DatabaseSchema> rowFunction = input -> {
                log.debug("Get ovsdb database schema {}", dbName);
                DatabaseSchema dbSchema = FromJsonUtil.jsonNodeToDbSchema(dbName, input);
                if (dbSchema == null) {
                    log.debug("Get ovsdb database schema error");
                    return null;
                }
                schema.put(dbName, dbSchema);
                return dbSchema;
            };

            ListenableFuture<JsonNode> input = getSchema(dbNames);
            if (input != null) {
                return futureTransform(input, rowFunction);
            }
            return null;
        } else {
            return Futures.immediateFuture(databaseSchema);
        }
    }

    @Override
    public ListenableFuture<TableUpdates> monitorTables(String dbName, String id) {
        if (dbName == null) {
            return null;
        }
        DatabaseSchema dbSchema = schema.get(dbName);
        if (dbSchema != null) {
            Function<JsonNode, TableUpdates> rowFunction = input -> {
                log.debug("Get table updates");
                TableUpdates updates = FromJsonUtil.jsonNodeToTableUpdates(input, dbSchema);
                if (updates == null) {
                    log.debug("Get table updates error");
                    return null;
                }
                return updates;
            };
            return futureTransform(monitor(dbSchema, id), rowFunction);
        }
        return null;
    }

    private ListenableFuture<List<OperationResult>> transactConfig(String dbName,
                                                                   List<Operation> operations) {
        if (dbName == null) {
            return null;
        }
        DatabaseSchema dbSchema = schema.get(dbName);
        if (dbSchema != null) {
            Function<List<JsonNode>, List<OperationResult>> rowFunction = (input -> {
                try {
                    log.debug("Get ovsdb operation result");
                    List<OperationResult> result = FromJsonUtil.jsonNodeToOperationResult(input, operations);
                    if (result == null) {
                        log.debug("The operation result is null");
                        return null;
                    }
                    return result;
                } catch (Exception e) {
                    log.error("Exception while parsing result", e);
                }
                return null;
            });
            return futureTransform(transact(dbSchema, operations), rowFunction);
        }
        return null;
    }

    @Override
    public ListenableFuture<JsonNode> getSchema(List<String> dbnames) {
        String id = java.util.UUID.randomUUID().toString();
        String getSchemaString = JsonRpcWriterUtil.getSchemaStr(id, dbnames);

        SettableFuture<JsonNode> sf = SettableFuture.create();
        requestResult.put(id, sf);
        requestMethod.put(id, "getSchema");

        channel.writeAndFlush(getSchemaString);
        return sf;
    }

    @Override
    public ListenableFuture<List<String>> echo() {
        String id = java.util.UUID.randomUUID().toString();
        String echoString = JsonRpcWriterUtil.echoStr(id);

        SettableFuture<List<String>> sf = SettableFuture.create();
        requestResult.put(id, sf);
        requestMethod.put(id, "echo");

        channel.writeAndFlush(echoString);
        return sf;
    }

    @Override
    public ListenableFuture<JsonNode> monitor(DatabaseSchema dbSchema,
                                              String monitorId) {
        String id = java.util.UUID.randomUUID().toString();
        String monitorString = JsonRpcWriterUtil.monitorStr(id, monitorId,
                                                            dbSchema);

        SettableFuture<JsonNode> sf = SettableFuture.create();
        requestResult.put(id, sf);
        requestMethod.put(id, "monitor");

        channel.writeAndFlush(monitorString);
        return sf;
    }

    @Override
    public ListenableFuture<List<String>> listDbs() {
        String id = java.util.UUID.randomUUID().toString();
        String listDbsString = JsonRpcWriterUtil.listDbsStr(id);

        SettableFuture<List<String>> sf = SettableFuture.create();
        requestResult.put(id, sf);
        requestMethod.put(id, "listDbs");

        channel.writeAndFlush(listDbsString);
        return sf;
    }

    @Override
    public ListenableFuture<List<JsonNode>> transact(DatabaseSchema dbSchema,
                                                     List<Operation> operations) {
        String id = java.util.UUID.randomUUID().toString();
        String transactString = JsonRpcWriterUtil.transactStr(id, dbSchema,
                                                              operations);

        SettableFuture<List<JsonNode>> sf = SettableFuture.create();
        requestResult.put(id, sf);
        requestMethod.put(id, "transact");
        channel.writeAndFlush(transactString);
        return sf;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void processResult(JsonNode response) {
        log.debug("Handle result");
        String requestId = response.get("id").asText();
        SettableFuture sf = requestResult.get(requestId);
        if (sf == null) {
            log.debug("No such future to process");
            return;
        }
        String methodName = requestMethod.get(requestId);
        sf.set(FromJsonUtil.jsonResultParser(response, methodName));

        requestResult.remove(requestId);
        requestMethod.remove(requestId);
    }

    @Override
    public void processRequest(JsonNode requestJson) {
        log.debug("Handle request");
        if (requestJson.get("method").asText().equalsIgnoreCase("echo")) {
            log.debug("handle echo request");

            String replyString = FromJsonUtil.getEchoRequestStr(requestJson);
            channel.writeAndFlush(replyString);
        } else {
            FromJsonUtil.jsonCallbackRequestParser(requestJson, monitorCallBack);
        }
    }

    @Override
    public void setCallback(Callback monitorCallback) {
        this.monitorCallBack = monitorCallback;
    }

    @Override
    public Set<OvsdbBridge> getBridges() {
        Set<OvsdbBridge> ovsdbBridges = new HashSet<>();
        OvsdbTableStore tableStore = getTableStore(DATABASENAME);
        if (tableStore == null) {
            return ovsdbBridges;
        }
        OvsdbRowStore rowStore = tableStore.getRows(BRIDGE);
        if (rowStore == null) {
            return ovsdbBridges;
        }
        ConcurrentMap<String, Row> rows = rowStore.getRowStore();
        for (String uuid : rows.keySet()) {
            Row bridgeRow = getRow(DATABASENAME, BRIDGE, uuid);
            OvsdbBridge ovsdbBridge = getOvsdbBridge(bridgeRow, Uuid.uuid(uuid));
            if (ovsdbBridge != null) {
                ovsdbBridges.add(ovsdbBridge);
            }
        }
        return ovsdbBridges;
    }

    @Override
    public Set<ControllerInfo> getControllers(DeviceId openflowDeviceId) {
        Uuid bridgeUuid = getBridgeUuid(openflowDeviceId);
        if (bridgeUuid == null) {
            log.warn("bad bridge Uuid");
            return null;
        }
        List<Controller> controllers = getControllers(bridgeUuid);
        if (controllers == null) {
            log.warn("bad list of controllers");
            return null;
        }
        return controllers.stream().map(controller -> new ControllerInfo(
                (String) controller.getTargetColumn()
                        .data())).collect(Collectors.toSet());
    }

    private List<Controller> getControllers(Uuid bridgeUuid) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        if (dbSchema == null) {
            return null;
        }
        OvsdbRowStore rowStore = getRowStore(DATABASENAME, BRIDGE);
        if (rowStore == null) {
            log.debug("There is no bridge table");
            return null;
        }

        Row bridgeRow = rowStore.getRow(bridgeUuid.value());
        Bridge bridge = (Bridge) TableGenerator.
                getTable(dbSchema, bridgeRow, OvsdbTable.BRIDGE);

        //FIXME remove log
        log.warn("type of controller column", bridge.getControllerColumn()
                .data().getClass());
        Set<Uuid> controllerUuids = (Set<Uuid>) ((OvsdbSet) bridge
                .getControllerColumn().data()).set();

        OvsdbRowStore controllerRowStore = getRowStore(DATABASENAME, CONTROLLER);
        if (controllerRowStore == null) {
            log.debug("There is no controller table");
            return null;
        }

        List<Controller> ovsdbControllers = new ArrayList<>();
        ConcurrentMap<String, Row> controllerTableRows = controllerRowStore.getRowStore();
        controllerTableRows.forEach((key, row) -> {
            if (!controllerUuids.contains(Uuid.uuid(key))) {
                return;
            }
            Controller controller = (Controller) TableGenerator
                    .getTable(dbSchema, row, OvsdbTable.CONTROLLER);
            ovsdbControllers.add(controller);
        });
        return ovsdbControllers;
    }


    private Uuid getBridgeUuid(DeviceId openflowDeviceId) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        if (dbSchema == null) {
            return null;
        }
        OvsdbRowStore rowStore = getRowStore(DATABASENAME, BRIDGE);
        if (rowStore == null) {
            log.debug("There is no bridge table");
            return null;
        }

        ConcurrentMap<String, Row> bridgeTableRows = rowStore.getRowStore();
        final AtomicReference<Uuid> uuid = new AtomicReference<>();
        for (Map.Entry<String, Row> entry : bridgeTableRows.entrySet()) {
            Bridge bridge = (Bridge) TableGenerator.getTable(
                    dbSchema,
                    entry.getValue(),
                    OvsdbTable.BRIDGE);

            if (matchesDpid(bridge, openflowDeviceId)) {
                uuid.set(Uuid.uuid(entry.getKey()));
                break;
            }
        }
        if (uuid.get() == null) {
            log.debug("There is no bridge for {}", openflowDeviceId);
        }
        return uuid.get();
    }

    private static boolean matchesDpid(Bridge b, DeviceId deviceId) {
        String ofDpid = deviceId.toString().replace("of:", "");
        Set ofDeviceIds = ((OvsdbSet) b.getDatapathIdColumn().data()).set();
        //TODO Set<String>
        return ofDeviceIds.contains(ofDpid);
    }

    @Override
    public Set<OvsdbPort> getPorts() {
        return (Set<OvsdbPort>) getElements(this::getOvsdbPort);
    }

    @Override
    public Set<Interface> getInterfaces() {
        return (Set<Interface>) getElements(this::getInterface);
    }

    private Set<?> getElements(Function<Row, ?> method) {
        OvsdbTableStore tableStore = getTableStore(DATABASENAME);
        if (tableStore == null) {
            return null;
        }
        OvsdbRowStore rowStore = tableStore.getRows(INTERFACE);
        if (rowStore == null) {
            return null;
        }
        ConcurrentMap<String, Row> rows = rowStore.getRowStore();

        return rows.keySet()
                .stream()
                .map(uuid -> getRow(DATABASENAME, INTERFACE, uuid))
                .map(method)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Interface getInterface(String intf) {
        return getInterfaces().stream()
                .filter(ovsdbIntf -> ovsdbIntf.getName().equals(intf))
                .findAny().orElse(null);
    }

    private Interface getInterface(Row row) {
        DatabaseSchema dbSchema = getDatabaseSchema(DATABASENAME);
        Interface intf = (Interface) TableGenerator
                .getTable(dbSchema, row, OvsdbTable.INTERFACE);
        if (intf == null) {
            return null;
        }
        return intf;
    }
    @Override
    public DatabaseSchema getDatabaseSchema(String dbName) {
        return schema.get(dbName);
    }

    private OvsdbPort getOvsdbPort(Row row) {
        DatabaseSchema dbSchema = getDatabaseSchema(DATABASENAME);
        Interface intf = (Interface) TableGenerator
                .getTable(dbSchema, row, OvsdbTable.INTERFACE);
        if (intf == null) {
            return null;
        }
        long ofPort = getOfPort(intf);
        String portName = intf.getName();
        if ((ofPort < 0) || (portName == null)) {
            return null;
        }
        return new OvsdbPort(new OvsdbPortNumber(ofPort), new OvsdbPortName(portName));
    }

    private OvsdbBridge getOvsdbBridge(Row row, Uuid bridgeUuid) {
        DatabaseSchema dbSchema = getDatabaseSchema(DATABASENAME);
        Bridge bridge = (Bridge) TableGenerator.getTable(dbSchema, row, OvsdbTable.BRIDGE);
        if (bridge == null) {
            return null;
        }

        OvsdbSet datapathIdSet = (OvsdbSet) bridge.getDatapathIdColumn().data();
        @SuppressWarnings("unchecked")
        Set<String> datapathIds = datapathIdSet.set();
        if (datapathIds == null || datapathIds.isEmpty()) {
            return null;
        }
        String datapathId = (String) datapathIds.toArray()[0];
        String bridgeName = bridge.getName();
        if ((datapathId == null) || (bridgeName == null)) {
            return null;
        }

        List<Controller> controllers = getControllers(bridgeUuid);

        if (controllers != null) {
            List<ControllerInfo> controllerInfos = controllers.stream().map(
                    controller -> new ControllerInfo(
                    (String) controller.getTargetColumn()
                            .data())).collect(Collectors.toList());

            return OvsdbBridge.builder()
                    .name(bridgeName)
                    .datapathId(datapathId)
                    .controllers(controllerInfos)
                    .build();
        } else {
            return OvsdbBridge.builder()
                    .name(bridgeName)
                    .datapathId(datapathId)
                    .build();
        }
    }

    private OvsdbQos getOvsdbQos(Row row) {
        DatabaseSchema dbSchema = getDatabaseSchema(DATABASENAME);
        Qos qos = (Qos) TableGenerator.getTable(dbSchema, row, OvsdbTable.QOS);
        if (qos == null) {
            return null;
        }

        String type = (String) qos.getTypeColumn().data();
        Map<String, String> otherConfigs;
        Map<String, String> externalIds;
        Map<Long, String> queues;

        otherConfigs = ((OvsdbMap) qos.getOtherConfigColumn().data()).map();
        externalIds  = ((OvsdbMap) qos.getExternalIdsColumn().data()).map();
        queues = ((OvsdbMap) qos.getQueuesColumn().data()).map();
        return OvsdbQos.builder().qosType(type).
                queues(queues).otherConfigs(otherConfigs).
                externalIds(externalIds).build();
    }

    private OvsdbQueue getOvsdbQueue(Row row) {
        DatabaseSchema dbSchema = getDatabaseSchema(DATABASENAME);
        Queue queue = (Queue) TableGenerator.getTable(dbSchema, row, OvsdbTable.QUEUE);
        if (queue == null) {
            return null;
        }

        OvsdbSet dscpOvsdbSet = ((OvsdbSet) queue.getDscpColumn().data());
        Set dscpSet = dscpOvsdbSet.set();
        Long dscp = null;
        if (dscpSet != null && !dscpSet.isEmpty()) {
            dscp = Long.valueOf(dscpSet.toArray()[0].toString());
        }

        Map<String, String> otherConfigs;
        Map<String, String> externalIds;

        otherConfigs = ((OvsdbMap) queue.getOtherConfigColumn().data()).map();
        externalIds  = ((OvsdbMap) queue.getExternalIdsColumn().data()).map();
        return OvsdbQueue.builder().dscp(dscp).
                otherConfigs(otherConfigs).externalIds(externalIds).build();
    }

    private long getOfPort(Interface intf) {
        OvsdbSet ofPortSet = (OvsdbSet) intf.getOpenFlowPortColumn().data();
        @SuppressWarnings("unchecked")
        Set<Integer> ofPorts = ofPortSet.set();
        if (ofPorts == null || ofPorts.isEmpty()) {
            log.debug("The ofport is null in {}", intf.getName());
            return -1;
        }
        // return (long) ofPorts.toArray()[0];
        Iterator<Integer> it = ofPorts.iterator();
        return Long.parseLong(it.next().toString());
    }

    @Override
    public Set<OvsdbPort> getLocalPorts(Iterable<String> ifaceids) {
        Set<OvsdbPort> ovsdbPorts = new HashSet<>();
        OvsdbTableStore tableStore = getTableStore(DATABASENAME);
        if (tableStore == null) {
            return null;
        }
        OvsdbRowStore rowStore = tableStore.getRows(INTERFACE);
        if (rowStore == null) {
            return null;
        }
        ConcurrentMap<String, Row> rows = rowStore.getRowStore();
        for (String uuid : rows.keySet()) {
            Row row = getRow(DATABASENAME, INTERFACE, uuid);
            DatabaseSchema dbSchema = getDatabaseSchema(DATABASENAME);
            Interface intf = (Interface) TableGenerator
                    .getTable(dbSchema, row, OvsdbTable.INTERFACE);
            if (intf == null || getIfaceid(intf) == null) {
                continue;
            }
            String portName = intf.getName();
            if (portName == null) {
                continue;
            }
            Set<String> ifaceidSet = Sets.newHashSet(ifaceids);
            if (portName.startsWith(TYPEVXLAN) || !ifaceidSet.contains(getIfaceid(intf))) {
                continue;
            }
            long ofPort = getOfPort(intf);
            if (ofPort < 0) {
                continue;
            }
            ovsdbPorts.add(new OvsdbPort(new OvsdbPortNumber(ofPort),
                                         new OvsdbPortName(portName)));
        }
        return ovsdbPorts;
    }

    private String getIfaceid(Interface intf) {
        OvsdbMap ovsdbMap = (OvsdbMap) intf.getExternalIdsColumn().data();
        @SuppressWarnings("unchecked")
        Map<String, String> externalIds = ovsdbMap.map();
        if (externalIds.isEmpty()) {
            log.warn("The external_ids is null");
            return null;
        }
        String ifaceid = externalIds.get(EXTERNAL_ID_INTERFACE_ID);
        if (ifaceid == null) {
            log.warn("The ifaceid is null");
            return null;
        }
        return ifaceid;
    }

    @Override
    public void disconnect() {
        channel.disconnect();
    }

    @Override
    public List<OvsdbPortName> getPorts(List<String> portNames, DeviceId deviceId) {
        Uuid bridgeUuid = getBridgeUuid(deviceId);
        if (bridgeUuid == null) {
            log.error("Can't find the bridge for the deviceId {}", deviceId);
            return Collections.emptyList();
        }
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        Row bridgeRow = getRow(DATABASENAME, BRIDGE, bridgeUuid.value());
        Bridge bridge = (Bridge) TableGenerator.getTable(dbSchema, bridgeRow, OvsdbTable.BRIDGE);
        if (bridge == null) {
            return Collections.emptyList();
        }
        OvsdbSet setPorts = (OvsdbSet) bridge.getPortsColumn().data();
        Set<Uuid> portSet = setPorts.set();
        if (portSet.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Uuid, Port> portMap = portSet.stream().collect(Collectors.toMap(
                java.util.function.Function.identity(), port -> (Port) TableGenerator
                        .getTable(dbSchema, getRow(DATABASENAME,
                                PORT, port.value()), OvsdbTable.PORT)));

        List<OvsdbPortName> portList = portMap.entrySet().stream().filter(port -> Objects.nonNull(port.getValue())
                && portNames.contains(port.getValue().getName())
                && Objects.nonNull(getInterfacebyPort(port.getKey().value(), port.getValue().getName())))
                .map(port -> new OvsdbPortName(port.getValue().getName())).collect(Collectors.toList());

        return Collections.unmodifiableList(portList);
    }

    @Override
    public boolean getPortError(List<OvsdbPortName> portNames, DeviceId bridgeId) {
        Uuid bridgeUuid = getBridgeUuid(bridgeId);

        List<Interface> interfaceList = portNames.stream().collect(Collectors
                .toMap(java.util.function.Function.identity(),
                        port -> (Interface) getInterfacebyPort(getPortUuid(port.value(),
                                bridgeUuid.value()), port.value())))
                .entrySet().stream().filter(intf -> Objects.nonNull(intf.getValue())
                        && ((OvsdbSet) intf.getValue().getOpenFlowPortColumn().data()).set()
                        .stream().findAny().orElse(OFPORT_ERROR_COMPARISON).equals(OFPORT_ERROR))
                .map(Map.Entry::getValue).collect(Collectors.toList());

        interfaceList.forEach(intf -> ((Consumer<Interface>) intf1 -> {
            try {
                Set<String> setErrors = ((OvsdbSet) intf1.getErrorColumn().data()).set();
                log.info("Port has errors. ofport value - {}, Interface - {} has error - {} ",
                        intf1.getOpenFlowPortColumn().data(), intf1.getName(), setErrors.stream()
                                .findFirst().get());
            } catch (ColumnSchemaNotFoundException | VersionMismatchException e) {
                log.debug("Port has errors. ofport value - {}, Interface - {} has error - {} ",
                        intf1.getOpenFlowPortColumn().data(), intf1.getName(), e);
            }
        }).accept(intf));

        return !interfaceList.isEmpty();
    }

    private Interface getInterfacebyPort(String portUuid, String portName) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);

        Row portRow = getRow(DATABASENAME, PORT, portUuid);
        Port port = (Port) TableGenerator.getTable(dbSchema, portRow,
                OvsdbTable.PORT);
        if (port == null) {
            return null;
        }

        OvsdbSet setInterfaces = (OvsdbSet) port.getInterfacesColumn().data();
        Set<Uuid> interfaces = setInterfaces.set();

        return interfaces.stream().map(intf -> (Interface) TableGenerator
                .getTable(dbSchema, getRow(DATABASENAME,
                        INTERFACE, intf.value()), OvsdbTable.INTERFACE))
                .filter(intf -> Objects.nonNull(intf) && portName.equalsIgnoreCase(intf.getName()))
                .findFirst().orElse(null);
    }

    @Override
    public Optional<Object> getFirstRow(String dbName, OvsdbTable tblName) {

        DatabaseSchema dbSchema = getDatabaseSchema(dbName);
        if (Objects.isNull(dbSchema)) {
            return Optional.empty();
        }

        OvsdbTableStore tableStore = ovsdbStore.getOvsdbTableStore(dbName);
        if (tableStore == null) {
            return Optional.empty();
        }
        OvsdbRowStore rowStore = tableStore.getRows(tblName.tableName());
        if (rowStore == null) {
            return Optional.empty();
        }

        ConcurrentMap<String, Row> rows = rowStore.getRowStore();
        if (rows == null) {
            log.debug("The {} Table Rows is null", tblName);
            return Optional.empty();
        }

        // There should be only 1 row in this table
        Optional<String> uuid = rows.keySet().stream().findFirst();
        if (uuid.isPresent() && rows.containsKey(uuid.get())) {
            return Optional.of(TableGenerator.getTable(dbSchema,
                    rows.get(uuid.get()), tblName));
        } else {
            return Optional.empty();
        }
    }


    @Override
    public Optional<DeviceMemoryStats> getDeviceMemoryUsage() {
        return Optional.empty();
    }


    @Override
    public Optional<DeviceCpuStats> getDeviceCpuUsage() {
        return Optional.empty();
    }

    private <I, O> ListenableFuture<O> futureTransform(
            ListenableFuture<I> input, Function<? super I, ? extends O> function) {
        // Wrapper around deprecated Futures.transform() method. As per Guava
        // recommendation, passing MoreExecutors.directExecutor() for identical
        // behavior.
        return Futures.transform(input, function, MoreExecutors.directExecutor());
    }
}