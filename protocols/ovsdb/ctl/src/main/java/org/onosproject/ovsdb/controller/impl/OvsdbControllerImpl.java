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
package org.onosproject.ovsdb.controller.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.ovsdb.controller.DefaultEventSubject;
import org.onosproject.ovsdb.controller.EventSubject;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbConstant;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbDatapathId;
import org.onosproject.ovsdb.controller.OvsdbEvent;
import org.onosproject.ovsdb.controller.OvsdbEvent.Type;
import org.onosproject.ovsdb.controller.OvsdbEventListener;
import org.onosproject.ovsdb.controller.OvsdbIfaceId;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbNodeListener;
import org.onosproject.ovsdb.controller.OvsdbPortName;
import org.onosproject.ovsdb.controller.OvsdbPortNumber;
import org.onosproject.ovsdb.controller.OvsdbPortType;
import org.onosproject.ovsdb.controller.driver.OvsdbAgent;
import org.onosproject.ovsdb.controller.impl.TlsParams.TlsMode;
import org.onosproject.ovsdb.rfc.jsonrpc.Callback;
import org.onosproject.ovsdb.rfc.message.TableUpdate;
import org.onosproject.ovsdb.rfc.message.TableUpdates;
import org.onosproject.ovsdb.rfc.message.UpdateNotification;
import org.onosproject.ovsdb.rfc.notation.OvsdbMap;
import org.onosproject.ovsdb.rfc.notation.OvsdbSet;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.Uuid;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.table.Bridge;
import org.onosproject.ovsdb.rfc.table.Interface;
import org.onosproject.ovsdb.rfc.table.OvsdbTable;
import org.onosproject.ovsdb.rfc.table.TableGenerator;
import org.onosproject.ovsdb.rfc.utils.FromJsonUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.get;
import static org.onosproject.ovsdb.controller.impl.Controller.MIN_KS_LENGTH;
import static org.onosproject.ovsdb.controller.impl.OsgiPropertyConstants.*;

/**
 * The implementation of OvsdbController.
 */
@Component(immediate = true, service = OvsdbController.class,
        property = {
                "serverMode" + ":Boolean=" + SERVER_MODE_DEFAULT,
                "enableOvsdbTls" + ":Boolean=" + OVSDB_TLS_FLAG_DEFAULT,
                "keyStoreLocation" + "=" + KS_FILE_DEFAULT,
                "keyStorePassword" + "=" + KS_PASSWORD_DEFAULT,
                "trustStoreLocation" + "=" + TS_FILE_DEFAULT,
                "trustStorePassword" + "=" + TS_PASSWORD_DEFAULT,
        })
public class OvsdbControllerImpl implements OvsdbController {

    public static final Logger log = LoggerFactory
            .getLogger(OvsdbControllerImpl.class);
    private static final long DEFAULT_OVSDB_RPC_TIMEOUT = 3000;
    private final Controller controller = new Controller();
    protected ConcurrentHashMap<OvsdbNodeId, OvsdbClientService> ovsdbClients =
            new ConcurrentHashMap<>();
    protected OvsdbAgent agent = new InternalOvsdbNodeAgent();
    protected InternalMonitorCallBack updateCallback = new InternalMonitorCallBack();
    protected Set<OvsdbNodeListener> ovsdbNodeListener = new CopyOnWriteArraySet<>();
    protected Set<OvsdbEventListener> ovsdbEventListener = new CopyOnWriteArraySet<>();
    protected ConcurrentHashMap<String, OvsdbClientService> requestNotification =
            new ConcurrentHashMap<>();
    protected ConcurrentHashMap<String, String> requestDbName = new ConcurrentHashMap<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    /** Run as server mode, listen on 6640 port. */
    private boolean serverMode = SERVER_MODE_DEFAULT;

    /** TLS mode for OVSDB channel; options are: true false. */
    private boolean enableOvsdbTls = OVSDB_TLS_FLAG_DEFAULT;

    /** File path to KeyStore for Ovsdb TLS Connections. */
    protected String keyStoreLocation = KS_FILE_DEFAULT;

    /** File path to TrustStore for Ovsdb TLS Connections. */
    protected String trustStoreLocation = TS_FILE_DEFAULT;

    /** KeyStore Password. */
    protected String keyStorePassword = KS_PASSWORD_DEFAULT;

    /** TrustStore Password. */
    protected String trustStorePassword = TS_PASSWORD_DEFAULT;

    @Activate
    public void activate(ComponentContext context) {
        configService.registerProperties(getClass());
        modified(context);
        controller.start(agent, updateCallback, serverMode);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        controller.stop();

        configService.unregisterProperties(getClass(), false);

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        this.setConfigParams(context.getProperties());
    }

