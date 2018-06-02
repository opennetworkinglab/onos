/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.odtn.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.concurrent.atomic.AtomicReference;
import org.onosproject.config.FailedException;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.odtn.TapiResolver;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.onosproject.odtn.utils.tapi.DcsBasedTapiObjectRefFactory;
import org.onosproject.odtn.utils.tapi.TapiCepPair;
import org.onosproject.odtn.utils.tapi.TapiConnection;
import org.onosproject.odtn.utils.tapi.TapiNepPair;
import org.onosproject.odtn.utils.tapi.TapiCepRefHandler;
import org.onosproject.odtn.utils.tapi.TapiConnectionHandler;

import org.onosproject.odtn.utils.tapi.TapiNepRef;
import org.onosproject.odtn.utils.tapi.TapiRouteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onlab.osgi.DefaultServiceDirectory.getService;

/**
 * DCS-dependent Tapi connection manager implementation.
 */
public final class DcsBasedTapiConnectionManager implements TapiConnectionManager {

    private final Logger log = LoggerFactory.getLogger(getClass());
    protected TapiPathComputer connectionController;
    private TapiResolver resolver;
    private NetworkConfigService netcfgService;

    private List<DcsBasedTapiConnectionManager> connectionManagerList = new ArrayList<>();
    private TapiConnection connection = null;
    private TapiConnectionHandler connectionHandler = TapiConnectionHandler.create();
    private Operation op = null;


    enum Operation {
        CREATE,
        DELETE
    }

    private DcsBasedTapiConnectionManager() {
    }

    public static DcsBasedTapiConnectionManager create() {
        DcsBasedTapiConnectionManager self = new DcsBasedTapiConnectionManager();
        self.connectionController = DefaultTapiPathComputer.create();
        self.resolver = getService(TapiResolver.class);
        self.netcfgService = getService(NetworkConfigService.class);
        return self;
    }

    @Override
    public TapiConnectionHandler createConnection(TapiNepPair neps) {

        // Calculate route
        TapiConnection connection = connectionController.pathCompute(neps);
        log.info("Calculated path: {}", connection);

        createConnectionRecursively(connection);
        return connectionHandler;
    }

    @Override
    public void deleteConnection(TapiConnectionHandler connectionHandler) {

        deleteConnectionRecursively(connectionHandler);
    }

    @Override
    public void apply() {
        connectionManagerList.forEach(DcsBasedTapiConnectionManager::apply);

        switch (op) {
            case CREATE:
                notifyDeviceConfigChange(true);
                connectionHandler.add();
                break;
            case DELETE:
                notifyDeviceConfigChange(false);
                connectionHandler.remove();
                break;
            default:
                throw new FailedException("Unknown operation type.");
        }
    }

    /**
     * Emit NetworkConfig event with parameters for device config,
     * to notify configuration change to device drivers.
     */
    private void notifyDeviceConfigChange(boolean enable) {
        if (!this.connection.getCeps().isSameNode()) {
            return;
        }

        TapiNepRef left = this.connection.getCeps().left().getNepRef();
        TapiNepRef right = this.connection.getCeps().right().getNepRef();

        // update with latest data in DCS
        left = resolver.getNepRef(left);
        right = resolver.getNepRef(right);

        AtomicReference<TapiNepRef> line = new AtomicReference<>();
        AtomicReference<TapiNepRef> client = new AtomicReference<>();
        Arrays.asList(left, right).forEach(nep -> {
            if (nep.getPortType() == OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE) {
                line.set(nep);
            }
            if (nep.getPortType() == OdtnDeviceDescriptionDiscovery.OdtnPortType.CLIENT) {
                client.set(nep);
            }
        });

        DeviceConfigEventEmitter eventEmitter = DeviceConfigEventEmitter.create();
        eventEmitter.emit(line.get(), client.get(), enable);
    }

    /**
     * Generate TAPI connection and its under connections recursively
     * and add them to creation queue.
     *
     * @param connection connection to be created
     */
    private void createConnectionRecursively(TapiConnection connection) {
        op = Operation.CREATE;
        connectionManagerList.clear();
        this.connection = connection;

        TapiRouteHandler routeBuilder = TapiRouteHandler.create();

        // Create under connection, and set them into routeBuilder
        connection.getLowerConnections().forEach(lowerConnection -> {
            delegateConnectionCreation(lowerConnection);
            routeBuilder.addCep(lowerConnection.getCeps().left());
            routeBuilder.addCep(lowerConnection.getCeps().right());
        });

        connectionHandler.addRoute(routeBuilder.getModelObject());

        connectionHandler.addCep(TapiCepRefHandler.create()
                .setCep(connection.getCeps().left()).getModelObject());
        connectionHandler.addCep(TapiCepRefHandler.create()
                .setCep(connection.getCeps().right()).getModelObject());

        connectionManagerList.forEach(manager ->
                connectionHandler.addLowerConnection(manager.getConnectionHandler().getModelObject()));

    }

    /**
     * Generate TAPI connection and its under connections recursively
     * and add them to deletion queue.
     *
     * @param connectionHandler connectionHandler of connection to be deleted
     */
    private void deleteConnectionRecursively(TapiConnectionHandler connectionHandler) {
        op = Operation.DELETE;
        connectionManagerList.clear();

        // read target to be deleted
        connectionHandler.read();
        log.info("model: {}", connectionHandler.getModelObject());

        this.connection = TapiConnection.create(
                TapiCepPair.create(
                        DcsBasedTapiObjectRefFactory.create(connectionHandler.getCeps().get(0)),
                        DcsBasedTapiObjectRefFactory.create(connectionHandler.getCeps().get(1)))
        );

        this.connectionHandler = connectionHandler;
        this.connectionHandler.getLowerConnections().forEach(lowerConnectionHandler -> {
            delegateConnectionDeletion(lowerConnectionHandler);
        });
    }

    /**
     * Delegate lower-connection creation to other corresponding TapiConnectionManager of each Nodes.
     *
     * @param connection connection to be created
     */
    private void delegateConnectionCreation(TapiConnection connection) {
        log.info("ceps: {}", connection.getCeps());
        DcsBasedTapiConnectionManager manager = DcsBasedTapiConnectionManager.create();
        manager.createConnectionRecursively(connection);
        connectionManagerList.add(manager);
    }

    /**
     * Delegate lower-connection deletion to other corresponding TapiConnectionManager of each Nodes.
     *
     * @param connectionHandler connectionHandler of connection to be deleted
     */
    private void delegateConnectionDeletion(TapiConnectionHandler connectionHandler) {
        log.info("model: {}", connectionHandler.getModelObject());
        DcsBasedTapiConnectionManager manager = DcsBasedTapiConnectionManager.create();
        manager.deleteConnectionRecursively(connectionHandler);
        connectionManagerList.add(manager);
    }


    public TapiConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

}
