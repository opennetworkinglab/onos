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
package org.onosproject.ovsdb.controller.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
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
import org.onosproject.ovsdb.rfc.jsonrpc.Callback;
import org.onosproject.ovsdb.rfc.message.TableUpdate;
import org.onosproject.ovsdb.rfc.message.TableUpdates;
import org.onosproject.ovsdb.rfc.message.UpdateNotification;
import org.onosproject.ovsdb.rfc.notation.OvsdbMap;
import org.onosproject.ovsdb.rfc.notation.OvsdbSet;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.UUID;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.table.Bridge;
import org.onosproject.ovsdb.rfc.table.Interface;
import org.onosproject.ovsdb.rfc.table.OvsdbTable;
import org.onosproject.ovsdb.rfc.table.Port;
import org.onosproject.ovsdb.rfc.table.TableGenerator;
import org.onosproject.ovsdb.rfc.utils.FromJsonUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The implementation of OvsdbController.
 */
@Component(immediate = true)
@Service
public class OvsdbControllerImpl implements OvsdbController {

    public static final Logger log = LoggerFactory
            .getLogger(OvsdbControllerImpl.class);

    protected ConcurrentHashMap<OvsdbNodeId, OvsdbClientService> ovsdbClients =
            new ConcurrentHashMap<OvsdbNodeId, OvsdbClientService>();

    protected OvsdbAgent agent = new InternalOvsdbNodeAgent();
    protected InternalMonitorCallBack updateCallback = new InternalMonitorCallBack();

    protected Set<OvsdbNodeListener> ovsdbNodeListener = new CopyOnWriteArraySet<>();
    protected Set<OvsdbEventListener> ovsdbEventListener = new CopyOnWriteArraySet<>();

    protected ConcurrentHashMap<String, OvsdbClientService> requestNotification =
            new ConcurrentHashMap<String, OvsdbClientService>();

    protected ConcurrentHashMap<String, String> requestDbName = new ConcurrentHashMap<String, String>();

    private final Controller controller = new Controller();

    @Activate
    public void activate(ComponentContext context) {
        controller.start(agent, updateCallback);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        controller.stop();
        log.info("Stoped");
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OvsdbClientService getOvsdbClient(OvsdbNodeId nodeId) {
        return ovsdbClients.get(nodeId);
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
                return;
            } else {
                ovsdbClients.put(nodeId, ovsdbClient);

                try {
                    List<String> dbNames = ovsdbClient.listDbs().get();
                    for (String dbName : dbNames) {
                        DatabaseSchema dbSchema;
                        dbSchema = ovsdbClient.getOvsdbSchema(dbName).get();

                        log.debug("Begin to monitor tables");
                        String id = java.util.UUID.randomUUID().toString();
                        TableUpdates updates = ovsdbClient
                                .monitorTables(dbName, id).get();

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
                } catch (ExecutionException e) {
                    log.error("Exception thrown while to get message from ovsdb");
                }

                log.debug("Add node to north");
                for (OvsdbNodeListener l : ovsdbNodeListener) {
                    l.nodeAdded(nodeId);
                }
                return;
            }
        }

        @Override
        public void removeConnectedNode(OvsdbNodeId nodeId) {
            ovsdbClients.remove(nodeId);
            log.debug("Node connection is removed");
            for (OvsdbNodeListener l : ovsdbNodeListener) {
                l.nodeRemoved(nodeId);
            }
        }
    }

    /**
     * Processes table updates.
     *
     * @param clientService OvsdbClientService instance
     * @param updates TableUpdates instance
     * @param dbName ovsdb database name
     */
    private void processTableUpdates(OvsdbClientService clientService,
                                     TableUpdates updates, String dbName)
            throws InterruptedException {
        checkNotNull(clientService, "OvsdbClientService is not null");

        DatabaseSchema dbSchema = clientService.getDatabaseSchema(dbName);

        for (String tableName : updates.result().keySet()) {
            TableUpdate update = updates.result().get(tableName);
            for (UUID uuid : (Set<UUID>) update.rows().keySet()) {
                log.debug("Begin to process table updates uuid: {}, databaseName: {}, tableName: {}",
                         uuid.value(), dbName, tableName);

                Row row = clientService.getRow(dbName, tableName, uuid.value());
                clientService.updateOvsdbStore(dbName, tableName, uuid.value(),
                                               update.getNew(uuid));
                if (update.getNew(uuid) != null) {
                    boolean isNewRow = (row == null) ? true : false;
                    if (isNewRow) {
                        if (OvsdbConstant.PORT.equals(tableName)) {
                            dispatchEvent(clientService, update.getNew(uuid),
                                          null, OvsdbEvent.Type.PORT_ADDED,
                                          dbSchema);
                        }
                    }
                } else if (update.getOld(uuid) != null) {
                    clientService.removeRow(dbName, tableName, uuid.toString());
                    if (update.getOld(uuid) != null) {
                        if (OvsdbConstant.PORT.equals(tableName)) {
                            dispatchEvent(clientService, null,
                                          update.getOld(uuid),
                                          OvsdbEvent.Type.PORT_REMOVED,
                                          dbSchema);
                        }
                    }
                }
            }
        }
    }