    /**
     * Sets config params.
     *
     * @param properties dictionary
     */
    public void setConfigParams(Dictionary<?, ?> properties) {
        boolean restartRequired = setServerMode(properties);
        TlsParams tlsParams = getTlsParams(properties);
        restartRequired |= controller.setTlsParameters(tlsParams);
        if (restartRequired) {
            restartController();
        }
    }

    /**
     * Gets the TLS parameters from the properties dict.
     *
     * @param properties dictionary
     * @return TlsParams Modified Tls Params
     */
    private TlsParams getTlsParams(Dictionary<?, ?> properties) {
        TlsMode mode = null;

        boolean flag = Tools.isPropertyEnabled(properties, OVSDB_TLS_FLAG);
        if (Objects.isNull(flag) || !flag) {
            log.warn("OvsdbTLS Disabled");
            mode = TlsMode.DISABLED;
        } else {
            log.warn("OvsdbTLS Enabled");
            mode = TlsMode.ENABLED;
        }

        String ksLocation = null, tsLocation = null, ksPwd = null, tsPwd = null;

        ksLocation = get(properties, KS_FILE);
        if (Strings.isNullOrEmpty(ksLocation)) {
            log.warn("trustStoreLocation is not configured");
            mode = TlsMode.DISABLED;
        }

        tsLocation = get(properties, TS_FILE);
        if (Strings.isNullOrEmpty(tsLocation)) {
            log.warn("trustStoreLocation is not configured");
            mode = TlsMode.DISABLED;
        }

        ksPwd = get(properties, KS_PASSWORD);
        if (Strings.isNullOrEmpty(ksPwd) || MIN_KS_LENGTH > ksPwd.length()) {
            log.warn("keyStorePassword is not configured or Password length too small");
            mode = TlsMode.DISABLED;
        }

        tsPwd = get(properties, TS_PASSWORD);
        if (Strings.isNullOrEmpty(tsPwd) || MIN_KS_LENGTH > tsPwd.length()) {
            log.warn("trustStorePassword is not configured or Password length too small");
            mode = TlsMode.DISABLED;
        }

        TlsParams tlsParams = new TlsParams(mode, ksLocation, tsLocation, ksPwd, tsPwd);
        log.info("OVSDB TLS Params: {}", tlsParams);
        return tlsParams;
    }

    private boolean setServerMode(Dictionary<?, ?> properties) {
        boolean flag = Tools.isPropertyEnabled(properties, "serverMode");
        if (Objects.isNull(flag) || flag == serverMode) {
            log.info("Ovsdb server mode is not configured, " +
                             "or modified. Using current value of {}", serverMode);
            return false;
        } else {
            serverMode = flag;
            log.info("Configured. OVSDB server mode was {}",
                     serverMode ? "enabled" : "disabled");
            return true;
        }

    }

    @Override
    public void addNodeListener(OvsdbNodeListener listener) {
        if (!ovsdbNodeListener.contains(listener)) {
            this.ovsdbNodeListener.add(listener);
        }
    }

    @Override
    public void removeNodeListener(OvsdbNodeListener listener) {
        this.ovsdbNodeListener.remove(listener);
    }

    @Override
    public void addOvsdbEventListener(OvsdbEventListener listener) {
        if (!ovsdbEventListener.contains(listener)) {
            this.ovsdbEventListener.add(listener);
        }
    }

    @Override
    public void removeOvsdbEventListener(OvsdbEventListener listener) {
        this.ovsdbEventListener.remove(listener);
    }

    @Override
    public List<OvsdbNodeId> getNodeIds() {
        return ImmutableList.copyOf(ovsdbClients.keySet());
    }

    @Override
    public OvsdbClientService getOvsdbClient(OvsdbNodeId nodeId) {
        return ovsdbClients.get(nodeId);
    }

    @Override
    public void connect(IpAddress ip, TpPort port) {
        controller.connect(ip, port);
    }

    @Override
    public void connect(IpAddress ip, TpPort port, Consumer<Exception> failhandler) {
        controller.connect(ip, port, failhandler);
    }

    @Override
    public void setServerMode(boolean serverMode) {
        this.serverMode = serverMode;
        restartController();
    }

