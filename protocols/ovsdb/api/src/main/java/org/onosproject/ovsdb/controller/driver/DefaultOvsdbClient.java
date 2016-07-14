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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import io.netty.channel.Channel;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.ovsdb.controller.OvsdbBridge;
import org.onosproject.ovsdb.controller.OvsdbBridgeName;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbConstant;
import org.onosproject.ovsdb.controller.OvsdbDatapathId;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbPort;
import org.onosproject.ovsdb.controller.OvsdbPortName;
import org.onosproject.ovsdb.controller.OvsdbPortNumber;
import org.onosproject.ovsdb.controller.OvsdbRowStore;
import org.onosproject.ovsdb.controller.OvsdbStore;
import org.onosproject.ovsdb.controller.OvsdbTableStore;
import org.onosproject.ovsdb.controller.OvsdbTunnel;
import org.onosproject.ovsdb.rfc.exception.BridgeCreateException;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * An representation of an ovsdb client.
 */
public class DefaultOvsdbClient
        implements OvsdbProviderService, OvsdbClientService {

    private final Logger log = LoggerFactory
            .getLogger(DefaultOvsdbClient.class);

    private Channel channel;

    private OvsdbAgent agent;
    private boolean connected;
    private OvsdbNodeId nodeId;
    private Callback monitorCallBack;

    private OvsdbStore ovsdbStore = new OvsdbStore();

    private final Map<String, String> requestMethod = Maps.newHashMap();
    private final Map<String, SettableFuture<? extends Object>> requestResult = Maps
            .newHashMap();

    private final Map<String, DatabaseSchema> schema = Maps.newHashMap();
    private final Set<OvsdbTunnel> ovsdbTunnels = new HashSet<OvsdbTunnel>();

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

    @Override
    public String getPortUuid(String portName, String bridgeUuid) {
        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);

        Row bridgeRow = getRow(OvsdbConstant.DATABASENAME,
                               OvsdbConstant.BRIDGE, bridgeUuid);

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
                Row portRow = getRow(OvsdbConstant.DATABASENAME,
                                     OvsdbConstant.PORT, uuid.value());
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
    public String getInterfaceUuid(String portUuid, String portName) {
        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);

        Row portRow = getRow(OvsdbConstant.DATABASENAME, OvsdbConstant.PORT,
                             portUuid);
        Port port = (Port) TableGenerator.getTable(dbSchema, portRow,
                                                   OvsdbTable.PORT);

        if (port != null) {
            OvsdbSet setInterfaces = (OvsdbSet) port.getInterfacesColumn().data();
            @SuppressWarnings("unchecked")
            Set<Uuid> interfaces = setInterfaces.set();

            if (interfaces == null || interfaces.size() == 0) {
                log.warn("The interface uuid is null");
                return null;
            }

            for (Uuid uuid : interfaces) {
                Row intfRow = getRow(OvsdbConstant.DATABASENAME,
                                     OvsdbConstant.INTERFACE, uuid.value());
                Interface intf = (Interface) TableGenerator
                        .getTable(dbSchema, intfRow, OvsdbTable.INTERFACE);
                if (intf != null && portName.equalsIgnoreCase(intf.getName())) {
                    return uuid.value();
                }
            }

        }

        return null;
    }

    @Override
    public String getBridgeUuid(String bridgeName) {
        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);

        OvsdbRowStore rowStore = getRowStore(OvsdbConstant.DATABASENAME,
                                             OvsdbConstant.BRIDGE);
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
                    .getTable(dbSchema, bridgeTableRows.get(uuid),
                              OvsdbTable.BRIDGE);

            if (bridge.getName().equals(bridgeName)) {
                return uuid;
            }

        }
        return null;
    }

    @Override
    public String getControllerUuid(String controllerName,
                                    String controllerTarget) {
        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);
        OvsdbRowStore rowStore = getRowStore(OvsdbConstant.DATABASENAME,
                                             OvsdbConstant.CONTROLLER);
        if (rowStore == null) {
            log.debug("The controller uuid is null");
            return null;
        }

        ConcurrentMap<String, Row> controllerTableRows = rowStore.getRowStore();
        if (controllerTableRows != null) {
            for (String uuid : controllerTableRows.keySet()) {

                Controller controller = (Controller) TableGenerator
                        .getTable(dbSchema, controllerTableRows.get(uuid),
                                  OvsdbTable.CONTROLLER);
                String target = (String) controller.getTargetColumn().data();
                if (target.equalsIgnoreCase(controllerTarget)) {
                    return uuid;
                }

            }
        }
        return null;
    }

    @Override
    public String getOvsUuid(String dbName) {
        OvsdbRowStore rowStore = getRowStore(OvsdbConstant.DATABASENAME,
                                             OvsdbConstant.DATABASENAME);
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
            log.error("Can't find bridge {} in {}", bridgeName,
                      nodeId.getIpAddress());
            return;
        }

        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);
        String portUuid = getPortUuid(portName, bridgeUuid);

        Port port = (Port) TableGenerator
                .createTable(dbSchema, OvsdbTable.PORT);

        port.setName(portName);
        if (portUuid == null) {
            insertConfig(OvsdbConstant.PORT, "_uuid", OvsdbConstant.BRIDGE,
                         "ports", bridgeUuid, port.getRow());
        } else {
            updateConfig(OvsdbConstant.PORT, "_uuid", portUuid, port.getRow());
        }

        return;
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
            deleteConfig(OvsdbConstant.PORT, "_uuid", portUuid,
                         OvsdbConstant.BRIDGE, "ports");
        }
    }

    @Override
    public void createBridge(String bridgeName) {
        log.debug("create bridge {}", bridgeName);

        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);
        if (dbSchema == null) {
            log.warn("The schema is null");
            return;
        }

        Bridge bridge = (Bridge) TableGenerator.createTable(dbSchema,
                                                            OvsdbTable.BRIDGE);
        if (bridge == null) {
            log.debug("Can not create bridge");
            return;
        }

        Set<String> failModes = new HashSet<>();
        failModes.add("secure");
        bridge.setFailMode(failModes);

        Set<String> protocols = new HashSet<>();
        protocols.add(OvsdbConstant.OPENFLOW13);
        bridge.setProtocols(protocols);

        String ovsUuid = getOvsUuid(OvsdbConstant.DATABASENAME);
        if (ovsUuid == null) {
            log.warn("The Open_vSwitch is null");
            return;
        }

        Map<String, String> options = new HashMap<>();
        options.put("disable-in-band", "true");
        bridge.setOtherConfig(options);

        String bridgeUuid = getBridgeUuid(bridgeName);
        if (bridgeUuid == null) {
            log.debug("Create a new bridge");

            bridge.setName(bridgeName);
            bridgeUuid = insertConfig(OvsdbConstant.BRIDGE, "_uuid",
                                      OvsdbConstant.DATABASENAME, "bridges",
                                      ovsUuid, bridge.getRow());

            if (bridgeUuid != null) {
                Port port = (Port) TableGenerator.createTable(dbSchema,
                                                              OvsdbTable.PORT);
                if (port != null) {
                    log.debug("the port is not null");
                    port.setName(bridgeName);

                    insertConfig(OvsdbConstant.PORT, "_uuid", "Bridge", "ports", bridgeUuid,
                                 port.getRow());
                }
            } else {
                String message = BridgeCreateException.createMessage(ovsUuid);
                throw new BridgeCreateException(message);
            }

        } else {
            log.info("Update a bridge");
            updateConfig(OvsdbConstant.BRIDGE, "_uuid", bridgeUuid, bridge.getRow());
        }

        setControllerAuto(bridgeUuid);
        log.info("Create bridge success");
    }

    @Override
    public void createBridge(String bridgeName, String dpid, String exPortName) {
        log.debug("create bridge {}", bridgeName);

        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);
        if (dbSchema == null) {
            log.warn("The schema is null");
            return;
        }

        Bridge bridge = (Bridge) TableGenerator.createTable(dbSchema,
                                                            OvsdbTable.BRIDGE);
        if (bridge == null) {
            log.debug("Can not create bridge");
            return;
        }

        Set<String> failModes = new HashSet<>();
        failModes.add("secure");
        bridge.setFailMode(failModes);

        Set<String> protocols = new HashSet<>();
        protocols.add(OvsdbConstant.OPENFLOW13);
        bridge.setProtocols(protocols);

        String ovsUuid = getOvsUuid(OvsdbConstant.DATABASENAME);
        if (ovsUuid == null) {
            log.warn("The Open_vSwitch is null");
            return;
        }

        Map<String, String> options = new HashMap<>();
        options.put("disable-in-band", "true");
        if (dpid != null) {
            options.put("datapath-id", dpid);
        }
        bridge.setOtherConfig(options);

        String bridgeUuid = getBridgeUuid(bridgeName);
        if (bridgeUuid == null) {
            log.debug("Create a new bridge");

            bridge.setName(bridgeName);
            bridgeUuid = insertConfig(OvsdbConstant.BRIDGE, "_uuid",
                                      OvsdbConstant.DATABASENAME, "bridges",
                                      ovsUuid, bridge.getRow());

            if (bridgeUuid != null) {
                Port port = (Port) TableGenerator.createTable(dbSchema,
                                                              OvsdbTable.PORT);
                if (port != null) {
                    log.debug("the port is not null");
                    port.setName(bridgeName);

                    insertConfig(OvsdbConstant.PORT, "_uuid", "Bridge", "ports", bridgeUuid,
                                 port.getRow());
                }
            } else {
                String message = BridgeCreateException.createMessage(ovsUuid);
                throw new BridgeCreateException(message);
            }

        } else {
            log.info("Update a bridge");
            updateConfig(OvsdbConstant.BRIDGE, "_uuid", bridgeUuid, bridge.getRow());
        }
        // Create external port
        if (exPortName != null) {
            createPort(bridgeName, exPortName);
        }

        setControllerAuto(bridgeUuid);
        log.info("Create bridge success");
    }

    @Override
    public boolean createBridge(String bridgeName, String dpid, List<ControllerInfo> controllers) {

        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);
        String ovsUuid = getOvsUuid(OvsdbConstant.DATABASENAME);

        if (dbSchema == null || ovsUuid == null) {
            log.warn("Couldn't find database Open_vSwitch");
            return false;
        }

        Bridge bridge = (Bridge) TableGenerator.createTable(dbSchema, OvsdbTable.BRIDGE);
        Set<String> failMode = new HashSet<>(Arrays.asList("secure"));
        bridge.setFailMode(failMode);

        Set<String> protocols = new HashSet<>(Arrays.asList(OvsdbConstant.OPENFLOW13));
        bridge.setProtocols(protocols);

        Map<String, String> options = new HashMap<>();
        options.put("disable-in-band", "true");
        if (dpid != null) {
            options.put("datapath-id", dpid);
        }
        bridge.setOtherConfig(options);

        String bridgeUuid = getBridgeUuid(bridgeName);
        if (bridgeUuid == null) {
            bridge.setName(bridgeName);
            bridgeUuid = insertConfig(OvsdbConstant.BRIDGE, "_uuid",
                                      OvsdbConstant.DATABASENAME, "bridges",
                                      ovsUuid, bridge.getRow());
        } else {
            updateConfig(OvsdbConstant.BRIDGE, "_uuid", bridgeUuid, bridge.getRow());
        }

        if (bridgeUuid != null) {
            createPort(bridgeName, bridgeName);
        } else {
            log.warn("Failed to create bridge {} on {}", bridgeName, nodeId.toString());
            return false;
        }

        setControllersWithUuid(Uuid.uuid(bridgeUuid), controllers);
        return true;
    }

    /**
     * Sets the bridge's controller automatically.
     * <p/>
     * The connection is a TCP connection to the local ONOS instance's IP
     * and the default OpenFlow port.
     *
     * @param bridgeUuid bridge uuid
     */
    private void setControllerAuto(String bridgeUuid) {
        IpAddress ipAddress = IpAddress.valueOf(((InetSocketAddress) channel.localAddress()).getAddress());
        ControllerInfo controllerInfo = new ControllerInfo(ipAddress, OvsdbConstant.OFPORT, "tcp");
        log.debug("Automatically setting controller for bridge {} to {}",
                  bridgeUuid, controllerInfo.target());
        setControllersWithUuid(Uuid.uuid(bridgeUuid), ImmutableList.of(controllerInfo));
    }

    @Override
    public void setControllersWithUuid(Uuid bridgeUuid, List<ControllerInfo> controllers) {

        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);
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
        OvsdbRowStore controllerRowStore = getRowStore(OvsdbConstant.DATABASENAME,
                                                       OvsdbConstant.CONTROLLER);
        if (controllerRowStore == null) {
            log.debug("There is no controller table");
            return;
        }

        removeControllers.forEach(c -> deleteConfig(OvsdbConstant.CONTROLLER, "_uuid", c.getRow().uuid().value(),
                                                    OvsdbConstant.BRIDGE, "controller"));

        newControllers.stream().map(c -> {
            Controller controller = (Controller) TableGenerator
                    .createTable(dbSchema, OvsdbTable.CONTROLLER);
            controller.setTarget(c.target());
            return controller;
        }).forEach(c -> {
            String uuid = insertConfig(OvsdbConstant.CONTROLLER, "_uuid",
                                       OvsdbConstant.BRIDGE, "controller", bridgeUuid.value(),
                                       c.getRow());
            newControllerUuids.add(Uuid.uuid(uuid));

        });

        OvsdbRowStore rowStore = getRowStore(OvsdbConstant.DATABASENAME,
                                             OvsdbConstant.BRIDGE);
        if (rowStore == null) {
            log.debug("There is no bridge table");
            return;
        }

        Row bridgeRow = rowStore.getRow(bridgeUuid.value());
        Bridge bridge = (Bridge) TableGenerator.getTable(dbSchema, bridgeRow, OvsdbTable.BRIDGE);
        bridge.setController(OvsdbSet.ovsdbSet(newControllerUuids));
        updateConfig(OvsdbConstant.BRIDGE, "_uuid", bridgeUuid.value(), bridge.getRow());
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
        deleteConfig(OvsdbConstant.BRIDGE, "_uuid", bridgeUuid,
                     OvsdbConstant.DATABASENAME, "bridges");
    }

    @Override
    public boolean createTunnel(String bridgeName, String portName, String tunnelType, Map<String, String> options) {

        String bridgeUuid  = getBridgeUuid(bridgeName);
        if (bridgeUuid == null) {
            log.warn("Couldn't find bridge {} in {}", bridgeName, nodeId.getIpAddress());
            return false;
        }

        if (getPortUuid(portName, bridgeUuid) != null) {
            log.warn("Port {} already exists", portName);
            // remove existing one and re-create?
            return false;
        }

        ArrayList<Operation> operations = Lists.newArrayList();
        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);

        // insert a new port to the port table
        Port port = (Port) TableGenerator.createTable(dbSchema, OvsdbTable.PORT);
        port.setName(portName);
        Insert portInsert = new Insert(dbSchema.getTableSchema("Port"), "Port", port.getRow());
        portInsert.getRow().put("interfaces", Uuid.uuid("Interface"));
        operations.add(portInsert);

        // update the bridge table
        Condition condition = ConditionUtil.isEqual("_uuid", Uuid.uuid(bridgeUuid));
        Mutation mutation = MutationUtil.insert("ports", Uuid.uuid("Port"));
        List<Condition> conditions = new ArrayList<>(Arrays.asList(condition));
        List<Mutation> mutations = new ArrayList<>(Arrays.asList(mutation));
        operations.add(new Mutate(dbSchema.getTableSchema("Bridge"), conditions, mutations));

        // insert a tunnel interface
        Interface intf = (Interface) TableGenerator.createTable(dbSchema, OvsdbTable.INTERFACE);
        intf.setName(portName);
        intf.setType(tunnelType);
        intf.setOptions(options);
        Insert intfInsert = new Insert(dbSchema.getTableSchema("Interface"), "Interface", intf.getRow());
        operations.add(intfInsert);

        transactConfig(OvsdbConstant.DATABASENAME, operations);
        return true;
    }

    @Override
    public void dropTunnel(IpAddress srcIp, IpAddress dstIp) {
        String bridgeName = OvsdbConstant.INTEGRATION_BRIDGE;
        String portName = getTunnelName(OvsdbConstant.TYPEVXLAN, dstIp);
        String bridgeUuid = getBridgeUuid(OvsdbConstant.INTEGRATION_BRIDGE);
        if (bridgeUuid == null) {
            log.warn("Could not find bridge {} in {}", bridgeName,
                     nodeId.getIpAddress());
            return;
        }

        String portUuid = getPortUuid(portName, bridgeUuid);
        if (portUuid != null) {
            log.info("Delete tunnel");
            deleteConfig(OvsdbConstant.PORT, "_uuid", portUuid,
                         OvsdbConstant.BRIDGE, "ports");
        }

        return;
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
        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);
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
        transactConfig(OvsdbConstant.DATABASENAME, operations);

        return;
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
        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);
        TableSchema tableSchema = dbSchema.getTableSchema(tableName);

        List<Condition> conditions = Lists.newArrayList();
        Condition condition = ConditionUtil.isEqual(columnName, Uuid.uuid(uuid));
        conditions.add(condition);

        Update update = new Update(tableSchema, row, conditions);

        ArrayList<Operation> operations = Lists.newArrayList();
        operations.add(update);

        transactConfig(OvsdbConstant.DATABASENAME, operations);
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
        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);
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
            Condition condition = ConditionUtil.isEqual("_uuid",
                                                       Uuid.uuid(parentUuid));
            conditions.add(condition);

            Mutate op = new Mutate(parentTableSchema, conditions, mutations);
            operations.add(op);
        }
        if (childTableName.equalsIgnoreCase(OvsdbConstant.PORT)) {
            log.info("Handle port insert");
            Insert intfInsert = handlePortInsertTable(OvsdbConstant.INTERFACE,
                                                      row);

            if (intfInsert != null) {
                operations.add(intfInsert);
            }

            Insert ins = (Insert) operations.get(0);
            ins.getRow().put("interfaces",
                             Uuid.uuid(OvsdbConstant.INTERFACE));
        }

        List<OperationResult> results;
        try {
            results = transactConfig(OvsdbConstant.DATABASENAME, operations)
                    .get();

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
     * @param tableName ovsdb table interface
     * @param portRow   row of port
     * @return insert, empty if null
     */
    private Insert handlePortInsertTable(String tableName, Row portRow) {
        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);

        TableSchema portTableSchema = dbSchema
                .getTableSchema(OvsdbConstant.PORT);
        ColumnSchema portColumnSchema = portTableSchema.getColumnSchema("name");

        String portName = (String) portRow.getColumn(portColumnSchema.name()).data();

        Interface inf = (Interface) TableGenerator
                .createTable(dbSchema, OvsdbTable.INTERFACE);

        inf.setName(portName);

        TableSchema intfTableSchema = dbSchema
                .getTableSchema(OvsdbConstant.INTERFACE);
        Insert insert = new Insert(intfTableSchema, OvsdbConstant.INTERFACE,
                                   inf.getRow());
        return insert;
    }

    /**
     * Gets tunnel name.
     *
     * @param tunnelType
     * @param dstIp      the remote ip address
     * @return tunnel name
     */
    private String getTunnelName(String tunnelType, IpAddress dstIp) {
        return tunnelType + "-" + dstIp.toString();
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
            Function<JsonNode, DatabaseSchema> rowFunction = new Function<JsonNode, DatabaseSchema>() {
                @Override
                public DatabaseSchema apply(JsonNode input) {
                    log.info("Get ovsdb database schema {}", dbName);
                    DatabaseSchema dbSchema = FromJsonUtil
                            .jsonNodeToDbSchema(dbName, input);
                    if (dbSchema == null) {
                        log.debug("Get ovsdb database schema error");
                        return null;
                    }
                    schema.put(dbName, dbSchema);

                    return dbSchema;
                }
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
            Function<JsonNode, TableUpdates> rowFunction = new Function<JsonNode, TableUpdates>() {
                @Override
                public TableUpdates apply(JsonNode input) {
                    log.info("Get table updates");
                    TableUpdates updates = FromJsonUtil
                            .jsonNodeToTableUpdates(input, dbSchema);
                    if (updates == null) {
                        log.debug("Get table updates error");
                        return null;
                    }
                    return updates;
                }
            };
            return Futures.transform(monitor(dbSchema, id), rowFunction);
        }
        return null;
    }

    @Override
    public ListenableFuture<List<OperationResult>> transactConfig(String dbName,
                                                                  List<Operation> operations) {
        if (dbName == null) {
            return null;
        }
        DatabaseSchema dbSchema = schema.get(dbName);
        if (dbSchema != null) {
            Function<List<JsonNode>, List<OperationResult>> rowFunction = (input -> {
                log.info("Get ovsdb operation result");
                List<OperationResult> result = FromJsonUtil
                        .jsonNodeToOperationResult(input, operations);

                if (result == null) {
                    log.debug("The operation result is null");
                    return null;
                }
                return result;
            });
            return Futures.transform(transact(dbSchema, operations),
                                     rowFunction);
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

        Object result;
        result = FromJsonUtil.jsonResultParser(response, methodName);

        sf.set(result);
        return;
    }

    @Override
    public void processRequest(JsonNode requestJson) {
        log.debug("Handle request");
        if (requestJson.get("method").asText().equalsIgnoreCase("echo")) {
            log.debug("handle echo request");

            String replyString = FromJsonUtil.getEchoRequestStr(requestJson);
            channel.writeAndFlush(replyString);

            return;
        } else {
            FromJsonUtil
                    .jsonCallbackRequestParser(requestJson, monitorCallBack);
            return;
        }
    }

    @Override
    public void setCallback(Callback monitorCallback) {
        this.monitorCallBack = monitorCallback;
    }

    @Override
    public Set<OvsdbTunnel> getTunnels() {
        return ovsdbTunnels;
    }

    @Override
    public Set<OvsdbBridge> getBridges() {
        Set<OvsdbBridge> ovsdbBridges = new HashSet<OvsdbBridge>();
        OvsdbTableStore tableStore = getTableStore(OvsdbConstant.DATABASENAME);
        if (tableStore == null) {
            return null;
        }
        OvsdbRowStore rowStore = tableStore.getRows(OvsdbConstant.BRIDGE);
        if (rowStore == null) {
            return null;
        }
        ConcurrentMap<String, Row> rows = rowStore.getRowStore();
        for (String uuid : rows.keySet()) {
            Row row = getRow(OvsdbConstant.DATABASENAME, OvsdbConstant.BRIDGE,
                             uuid);
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
        return controllers.stream().
                map(controller -> new ControllerInfo(
                        (String) controller.getTargetColumn()
                                .data())).collect(Collectors.toSet());
    }

    private List<Controller> getControllers(Uuid bridgeUuid) {
        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);
        if (dbSchema == null) {
            return null;
        }
        OvsdbRowStore rowStore = getRowStore(OvsdbConstant.DATABASENAME,
                                             OvsdbConstant.BRIDGE);
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
//        Set<String> controllerUuidStrings = (Set<String>) bridge.getControllerColumn().data();

        OvsdbRowStore controllerRowStore = getRowStore(OvsdbConstant.DATABASENAME,
                                                       OvsdbConstant.CONTROLLER);
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
        DatabaseSchema dbSchema = schema.get(OvsdbConstant.DATABASENAME);
        if (dbSchema == null) {
            return null;
        }
        OvsdbRowStore rowStore = getRowStore(OvsdbConstant.DATABASENAME,
                                             OvsdbConstant.BRIDGE);
        if (rowStore == null) {
            log.debug("There is no bridge table");
            return null;
        }

        ConcurrentMap<String, Row> bridgeTableRows = rowStore.getRowStore();
        final AtomicReference<Uuid> uuid = new AtomicReference<>();
        for (Map.Entry<String, Row> entry : bridgeTableRows.entrySet()) {
            Bridge b = (Bridge) TableGenerator.getTable(dbSchema,
                                                        entry.getValue(),
                                                        OvsdbTable.BRIDGE);
            if (matchesDpid(b, openflowDeviceId)) {
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
        Set<OvsdbPort> ovsdbPorts = new HashSet<OvsdbPort>();
        OvsdbTableStore tableStore = getTableStore(OvsdbConstant.DATABASENAME);
        if (tableStore == null) {
            return null;
        }
        OvsdbRowStore rowStore = tableStore.getRows(OvsdbConstant.INTERFACE);
        if (rowStore == null) {
            return null;
        }
        ConcurrentMap<String, Row> rows = rowStore.getRowStore();
        for (String uuid : rows.keySet()) {
            Row row = getRow(OvsdbConstant.DATABASENAME,
                             OvsdbConstant.INTERFACE, uuid);
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

    //Gets ovsdb port.
    private OvsdbPort getOvsdbPort(Row row) {
        DatabaseSchema dbSchema = getDatabaseSchema(OvsdbConstant.DATABASENAME);
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

        OvsdbPort ovsdbPort = new OvsdbPort(new OvsdbPortNumber(ofPort),
                                            new OvsdbPortName(portName));
        return ovsdbPort;
    }

    ////Gets ovsdb bridge.
    private OvsdbBridge getOvsdbBridge(Row row) {
        DatabaseSchema dbSchema = getDatabaseSchema(OvsdbConstant.DATABASENAME);
        Bridge bridge = (Bridge) TableGenerator.getTable(dbSchema, row,
                                                         OvsdbTable.BRIDGE);
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

        OvsdbBridge ovsdbBridge = new OvsdbBridge(new OvsdbBridgeName(bridgeName),
                                                  new OvsdbDatapathId(datapathId));
        return ovsdbBridge;
    }

    //Gets ofPort in the interface.
    private long getOfPort(Interface intf) {
        OvsdbSet ofPortSet = (OvsdbSet) intf.getOpenFlowPortColumn().data();
        @SuppressWarnings("unchecked")
        Set<Integer> ofPorts = ofPortSet.set();
        while (ofPorts == null || ofPorts.size() <= 0) {
            log.debug("The ofport is null in {}", intf.getName());
            return -1;
        }
        // return (long) ofPorts.toArray()[0];
        Iterator<Integer> it = ofPorts.iterator();
        return Long.parseLong(it.next().toString());
    }

    @Override
    public Set<OvsdbPort> getLocalPorts(Iterable<String> ifaceids) {
        Set<OvsdbPort> ovsdbPorts = new HashSet<OvsdbPort>();
        OvsdbTableStore tableStore = getTableStore(OvsdbConstant.DATABASENAME);
        if (tableStore == null) {
            return null;
        }
        OvsdbRowStore rowStore = tableStore.getRows(OvsdbConstant.INTERFACE);
        if (rowStore == null) {
            return null;
        }
        ConcurrentMap<String, Row> rows = rowStore.getRowStore();
        for (String uuid : rows.keySet()) {
            Row row = getRow(OvsdbConstant.DATABASENAME,
                             OvsdbConstant.INTERFACE, uuid);
            DatabaseSchema dbSchema = getDatabaseSchema(OvsdbConstant.DATABASENAME);
            Interface intf = (Interface) TableGenerator
                    .getTable(dbSchema, row, OvsdbTable.INTERFACE);
            if (intf == null || getIfaceid(intf) == null) {
                continue;
            }
            String portName = intf.getName();
            Set<String> ifaceidSet = Sets.newHashSet(ifaceids);
            if (portName.startsWith("vxlan")
                    || !ifaceidSet.contains(getIfaceid(intf))) {
                continue;
            }
            long ofPort = getOfPort(intf);
            if ((ofPort < 0) || (portName == null)) {
                continue;
            }

            OvsdbPort ovsdbPort = new OvsdbPort(new OvsdbPortNumber(ofPort),
                                                new OvsdbPortName(portName));
            if (ovsdbPort != null) {
                ovsdbPorts.add(ovsdbPort);
            }
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
        String ifaceid = externalIds
                .get(OvsdbConstant.EXTERNAL_ID_INTERFACE_ID);
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
