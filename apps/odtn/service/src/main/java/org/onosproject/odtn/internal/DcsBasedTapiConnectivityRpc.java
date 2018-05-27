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

import java.util.List;
import java.util.stream.Collectors;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.odtn.TapiResolver;
import org.onosproject.odtn.utils.tapi.TapiNepPair;
import org.onosproject.odtn.utils.tapi.TapiConnectionHandler;
import org.onosproject.odtn.utils.tapi.TapiConnectivityServiceHandler;
import org.onosproject.odtn.utils.tapi.TapiContextHandler;
import org.onosproject.odtn.utils.tapi.TapiCreateConnectivityInputHandler;
import org.onosproject.odtn.utils.tapi.TapiCreateConnectivityOutputHandler;
import org.onosproject.odtn.utils.tapi.TapiDeleteConnectivityInputHandler;
import org.onosproject.odtn.utils.tapi.TapiDeleteConnectivityOutputHandler;
import org.onosproject.odtn.utils.tapi.TapiGetConnectivityDetailsInputHandler;
import org.onosproject.odtn.utils.tapi.TapiGetConnectivityDetailsOutputHandler;
import org.onosproject.odtn.utils.tapi.TapiGetConnectivityListOutputHandler;
import org.onosproject.odtn.utils.tapi.TapiNepRef;
import org.onosproject.odtn.utils.tapi.TapiObjectHandler;
import org.onosproject.odtn.utils.tapi.TapiSepHandler;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.TapiConnectivityService;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.tapiconnectivity.connectivitycontext.DefaultConnectivityService;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.RpcInput;
import org.onosproject.yang.model.RpcOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onlab.osgi.DefaultServiceDirectory.getService;


/**
 * DCS-dependent tapi-connectivity yang RPCs implementation.
 */