    /**
     * Dispatches event to the north.
     *
     * @param clientService OvsdbClientService instance
     * @param newRow a new row
     * @param oldRow an old row
     * @param eventType type of event
     * @param dbSchema ovsdb database schema
     */
    private void dispatchEvent(OvsdbClientService clientService, Row newRow,
                               Row oldRow, Type eventType,
                               DatabaseSchema dbSchema) {
        Port port = null;
        if (OvsdbEvent.Type.PORT_ADDED.equals(eventType)) {
            port = (Port) TableGenerator.getTable(dbSchema, newRow,
                                                  OvsdbTable.PORT);
        } else if (OvsdbEvent.Type.PORT_REMOVED.equals(eventType)) {
            port = (Port) TableGenerator.getTable(dbSchema, oldRow,
                                                  OvsdbTable.PORT);
        }
        if (port == null) {
            return;
        }

        long dpid = getDataPathid(clientService, dbSchema);
        OvsdbSet intfUuidSet = (OvsdbSet) port.getInterfacesColumn().data();
        @SuppressWarnings({ "unchecked" })
        Set<UUID> intfUuids = intfUuidSet.set();
        for (UUID intfUuid : intfUuids) {
            Row intfRow = clientService
                    .getRow(OvsdbConstant.DATABASENAME, "Interface",
                            intfUuid.toString());
            if (intfRow == null) {
                continue;
            }
            Interface intf = (Interface) TableGenerator
                    .getTable(dbSchema, intfRow, OvsdbTable.INTERFACE);

            String portType = (String) intf.getTypeColumn().data();
            long localPort = getOfPort(intf);
            String[] macAndIfaceId = getMacAndIfaceid(intf);
            if (macAndIfaceId == null) {
                return;
            }
            EventSubject eventSubject = new DefaultEventSubject(
                                                                MacAddress
                                                                        .valueOf(macAndIfaceId[0]),
                                                                new HashSet<IpAddress>(),
                                                                new OvsdbPortName(port.getName()),
                                                                new OvsdbPortNumber(localPort),
                                                                new OvsdbDatapathId(Long.toString(dpid)),
                                                                new OvsdbPortType(portType),
                                                                new OvsdbIfaceId(macAndIfaceId[1]));
            for (OvsdbEventListener listener : ovsdbEventListener) {
                listener.handle(new OvsdbEvent<EventSubject>(eventType,
                                                             eventSubject));
            }

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
            log.warn("The attachedMac is null");
            return null;
        }
        String ifaceid = externalIds
                .get(OvsdbConstant.EXTERNAL_ID_INTERFACE_ID);
        if (ifaceid == null) {
            log.warn("The ifaceid is null");
            return null;
        }
        return new String[] {attachedMac, ifaceid};
    }

    /**
     * Gets ofPorts number from table Interface.
     *
     * @param intf Interface instance
     * @return ofport the ofport number
     */
    private long getOfPort(Interface intf) {
        OvsdbSet ovsdbSet = (OvsdbSet) intf.getOpenFlowPortColumn().data();
        @SuppressWarnings("unchecked")
        Set<Long> ofPorts = ovsdbSet.set();
        while (ofPorts == null || ofPorts.size() <= 0) {
            log.debug("The ofport is null in {}", intf.getName());
            return 0;
        }
        return (long) ofPorts.toArray()[0];
    }

    /**
     * Gets datapathid from table bridge.
     *
     * @param clientService OvsdbClientService instance
     * @param dbSchema ovsdb database schema
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
        OvsdbSet ovsdbSet = (OvsdbSet) bridge.getDatapathIdColumn().data();
        @SuppressWarnings("unchecked")
        Set<String> dpids = ovsdbSet.set();
        if (dpids == null || dpids.size() == 0) {
            return 0;
        }
        return stringToLong((String) dpids.toArray()[0]);
    }

    private long stringToLong(String values) {
        long value = (new BigInteger(values.replaceAll(":", ""), 16))
                .longValue();
        return value;
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
