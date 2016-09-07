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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import io.netty.channel.Channel;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.MirroringStatistics;
import org.onosproject.net.behaviour.MirroringName;
import org.onosproject.ovsdb.controller.OvsdbBridge;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbInterface;
import org.onosproject.ovsdb.controller.OvsdbInterface.Type;
import org.onosproject.ovsdb.controller.OvsdbMirror;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbPort;
import org.onosproject.ovsdb.controller.OvsdbPortName;
import org.onosproject.ovsdb.controller.OvsdbPortNumber;
import org.onosproject.ovsdb.controller.OvsdbRowStore;
import org.onosproject.ovsdb.controller.OvsdbStore;
import org.onosproject.ovsdb.controller.OvsdbTableStore;

import org.onosproject.ovsdb.rfc.jsonrpc.Callback;
import org.onosproject.ovsdb.rfc.message.OperationResult;
import org.onosproject.ovsdb.rfc.message.TableUpdates;
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
import org.onosproject.ovsdb.rfc.table.TableGenerator;
import org.onosproject.ovsdb.rfc.utils.ConditionUtil;
import org.onosproject.ovsdb.rfc.utils.FromJsonUtil;
import org.onosproject.ovsdb.rfc.utils.JsonRpcWriterUtil;
import org.onosproject.ovsdb.rfc.utils.MutationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.onosproject.ovsdb.controller.OvsdbConstant.*;

/**
 * An representation of an ovsdb client.
 */
public class DefaultOvsdbClient implements OvsdbProviderService, OvsdbClientService {

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

    /**
     * Gets the ovsdb row.
     *
     * @param dbName    the ovsdb database name
     * @param tableName the ovsdb table name
     * @param uuid      the key of the row
     * @return row, empty if row is find
     */
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

    /**
     * Gets the Mirror uuid.
     *
     * @param mirrorName mirror name
     * @return mirror uuid, empty if no uuid is found
     */
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

