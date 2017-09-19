/*
* Copyright 2017-present Open Networking Foundation
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

package org.onosproject.incubator.protobuf.services.nb;

import com.google.common.collect.Sets;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.AbstractServerImplBuilder;

import java.io.IOException;
import java.util.Set;
/**
 * InProcessServer that manages startup/shutdown of a service within the same process
 * as the client is running. Used for unit testing purposes.
 */
public class InProcessServer<T extends io.grpc.BindableService> {
    private Server server;

    Set<T> services = Sets.newHashSet();

    private Class<T> clazz;

    public InProcessServer(Class<T> clazz) {
        this.clazz = clazz;
    }

    public void addServiceToBind(T service) {
        if (service != null) {
            services.add(service);
        }
    }

    public void start() throws IOException, InstantiationException, IllegalAccessException {

        AbstractServerImplBuilder builder = InProcessServerBuilder.forName("test").directExecutor();
        services.forEach(service -> builder.addService(service));
        server = builder.build().start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                InProcessServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     * @throws InterruptedException if there is an issue
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}