public class DcsBasedTapiConnectivityRpc implements TapiConnectivityService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected DynamicConfigService dcs;
    protected ModelConverter modelConverter;
    protected TapiResolver resolver;

    public void init() {
        dcs = getService(DynamicConfigService.class);
        modelConverter = getService(ModelConverter.class);
        resolver = getService(TapiResolver.class);
    }

    /**
     * Service interface of createConnectivityService.
     *
     * @param inputVar input of service interface createConnectivityService
     * @return output of service interface createConnectivityService
     */
    @Override
    public RpcOutput createConnectivityService(RpcInput inputVar) {

        try {
            TapiCreateConnectivityInputHandler input = new TapiCreateConnectivityInputHandler();
            input.setRpcInput(inputVar);
            // TODO validation check
            log.info("input SIPs: {}", input.getSips());

            List<TapiNepRef> nepRefs = input.getSips().stream()
                    .map(sipId -> resolver.getNepRef(sipId))
                    .collect(Collectors.toList());
            // for test
//            Map<String, String> filter = new HashMap<>();
//            filter.put(ODTN_PORT_TYPE, OdtnDeviceDescriptionDiscovery.OdtnPortType.CLIENT.value());
//            List<TapiNepRef> nepRefs = resolver.getNepRefs(filter);

            // setup connections
            TapiNepPair neps = TapiNepPair.create(nepRefs.get(0), nepRefs.get(1));
            DcsBasedTapiConnectionManager connectionManager = DcsBasedTapiConnectionManager.create();
            connectionManager.createConnection(neps);

            // setup connectivity service
            TapiConnectivityServiceHandler connectivityServiceHandler = TapiConnectivityServiceHandler.create();
            connectivityServiceHandler.addConnection(connectionManager.getConnectionHandler().getModelObject().uuid());
            neps.stream()
                    .map(nepRef -> TapiSepHandler.create().setSip(nepRef.getSipId()))
                    .forEach(sepBuilder -> {
                        connectivityServiceHandler.addSep(sepBuilder.getModelObject());
                    });

            // build
            connectionManager.apply();
            connectivityServiceHandler.add();

            // output
            TapiCreateConnectivityOutputHandler output = TapiCreateConnectivityOutputHandler.create()
                    .addService(connectivityServiceHandler.getModelObject());
            return new RpcOutput(RpcOutput.Status.RPC_SUCCESS, output.getDataNode());

        } catch (Throwable e) {
            log.error("Error:", e);
            return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);
        }

    }


    /**
     * Service interface of deleteConnectivityService.
     *
     * @param inputVar input of service interface deleteConnectivityService
     * @return output of service interface deleteConnectivityService
     */
    @Override
    public RpcOutput deleteConnectivityService(RpcInput inputVar) {

        try {
            TapiDeleteConnectivityInputHandler input = new TapiDeleteConnectivityInputHandler();
            input.setRpcInput(inputVar);
            log.info("input serviceId: {}", input.getId());

            TapiConnectivityServiceHandler serviceHandler = TapiConnectivityServiceHandler.create();
            serviceHandler.setId(input.getId());

            DefaultConnectivityService service = serviceHandler.read();

            service.connection().stream().forEach(connection -> {
                TapiConnectionHandler connectionHandler = TapiConnectionHandler.create();
                connectionHandler.setId(Uuid.fromString(connection.connectionId().toString()));
                DcsBasedTapiConnectionManager manager = DcsBasedTapiConnectionManager.create();
                manager.deleteConnection(connectionHandler);
                manager.apply();
            });
            serviceHandler.remove();

            TapiDeleteConnectivityOutputHandler output = TapiDeleteConnectivityOutputHandler.create()
                    .addService(serviceHandler.getModelObject());

            return new RpcOutput(RpcOutput.Status.RPC_SUCCESS, output.getDataNode());
        } catch (Throwable e) {
            log.error("Error:", e);
            return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);
        }
    }

    /**
     * Service interface of updateConnectivityService.
     *
     * @param inputVar input of service interface updateConnectivityService
     * @return output of service interface updateConnectivityService
     */
    @Override
    public RpcOutput updateConnectivityService(RpcInput inputVar) {
        log.error("Not implemented");
        return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);

    }

    /**
     * Service interface of getConnectivityServiceList.
     *
     * @param inputVar input of service interface getConnectivityServiceList
     * @return output of service interface getConnectivityServiceList
     */
    @Override
    public RpcOutput getConnectivityServiceList(RpcInput inputVar) {

        try {
            TapiGetConnectivityListOutputHandler output = TapiGetConnectivityListOutputHandler.create();
            log.info("get list called");

            TapiContextHandler handler = TapiContextHandler.create();
            handler.read();
            log.info("model : {}", handler.getModelObject());
            log.info("conserv : {}", handler.getConnectivityServices());

            handler.getConnectivityServices().stream()
                    .map(TapiObjectHandler::getModelObject)
                    .forEach(output::addService);

            return new RpcOutput(RpcOutput.Status.RPC_SUCCESS, output.getDataNode());

        } catch (Throwable e) {
            log.error("Error:", e);
            return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);
        }
    }


    /**
     * Service interface of getConnectivityServiceDetails.
     *
     * @param inputVar input of service interface getConnectivityServiceDetails
     * @return output of service interface getConnectivityServiceDetails
     */
    @Override
    public RpcOutput getConnectivityServiceDetails(RpcInput inputVar) {

        try {
            TapiGetConnectivityDetailsInputHandler input = new TapiGetConnectivityDetailsInputHandler();
            input.setRpcInput(inputVar);
            log.info("input serviceId: {}", input.getId());

            TapiConnectivityServiceHandler handler = TapiConnectivityServiceHandler.create();
            handler.setId(input.getId());
            handler.read();

            TapiGetConnectivityDetailsOutputHandler output = TapiGetConnectivityDetailsOutputHandler.create()
                    .addService(handler.getModelObject());

            return new RpcOutput(RpcOutput.Status.RPC_SUCCESS, output.getDataNode());

        } catch (Throwable e) {
            return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);
        }

    }

    /**
     * Service interface of getConnectionDetails.
     *
     * @param inputVar input of service interface getConnectionDetails
     * @return output of service interface getConnectionDetails
     */
    @Override
    public RpcOutput getConnectionDetails(RpcInput inputVar) {
        log.error("Not implemented");
        return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);
    }

}