    /**
     * Processes table updates.
     *
     * @param clientService OvsdbClientService instance
     * @param updates       TableUpdates instance
     * @param dbName        ovsdb database name
     */
    private void processTableUpdates(OvsdbClientService clientService,
                                     TableUpdates updates, String dbName)
            throws InterruptedException {
        checkNotNull(clientService, "OvsdbClientService is not null");

        DatabaseSchema dbSchema = clientService.getDatabaseSchema(dbName);

        for (String tableName : updates.result().keySet()) {
            TableUpdate update = updates.result().get(tableName);
            for (Uuid uuid : (Set<Uuid>) update.rows().keySet()) {
                log.debug("Begin to process table updates uuid: {}, databaseName: {}, tableName: {}",
                          uuid.value(), dbName, tableName);

                Row newRow = update.getNew(uuid);
                if (newRow != null) {
                    clientService.updateOvsdbStore(dbName, tableName,
                                                   uuid.value(), newRow);

                    if (OvsdbConstant.INTERFACE.equals(tableName)) {
                        dispatchInterfaceEvent(clientService,
                                               newRow,
                                               OvsdbEvent.Type.PORT_ADDED,
                                               dbSchema);
                    }
                } else if (update.getOld(uuid) != null) {
                    if (OvsdbConstant.INTERFACE.equals(tableName)) {
                        Row row = clientService.getRow(OvsdbConstant.DATABASENAME, tableName, uuid.value());
                        dispatchInterfaceEvent(clientService,
                                               row,
                                               OvsdbEvent.Type.PORT_REMOVED,
                                               dbSchema);
                    }
                    clientService.removeRow(dbName, tableName, uuid.value());
                }
            }
        }
    }

    /**
     * Dispatches event to the north.
     *
     * @param clientService OvsdbClientService instance
     * @param row           a new row
     * @param eventType     type of event
     * @param dbSchema      ovsdb database schema
     */
    private void dispatchInterfaceEvent(OvsdbClientService clientService,
                                        Row row,
                                        Type eventType,
                                        DatabaseSchema dbSchema) {

        long dpid = getDataPathid(clientService, dbSchema);
        Interface intf = (Interface) TableGenerator
                .getTable(dbSchema, row, OvsdbTable.INTERFACE);
        if (intf == null) {
            return;
        }

        String portType = (String) intf.getTypeColumn().data();
        long localPort = getOfPort(intf);
        if (localPort < 0) {
            return;
        }
        String[] macAndIfaceId = getMacAndIfaceid(intf);
        if (macAndIfaceId == null) {
            return;
        }

        EventSubject eventSubject = new DefaultEventSubject(MacAddress.valueOf(
                macAndIfaceId[0]),
                new HashSet<>(),
                                                            new OvsdbPortName(intf
                                                                                      .getName()),
                                                            new OvsdbPortNumber(localPort),
                                                            new OvsdbDatapathId(Long
                                                                                        .toString(dpid)),
                                                            new OvsdbPortType(portType),
                                                            new OvsdbIfaceId(macAndIfaceId[1]));
        for (OvsdbEventListener listener : ovsdbEventListener) {
            listener.handle(new OvsdbEvent<>(eventType,
                    eventSubject));
        }
    }

    /**
     * Gets mac and iface from the table Interface.
     *
     * @param intf Interface instance
     * @return attachedMac, ifaceid
     */
    private String[] getMacAndIfaceid(Interface intf) {
        OvsdbMap ovsdbMap = (OvsdbMap) intf.getExternalIdsColumn().data();
        @SuppressWarnings("unchecked")
        Map<String, String> externalIds = ovsdbMap.map();
        if (externalIds == null) {
            log.warn("The external_ids is null");
            return null;
        }

        String attachedMac = externalIds.get(OvsdbConstant.EXTERNAL_ID_VM_MAC);
        if (attachedMac == null) {
            log.debug("The attachedMac is null"); //FIXME why always null?
            return null;
        }
        String ifaceid = externalIds
                .get(OvsdbConstant.EXTERNAL_ID_INTERFACE_ID);
        if (ifaceid == null) {
            log.warn("The ifaceid is null");
            return null;
        }
        return new String[]{attachedMac, ifaceid};
    }

    /**
     * Gets ofPorts number from table Interface.
     *
     * @param intf Interface instance
     * @return ofport the ofport number
     */
    private long getOfPort(Interface intf) {
        OvsdbSet ofPortSet = (OvsdbSet) intf.getOpenFlowPortColumn().data();
        @SuppressWarnings("unchecked")
        Set<Integer> ofPorts = ofPortSet.set();
        if (ofPorts == null || ofPorts.isEmpty()) {
            log.debug("The ofport is null in {}", intf.getName());
            return -1;
        }
        Iterator<Integer> it = ofPorts.iterator();
        return Long.parseLong(it.next().toString());
    }

