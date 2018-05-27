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
import java.util.List;

import org.onosproject.config.FailedException;
import org.onosproject.net.device.DeviceService;
import org.onosproject.odtn.utils.tapi.TapiConnection;
import org.onosproject.odtn.utils.tapi.TapiNepPair;
import org.onosproject.odtn.utils.tapi.TapiCepRefHandler;
import org.onosproject.odtn.utils.tapi.TapiConnectionHandler;

import org.onosproject.odtn.utils.tapi.TapiRouteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onlab.osgi.DefaultServiceDirectory.getService;

/**
 * DCS-dependent Tapi connection manager implementation.
 */
public class DcsBasedTapiConnectionManager implements TapiConnectionManager {

    private final Logger log = LoggerFactory.getLogger(getClass());
    protected TapiPathComputer connectionController;
    private DeviceService deviceService;

    private List<DcsBasedTapiConnectionManager> connectionManagerList = new ArrayList<>();
    private TapiConnectionHandler connectionHandler = TapiConnectionHandler.create();
    private Operation op = null;


    enum Operation {
        CREATE,
        DELETE
    }

    public static DcsBasedTapiConnectionManager create() {
        DcsBasedTapiConnectionManager self = new DcsBasedTapiConnectionManager();
        self.connectionController = DefaultTapiPathComputer.create();
        self.deviceService = getService(DeviceService.class);
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

        // read target to be deleted
        this.connectionHandler = connectionHandler;
        this.connectionHandler.read();
        log.info("model: {}", connectionHandler.getModelObject());

        deleteConnectionRecursively(connectionHandler);
    }

    @Override
    public void apply() {
        connectionManagerList.forEach(DcsBasedTapiConnectionManager::apply);
        switch (op) {
            case CREATE:
                connectionHandler.add();
                break;
            case DELETE:
                connectionHandler.remove();
                break;
            default:
                throw new FailedException("Unknown operation type.");
        }
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
     * @param connectionHandler  connectionHandler of connection to be deleted
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
