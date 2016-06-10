/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.rpc.nb.impl;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.incubator.rpc.nb.mcast.MulticastRouteGrpcService;
import org.onosproject.net.mcast.MulticastRouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * gRPC server for northbound APIs.
 */
@Component(immediate = true)
public class GrpcServer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MulticastRouteService multicastRouteService;

    // TODO make configurable
    private int port = 50051;

    private Server server;

    @Activate
    public void activate() {
        start();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        stop();
        log.info("Stopped");
    }

    private void start() {
        try {
            server = NettyServerBuilder.forPort(port)
                    .addService(new MulticastRouteGrpcService(multicastRouteService))
                    .build()
                    .start();
            log.info("gRPC server started listening on " + port);
        } catch (IOException e) {
            log.error("Failed to start gRPC server", e);
        }
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
}