    /**
     * Gets datapathid from table bridge.
     *
     * @param clientService OvsdbClientService instance
     * @param dbSchema      ovsdb database schema
     * @return datapathid the bridge datapathid
     */
    private long getDataPathid(OvsdbClientService clientService,
                               DatabaseSchema dbSchema) {
        String bridgeUuid = clientService
                .getBridgeUuid(OvsdbConstant.INTEGRATION_BRIDGE);
        if (bridgeUuid == null) {
            log.debug("Unable to spot bridge uuid for {} in {}",
                      OvsdbConstant.INTEGRATION_BRIDGE, clientService);
            return 0;
        }

        Row bridgeRow = clientService.getRow(OvsdbConstant.DATABASENAME,
                                             "Bridge", bridgeUuid);
        Bridge bridge = (Bridge) TableGenerator.getTable(dbSchema, bridgeRow,
                                                         OvsdbTable.BRIDGE);
        OvsdbSet dpidSet = (OvsdbSet) bridge.getDatapathIdColumn().data();
        @SuppressWarnings("unchecked")
        Set<String> dpids = dpidSet.set();
        if (dpids == null || dpids.isEmpty()) {
            return 0;
        }
        return stringToLong((String) dpids.toArray()[0]);
    }

    private long stringToLong(String values) {
        long value = (new BigInteger(values.replaceAll(":", ""), 16))
                .longValue();
        return value;
    }

    private void restartController() {
        controller.stop();
        controller.start(agent, updateCallback, serverMode);
    }

    /**
     * Implementation of an Ovsdb Agent which is responsible for keeping track
     * of connected node and the state in which they are.
     */
    private class InternalOvsdbNodeAgent implements OvsdbAgent {
        @Override
        public void addConnectedNode(OvsdbNodeId nodeId,
                                     OvsdbClientService ovsdbClient) {

            if (ovsdbClients.get(nodeId) != null) {
                ovsdbClient.disconnect();
                return;
            } else {

                try {
                    List<String> dbNames = ovsdbClient.listDbs().get(DEFAULT_OVSDB_RPC_TIMEOUT, TimeUnit.MILLISECONDS);
                    for (String dbName : dbNames) {
                        DatabaseSchema dbSchema;
                        dbSchema = ovsdbClient.getOvsdbSchema(dbName)
                                .get(DEFAULT_OVSDB_RPC_TIMEOUT, TimeUnit.MILLISECONDS);

                        log.debug("Begin to monitor tables");
                        String id = java.util.UUID.randomUUID().toString();
                        TableUpdates updates = ovsdbClient
                                .monitorTables(dbName, id).get(DEFAULT_OVSDB_RPC_TIMEOUT, TimeUnit.MILLISECONDS);

                        requestDbName.put(id, dbName);
                        requestNotification.put(id, ovsdbClient);

                        if (updates != null) {
                            processTableUpdates(ovsdbClient, updates,
                                                dbSchema.name());
                        }
                    }
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting to get message from ovsdb");
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {
                    log.error("Exception thrown while to get message from ovsdb");
                    ovsdbClient.disconnect();
                    return;
                } catch (TimeoutException e) {
                    log.error("TimeoutException thrown while to get message from ovsdb");
                    ovsdbClient.disconnect();
                    return;
                }
                ovsdbClients.put(nodeId, ovsdbClient);

                log.debug("Add node to north");
                for (OvsdbNodeListener l : ovsdbNodeListener) {
                    l.nodeAdded(nodeId);
                }
                return;
            }
        }

        @Override
        public void removeConnectedNode(OvsdbNodeId nodeId) {
            requestNotification.forEach((k, v) -> {
                if (v.nodeId().equals(nodeId)) {
                    requestNotification.remove(k);
                    requestDbName.remove(k);

                    ovsdbClients.remove(nodeId);
                    log.debug("Node connection is removed");
                    for (OvsdbNodeListener l : ovsdbNodeListener) {
                        l.nodeRemoved(nodeId);
                    }
                }
            });
        }
    }

    /**
     * Implementation of an Callback which is responsible for receiving request
     * infomation from ovsdb.
     */
    private class InternalMonitorCallBack implements Callback {
        @Override
        public void update(UpdateNotification updateNotification) {
            Object key = updateNotification.jsonValue();
            OvsdbClientService ovsdbClient = requestNotification.get(key);

            String dbName = requestDbName.get(key);
            JsonNode updatesJson = updateNotification.tbUpdatesJsonNode();
            DatabaseSchema dbSchema = ovsdbClient.getDatabaseSchema(dbName);
            TableUpdates updates = FromJsonUtil
                    .jsonNodeToTableUpdates(updatesJson, dbSchema);
            try {
                processTableUpdates(ovsdbClient, updates, dbName);
            } catch (InterruptedException e) {
                log.warn("Interrupted while processing table updates");
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void locked(List<String> ids) {
            // TODO Auto-generated method stub
        }

        @Override
        public void stolen(List<String> ids) {
            // TODO Auto-generated method stub
        }

    }

}
