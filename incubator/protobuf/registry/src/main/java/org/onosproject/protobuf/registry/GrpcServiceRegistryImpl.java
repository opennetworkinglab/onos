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

package org.onosproject.protobuf.registry;

import com.google.common.collect.Maps;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.protobuf.api.GrpcServiceRegistry;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.onlab.util.Tools.get;

/**
 * A basic implementation of {@link GrpcServiceRegistry} designed for use with
 * built in gRPC services.
 *
 * NOTE: this is an early implementation in which the addition of any new
 * service forces a restart of the server, this is sufficient for testing but
 * inappropriate for deployment.
 */
@Service
@Component(immediate = false)
public class GrpcServiceRegistryImpl implements GrpcServiceRegistry {

    private static final int DEFAULT_SERVER_PORT = 64000;
    private static final int DEFAULT_SHUTDOWN_TIME = 1;
    private static final AtomicBoolean SERVICES_MODIFIED_SINCE_START = new AtomicBoolean(false);

    private static final String PORT_PROPERTY_NAME = "listeningPort";

    private final Map<Class<? extends BindableService>, BindableService> registeredServices =
            Maps.newHashMap();
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Server server;

    /* It is currently the responsibility of the administrator to notify
    clients of nonstandard port usage as there is no mechanism available to
    discover the port hosting gRPC services.
     */
    @Property(name = PORT_PROPERTY_NAME, intValue = DEFAULT_SERVER_PORT,
             label = "The port number which ONOS will use to host gRPC services.")
    private int listeningPort = DEFAULT_SERVER_PORT;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        attemptGracefulShutdownThenForce(DEFAULT_SHUTDOWN_TIME);
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context != null) {
            setProperties(context);
        }
        log.info("Connection was restarted to allow service to be added, " +
                         "this is a temporary workaround");
        restartServer(listeningPort);
    }

    @Override
    public boolean register(BindableService service) {
        synchronized (registeredServices) {
            if (!registeredServices.containsKey(service.getClass())) {
                registeredServices.put(service.getClass(), service);
            } else {
                log.warn("The specified class \"{}\" was not added becuase an " +
                                 "instance of the class is already registered.",
                         service.getClass().toString());
                return false;
            }
        }
        return restartServer(listeningPort);
    }

    @Override
    public boolean unregister(BindableService service) {
        synchronized (registeredServices) {
            if (registeredServices.containsKey(service.getClass())) {
                registeredServices.remove(service.getClass());
            } else {
                log.warn("The specified class \"{}\" was not removed because it " +
                                 "was not present.", service.getClass().toString());
                return false;
            }
        }
        return restartServer(listeningPort);
    }

    @Override
    public boolean containsService(Class<BindableService> serviceClass) {
        return registeredServices.containsKey(serviceClass);
    }

    private void setProperties(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        String listeningPort = get(properties, PORT_PROPERTY_NAME);
        this.listeningPort = listeningPort == null ? DEFAULT_SERVER_PORT :
                Integer.parseInt(listeningPort.trim());
    }

    /**
     * Attempts a graceful shutdown allowing {@code timeLimitSeconds} to elapse
     * before forcing a shutdown.
     *
     * @param timeLimitSeconds time before a shutdown is forced in seconds
     * @return true if the server is terminated, false otherwise
     */
    private boolean attemptGracefulShutdownThenForce(int timeLimitSeconds) {
        if (!server.isShutdown()) {
            server.shutdown();
        }
        try {
            /*This is not conditional in case the server is shutdown but
            handling requests submitted before shutdown was called.*/
            server.awaitTermination(timeLimitSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Awaiting server termination failed with error {}",
                      e.getMessage());
            Thread.currentThread().interrupt();
        }
        if (!server.isTerminated()) {
            server.shutdownNow();
            try {
                server.awaitTermination(10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.error("Server failed to terminate as expected with error" +
                                  " {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        return server.isTerminated();
    }

    private boolean restartServer(int port) {
        if (!attemptGracefulShutdownThenForce(DEFAULT_SHUTDOWN_TIME)) {
            log.error("Shutdown failed, the previous server may still be" +
                              " active.");
        }
        return createServerAndStart(port);
    }

    /**
     * Creates a server with the set of registered services on the specified
     * port.
     *
     * @param port the port on which this server will listen
     * @return true if the server was started successfully, false otherwise
     */
    private boolean createServerAndStart(int port) {

        ServerBuilder serverBuilder =
                ServerBuilder.forPort(port);
        synchronized (registeredServices) {
            registeredServices.values().forEach(
                    service -> serverBuilder.addService(service));
        }
        server = serverBuilder.build();
        try {
            server.start();
        } catch (IllegalStateException e) {
            log.error("The server could not be started because an existing " +
                              "server is already running: {}", e.getMessage());
            return false;
        } catch (IOException e) {
            log.error("The server could not be started due to a failure to " +
                              "bind: {} ", e.getMessage());
            return false;
        }
        return true;
    }
}