    /**
     * Gets mirrors of the device.
     *
     * @param deviceId target device id
     * @return set of mirroring; empty if no mirror is found
     */
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
            if (ports == null || ports.size() == 0) {
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
        } else {
            updateConfig(PORT, UUID, portUuid, port.getRow());
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
            deleteConfig(PORT, UUID, portUuid, BRIDGE, PORTS);
        }
    }

    @Deprecated
    @Override
    public void createBridge(String bridgeName) {
        OvsdbBridge ovsdbBridge = OvsdbBridge.builder()
                .name(bridgeName)
                .build();

        createBridge(ovsdbBridge);
    }

    @Deprecated
    @Override
    public void createBridge(String bridgeName, String dpid, String exPortName) {
        OvsdbBridge ovsdbBridge = OvsdbBridge.builder()
                .name(bridgeName)
                .failMode(BridgeDescription.FailMode.SECURE)
                .datapathId(dpid)
                .disableInBand()
                .controllers(Lists.newArrayList(localController()))
                .build();

        createBridge(ovsdbBridge);

        if (exPortName != null) {
            createPort(bridgeName, exPortName);
        }
    }

    @Deprecated
    @Override
    public boolean createBridge(String bridgeName, String dpid, List<ControllerInfo> controllers) {
        OvsdbBridge ovsdbBridge = OvsdbBridge.builder()
                .name(bridgeName)
                .failMode(BridgeDescription.FailMode.SECURE)
                .datapathId(dpid)
                .disableInBand()
                .controllers(controllers)
                .build();

        return createBridge(ovsdbBridge);
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

        Set<Uuid> newControllerUuids = new HashSet<>();

        Set<ControllerInfo> newControllers = new HashSet<>(controllers);
        List<Controller> removeControllers = new ArrayList<>();
        oldControllers.forEach(controller -> {
            ControllerInfo controllerInfo = new ControllerInfo((String) controller.getTargetColumn().data());
            if (newControllers.contains(controllerInfo)) {
                newControllers.remove(controllerInfo);
                newControllerUuids.add(controller.getRow().uuid());
            } else {
                removeControllers.add(controller);
            }
        });
        OvsdbRowStore controllerRowStore = getRowStore(DATABASENAME, CONTROLLER);
        if (controllerRowStore == null) {
            log.debug("There is no controller table");
            return;
        }

        removeControllers.forEach(c -> deleteConfig(CONTROLLER, UUID, c.getRow().uuid().value(),
                                                    BRIDGE, "controller"));
        newControllers.stream().map(c -> {
            Controller controller = (Controller) TableGenerator
                    .createTable(dbSchema, OvsdbTable.CONTROLLER);
            controller.setTarget(c.target());
            return controller;
        }).forEach(c -> {
            String uuid = insertConfig(CONTROLLER, UUID, BRIDGE, "controller", bridgeUuid.value(),
                                       c.getRow());
            newControllerUuids.add(Uuid.uuid(uuid));

        });

        OvsdbRowStore rowStore = getRowStore(DATABASENAME, BRIDGE);
        if (rowStore == null) {
            log.debug("There is no bridge table");
            return;
        }

        Row bridgeRow = rowStore.getRow(bridgeUuid.value());
        Bridge bridge = (Bridge) TableGenerator.getTable(dbSchema, bridgeRow, OvsdbTable.BRIDGE);
        bridge.setController(OvsdbSet.ovsdbSet(newControllerUuids));
        updateConfig(BRIDGE, UUID, bridgeUuid.value(), bridge.getRow());
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
        deleteConfig(BRIDGE, UUID, bridgeUuid, DATABASENAME, BRIDGES);
    }

    /**
     * Creates a mirror port. Mirrors the traffic
     * that goes to selectDstPort or comes from
     * selectSrcPort or packets containing selectVlan
     * to mirrorPort or to all ports that trunk mirrorVlan.
     *
     * @param mirror the OVSDB mirror description
     * @return true if mirror creation is successful, false otherwise
     */
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

        if (mirror.monitorDstPorts().size() == 0 &&
                mirror.monitorSrcPorts().size() == 0 &&
                mirror.monitorVlans().size() == 0) {
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

    /**
     * Drops the configuration for mirror.
     *
     * @param mirroringName
     */
    @Override
    public void dropMirror(MirroringName mirroringName) {
        String mirrorUuid = getMirrorUuid(mirroringName.name());
        if (mirrorUuid != null) {
            log.info("Deleted mirror {}", mirroringName.name());
            deleteConfig(MIRROR, UUID, mirrorUuid, BRIDGE, MIRRORS);
        }
        log.warn("Unable to delete {}", mirroringName.name());
        return;
    }

    @Deprecated
    @Override
    public boolean createTunnel(String bridgeName, String ifaceName, String tunnelType,
                                Map<String, String> options) {
        OvsdbInterface ovsdbIface = OvsdbInterface.builder()
                .name(ifaceName)
                .type(Type.valueOf(tunnelType))
                .options(options)
                .build();

        return createInterface(bridgeName, ovsdbIface);
    }

    @Deprecated
    @Override
    public void dropTunnel(IpAddress srcIp, IpAddress dstIp) {
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
            // remove existing one and re-create?
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

        // insert an interface
        Interface intf = (Interface) TableGenerator.createTable(dbSchema, OvsdbTable.INTERFACE);
        intf.setName(ovsdbIface.name());
        intf.setType(ovsdbIface.typeToString());
        intf.setOptions(ovsdbIface.options());
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
            deleteConfig(PORT, UUID, portId, BRIDGE, PORTS);
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
     */
    private void deleteConfig(String childTableName, String childColumnName,
                              String childUuid, String parentTableName,
                              String parentColumnName) {
        DatabaseSchema dbSchema = schema.get(DATABASENAME);
        TableSchema childTableSchema = dbSchema.getTableSchema(childTableName);

        ArrayList<Operation> operations = Lists.newArrayList();
        if (parentTableName != null && parentColumnName != null) {
            TableSchema parentTableSchema = dbSchema
                    .getTableSchema(parentTableName);
            ColumnSchema parentColumnSchema = parentTableSchema
                    .getColumnSchema(parentColumnName);
            List<Mutation> mutations = Lists.newArrayList();
            Mutation mutation = MutationUtil.delete(parentColumnSchema.name(),
                                                    Uuid.uuid(childUuid));
            mutations.add(mutation);
            List<Condition> conditions = Lists.newArrayList();
            Condition condition = ConditionUtil.includes(parentColumnName,
                                                         Uuid.uuid(childUuid));
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
            results = transactConfig(DATABASENAME, operations).get();
            return results.get(0).getUuid().value();
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
            List<String> dbNames = new ArrayList<String>();
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
                return Futures.transform(input, rowFunction);
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
            return Futures.transform(monitor(dbSchema, id), rowFunction);
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
                log.debug("Get ovsdb operation result");
                List<OperationResult> result = FromJsonUtil.jsonNodeToOperationResult(input, operations);
                if (result == null) {
                    log.debug("The operation result is null");
                    return null;
                }
                return result;
            });
            return Futures.transform(transact(dbSchema, operations), rowFunction);
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
            Row row = getRow(DATABASENAME, BRIDGE, uuid);
            OvsdbBridge ovsdbBridge = getOvsdbBridge(row);
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
            OvsdbPort ovsdbPort = getOvsdbPort(row);
            if (ovsdbPort != null) {
                ovsdbPorts.add(ovsdbPort);
            }
        }
        return ovsdbPorts;
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

    private OvsdbBridge getOvsdbBridge(Row row) {
        DatabaseSchema dbSchema = getDatabaseSchema(DATABASENAME);
        Bridge bridge = (Bridge) TableGenerator.getTable(dbSchema, row, OvsdbTable.BRIDGE);
        if (bridge == null) {
            return null;
        }

        OvsdbSet datapathIdSet = (OvsdbSet) bridge.getDatapathIdColumn().data();
        @SuppressWarnings("unchecked")
        Set<String> datapathIds = datapathIdSet.set();
        if (datapathIds == null || datapathIds.size() == 0) {
            return null;
        }
        String datapathId = (String) datapathIds.toArray()[0];
        String bridgeName = bridge.getName();
        if ((datapathId == null) || (bridgeName == null)) {
            return null;
        }
        return OvsdbBridge.builder().name(bridgeName).datapathId(datapathId).build();
    }

    private long getOfPort(Interface intf) {
        OvsdbSet ofPortSet = (OvsdbSet) intf.getOpenFlowPortColumn().data();
        @SuppressWarnings("unchecked")
        Set<Integer> ofPorts = ofPortSet.set();
        if (ofPorts == null || ofPorts.size() <= 0) {
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
        this.agent.removeConnectedNode(nodeId);
    }
}
