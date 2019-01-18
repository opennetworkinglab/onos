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

import java.util.HashMap;
import java.util.Map;
import org.onosproject.net.DeviceId;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.onosproject.odtn.TapiResolver;
import org.onosproject.odtn.utils.tapi.TapiCepRef;
import org.onosproject.odtn.utils.tapi.TapiConnection;
import org.onosproject.odtn.utils.tapi.TapiNepPair;
import org.onosproject.odtn.utils.tapi.TapiNepRef;

import static org.onlab.osgi.DefaultServiceDirectory.getService;
import static org.onosproject.odtn.utils.tapi.TapiObjectHandler.DEVICE_ID;
import static org.onosproject.odtn.utils.tapi.TapiObjectHandler.ODTN_PORT_TYPE;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.CONNECTION_ID;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DCS-dependent Tapi path computation engine implementation.
 */
public class DefaultTapiPathComputer implements TapiPathComputer {

    private final Logger log = LoggerFactory.getLogger(getClass());
    protected TapiResolver resolver;

    public static DefaultTapiPathComputer create() {
        DefaultTapiPathComputer self = new DefaultTapiPathComputer();
        self.resolver = getService(TapiResolver.class);
        return self;
    }

    @Override
    public TapiConnection pathCompute(TapiNepPair neps) {
        log.info("Path compute with: {}", neps);
        return pathComputeDetail(neps);
    }

    /**
     * Compute and decide multi-hop route/path from e2e intent.
     * <p>
     * FIXME this can work only for Phase1.0, we need some features to:
     * - define Route and select media channel
     * - with pre-defined topology and forwarding constraint of each devices or domains
     * - get all ConnectionEndPoint in the defined route and return them
     * as list of Cep pair for each connection to be created
     */
    private TapiConnection pathComputeDetail(TapiNepPair neps) {
        log.debug("TapiNepPair {}", neps);
        log.debug("Nep0 {}", neps.left());
        log.debug("Nep1 {}", neps.right());

        /*
         * RCAS: 20190117 - We assume that if port type is LINE, it relies on intents.
         * We construct just a single top-most connection object.
         */
        if (neps.left().getPortType() == LINE) {
            log.info("Connection between line ports");
            TapiConnection connection = TapiConnection.create(
                TapiCepRef.create(neps.left(), neps.left().getCepIds().get(0)),
                TapiCepRef.create(neps.right(), neps.right().getCepIds().get(0))
                );
            return connection;
        } else {
            return mockPathCompute(neps);
        }
    }


    /**
     * Mock to create path computation result.
     *
     * neps.left           connection                 neps.right
     * ■----------------------------------------------------■
     *  \                                                  /
     *   \                                                /
     *    \ leftLowerConnection                          / rightLowerConnection
     *     \                                            /
     *      \                                          /
     *       ■                                        ■
     *   leftLineNep                             rightLineNep
     */
    private TapiConnection mockPathCompute(TapiNepPair neps) {
        TapiNepRef leftLineNep = mockGetTransponderLinePort(neps.left());
        TapiNepRef rightLineNep = mockGetTransponderLinePort(neps.right());

        TapiConnection leftLowerConnection = TapiConnection.create(
                TapiCepRef.create(neps.left(), neps.left().getCepIds().get(0)),
                TapiCepRef.create(leftLineNep, leftLineNep.getCepIds().get(0))
        );

        TapiConnection rightLowerConnection = TapiConnection.create(
                TapiCepRef.create(neps.right(), neps.right().getCepIds().get(0)),
                TapiCepRef.create(rightLineNep, rightLineNep.getCepIds().get(0))
        );

        TapiConnection connection = TapiConnection.create(
                TapiCepRef.create(neps.left(), neps.left().getCepIds().get(0)),
                TapiCepRef.create(neps.right(), neps.right().getCepIds().get(0))
        );
        connection.addLowerConnection(leftLowerConnection)
                .addLowerConnection(rightLowerConnection);

        return connection;
    }

    /**
     * Mock to select line port from client port.
     */
    private TapiNepRef mockGetTransponderLinePort(TapiNepRef cliNepRef) {
        DeviceId deviceId = cliNepRef.getConnectPoint().deviceId();
        Map<String, String> filter = new HashMap<>();
        filter.put(DEVICE_ID, deviceId.toString());
        filter.put(ODTN_PORT_TYPE, OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE.value());
        filter.put(CONNECTION_ID, cliNepRef.getConnectionId());
        return resolver.getNepRefs(filter).stream().findAny().get();
    }

}
