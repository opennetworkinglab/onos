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

package org.onosproject.bmv2.ctl;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onlab.util.SharedScheduledExecutors;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2DefaultConfiguration;
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.bmv2.api.runtime.Bmv2DeviceAgent;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.bmv2.api.service.Bmv2DeviceContextService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.bmv2.api.context.Bmv2DefaultConfiguration.parse;
import static org.onosproject.store.service.MapEvent.Type.INSERT;
import static org.onosproject.store.service.MapEvent.Type.UPDATE;

@Component(immediate = true)
@Service
public class Bmv2DeviceContextServiceImpl implements Bmv2DeviceContextService {

    private static final String JSON_DEFAULT_CONFIG_PATH = "/default.json";
    private static final long CHECK_INTERVAL = 5_000; // milliseconds

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Bmv2Controller controller;

    private final ScheduledExecutorService scheduledExecutor = SharedScheduledExecutors.getPoolThreadExecutor();
    private final MapEventListener<DeviceId, Bmv2DeviceContext> contextListener = new ContextMapEventListener();
    private final ConcurrentMap<DeviceId, Lock> deviceLocks = Maps.newConcurrentMap();

    private ConsistentMap<DeviceId, Bmv2DeviceContext> contexts;
    private Map<String, ClassLoader> interpreterClassLoaders;
    private Bmv2DeviceContext defaultContext;
    private  ScheduledFuture<?> configChecker = null;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    public void activate() {
        KryoNamespace kryo = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(new BmvDeviceContextSerializer(), Bmv2DeviceContext.class)
                .build();

        this.contexts = storageService.<DeviceId, Bmv2DeviceContext>consistentMapBuilder()
                .withSerializer(Serializer.using(kryo))
                .withName("onos-bmv2-contexts")
                .build();

        Bmv2Configuration defaultConfiguration = loadDefaultConfiguration();
        Bmv2Interpreter defaultInterpreter = new Bmv2DefaultInterpreterImpl();
        defaultContext = new Bmv2DeviceContext(defaultConfiguration, defaultInterpreter);

        interpreterClassLoaders = Maps.newConcurrentMap();
        registerInterpreterClassLoader(defaultInterpreter.getClass(), this.getClass().getClassLoader());

        contexts.addListener(contextListener, scheduledExecutor);

        if (configChecker != null &&  configChecker.isCancelled()) {
            configChecker.cancel(false);
        }
        configChecker = scheduledExecutor.scheduleAtFixedRate(this::checkDevices, 0, CHECK_INTERVAL,
                                                              TimeUnit.MILLISECONDS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        contexts.removeListener(contextListener);
        if (configChecker != null) {
            configChecker.cancel(false);
        }
        log.info("Stopped");
    }

    @Override
    public Bmv2DeviceContext getContext(DeviceId deviceId) {
        checkNotNull(deviceId, "device id cannot be null");
        Versioned<Bmv2DeviceContext> versionedContext = contexts.get(deviceId);
        return (versionedContext == null) ? null : versionedContext.value();
    }

    @Override
    public void setContext(DeviceId deviceId, Bmv2DeviceContext context) {
        checkNotNull(deviceId, "device id cannot be null");
        checkNotNull(context, "context cannot be null");
        if (!interpreterClassLoaders.containsKey(context.interpreter().getClass().getName())) {
            log.error("Unable to set context, missing class loader for interpreter '{}'. " +
                              "Please register it with registerInterpreterClassLoader()",
                      context.interpreter().getClass().getName());
        } else {
            try {
                contexts.put(deviceId, context);
            } catch (ConsistentMapException.ConcurrentModification e) {
                log.error("Detected concurrent modification on context map");
            }
        }
    }

    @Override
    public void registerInterpreterClassLoader(Class<? extends Bmv2Interpreter> interpreterClass, ClassLoader loader) {
        interpreterClassLoaders.put(interpreterClass.getName(), loader);
    }

    @Override
    public Bmv2DeviceContext defaultContext() {
        return defaultContext;
    }

    @Override
    public void setDefaultContext(DeviceId deviceId) {
        Versioned<Bmv2DeviceContext> previous = contexts.put(deviceId, defaultContext);
        if (mastershipService.getMasterFor(deviceId) == null) {
            // Checking for who is the master here is ugly but necessary, as this method is called by Bmv2DeviceProvider
            // prior to master election. A solution could be to use a separate leadership contest instead of the
            // mastership service.
            triggerConfigCheck(deviceId, defaultContext);
        }
    }

    private void configCheck(DeviceId deviceId, Bmv2DeviceContext storedContext) {
        if (storedContext == null) {
            return;
        }
        // Synchronize executions over the same deviceId.
        Lock lock = deviceLocks.computeIfAbsent(deviceId, did -> new ReentrantLock());
        lock.lock();
        try {
            log.trace("Executing configuration check on {}...", deviceId);

            try {
                // FIXME: JSON dump is heavy, can we use the JSON MD5 to check the running configuration?
                String jsonString = controller.getAgent(deviceId).dumpJsonConfig();
                Bmv2Configuration deviceConfiguration = parse(Json.parse(jsonString).asObject());

                if (!storedContext.configuration().equals(deviceConfiguration)) {
                    log.info("Triggering configuration swap on {}...", deviceId);
                    try {
                        Bmv2DeviceAgent agent = controller.getAgent(deviceId);
                        String newJsonString = storedContext.configuration().json().toString();
                        agent.uploadNewJsonConfig(newJsonString);
                        agent.swapJsonConfig();
                    } catch (Bmv2RuntimeException e) {
                        log.error("Unable to swap configuration on {}: {}", deviceId, e.explain());
                    }
                }
            } catch (Bmv2RuntimeException e) {
                log.warn("Unable to dump JSON configuration from {}: {}", deviceId, e.explain());
            }
        } finally {
            lock.unlock();
        }
    }

    private void triggerConfigCheck(DeviceId deviceId, Bmv2DeviceContext context) {
            scheduledExecutor.schedule(() -> configCheck(deviceId, context), 0, TimeUnit.SECONDS);
    }

    private void checkDevices() {
        deviceService.getAvailableDevices().forEach(device -> {
            if (mastershipService.isLocalMaster(device.id())) {
                triggerConfigCheck(device.id(), getContext(device.id()));
            }
        });
    }

    protected static Bmv2DefaultConfiguration loadDefaultConfiguration() {
        try {
            JsonObject json = Json.parse(new BufferedReader(new InputStreamReader(
                    Bmv2DeviceContextServiceImpl.class.getResourceAsStream(JSON_DEFAULT_CONFIG_PATH)))).asObject();
            return parse(json);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load default configuration", e);
        }
    }

    /**
     * Listener of context changes that immediately triggers config checks (to swap the config if necessary).
     */
    private class ContextMapEventListener implements MapEventListener<DeviceId, Bmv2DeviceContext> {
        @Override
        public void event(MapEvent<DeviceId, Bmv2DeviceContext> event) {
            DeviceId deviceId = event.key();
            if (event.type().equals(INSERT) || event.type().equals(UPDATE)) {
                if (mastershipService.isLocalMaster(deviceId)) {
                    log.trace("Context {} for {}", event.type().name(), deviceId);
                    triggerConfigCheck(deviceId, event.newValue().value());
                }
            }
        }
    }

    /**
     * Context serializer.
     */
    private class BmvDeviceContextSerializer extends com.esotericsoftware.kryo.Serializer<Bmv2DeviceContext> {

        @Override
        public void write(Kryo kryo, Output output, Bmv2DeviceContext context) {
            kryo.writeObject(output, context.configuration().json().toString());
            kryo.writeObject(output, context.interpreter().getClass().getName());
        }

        @Override
        public Bmv2DeviceContext read(Kryo kryo, Input input, Class<Bmv2DeviceContext> type) {
            String jsonStr = kryo.readObject(input, String.class);
            String interpreterClassName = kryo.readObject(input, String.class);
            Bmv2Configuration configuration = parse(Json.parse(jsonStr).asObject());
            ClassLoader loader = interpreterClassLoaders.get(interpreterClassName);
            if (loader == null) {
                throw new IllegalStateException("No class loader registered for interpreter: " + interpreterClassName);
            }
            try {
                Bmv2Interpreter interpreter = (Bmv2Interpreter) loader.loadClass(interpreterClassName).newInstance();
                return new Bmv2DeviceContext(configuration, interpreter);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new RuntimeException("Unable to load interpreter class", e);
            }
        }
    }
}
