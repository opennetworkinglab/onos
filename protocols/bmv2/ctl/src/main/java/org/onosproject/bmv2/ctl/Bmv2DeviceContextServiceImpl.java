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
import org.onlab.util.SharedExecutors;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2DefaultConfiguration;
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.bmv2.api.runtime.Bmv2DeviceAgent;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.bmv2.api.service.Bmv2DeviceContextService;
import org.onosproject.net.DeviceId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;

@Component(immediate = true)
@Service
public class Bmv2DeviceContextServiceImpl implements Bmv2DeviceContextService {

    private static final String JSON_DEFAULT_CONFIG_PATH = "/default.json";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Bmv2Controller controller;

    private final ExecutorService executorService = SharedExecutors.getPoolThreadExecutor();

    private ConsistentMap<DeviceId, Bmv2DeviceContext> contexts;
    private Map<DeviceId, Bmv2DeviceContext> contextsMap;

    private Map<String, ClassLoader> interpreterClassLoaders;

    private Bmv2DeviceContext defaultContext;

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
        contextsMap = contexts.asJavaMap();

        interpreterClassLoaders = Maps.newConcurrentMap();

        Bmv2Configuration defaultConfiguration = loadDefaultConfiguration();
        Bmv2Interpreter defaultInterpreter = new Bmv2DefaultInterpreterImpl();
        defaultContext = new Bmv2DeviceContext(defaultConfiguration, defaultInterpreter);

        interpreterClassLoaders.put(defaultInterpreter.getClass().getName(), this.getClass().getClassLoader());

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Bmv2DeviceContext getContext(DeviceId deviceId) {
        checkNotNull(deviceId, "device id cannot be null");
        return contextsMap.get(deviceId);
    }

    @Override
    public void triggerConfigurationSwap(DeviceId deviceId, Bmv2DeviceContext context) {
        checkNotNull(deviceId, "device id cannot be null");
        checkNotNull(context, "context cannot be null");
        if (!interpreterClassLoaders.containsKey(context.interpreter().getClass().getName())) {
            log.error("Unable to trigger configuration swap, missing class loader for context interpreter. " +
                              "Please register it with registerInterpreterClassLoader()");
        } else {
            executorService.execute(() -> executeConfigurationSwap(deviceId, context));
        }
    }

    @Override
    public void registerInterpreterClassLoader(Class<? extends Bmv2Interpreter> interpreterClass, ClassLoader loader) {
        interpreterClassLoaders.put(interpreterClass.getName(), loader);
    }

    private void executeConfigurationSwap(DeviceId deviceId, Bmv2DeviceContext context) {
        contexts.compute(deviceId, (key, existingValue) -> {
            if (context.equals(existingValue)) {
                log.info("Dropping swap request as one has already been triggered for the given context.");
                return existingValue;
            }
            try {
                Bmv2DeviceAgent agent = controller.getAgent(deviceId);
                String jsonString = context.configuration().json().toString();
                agent.loadNewJsonConfig(jsonString);
                agent.swapJsonConfig();
                return context;
            } catch (Bmv2RuntimeException e) {
                log.error("Unable to swap configuration on {}: {}", deviceId, e.explain());
                return existingValue;
            }
        });
    }

    @Override
    public boolean notifyDeviceChange(DeviceId deviceId) {
        checkNotNull(deviceId, "device id cannot be null");

        Bmv2DeviceContext storedContext = getContext(deviceId);

        if (storedContext == null) {
            log.info("No context previously stored for {}, swapping to DEFAULT_CONTEXT.", deviceId);
            triggerConfigurationSwap(deviceId, defaultContext);
            // Device can be accepted.
            return false;
        } else {
            Bmv2Configuration deviceConfiguration = loadDeviceConfiguration(deviceId);
            if (deviceConfiguration == null) {
                log.warn("Unable to load configuration from device {}", deviceId);
                return false;
            }
            if (storedContext.configuration().equals(deviceConfiguration)) {
                return true;
            } else {
                log.info("Device context is different from the stored one, triggering configuration swap for {}...",
                         deviceId);
                triggerConfigurationSwap(deviceId, storedContext);
                return false;
            }
        }
    }

    /**
     * Load and parse a BMv2 JSON configuration from the given device.
     *
     * @param deviceId a device id
     * @return a BMv2 configuration
     */
    private Bmv2Configuration loadDeviceConfiguration(DeviceId deviceId) {
        try {
            String jsonString = controller.getAgent(deviceId).dumpJsonConfig();
            return Bmv2DefaultConfiguration.parse(Json.parse(jsonString).asObject());
        } catch (Bmv2RuntimeException e) {
            log.warn("Unable to load JSON configuration from {}: {}", deviceId, e.explain());
            return null;
        }
    }

    /**
     * Loads default configuration from file.
     *
     * @return a BMv2 configuration
     */
    protected static Bmv2DefaultConfiguration loadDefaultConfiguration() {
        try {
            JsonObject json = Json.parse(new BufferedReader(new InputStreamReader(
                    Bmv2DeviceContextServiceImpl.class.getResourceAsStream(JSON_DEFAULT_CONFIG_PATH)))).asObject();
            return Bmv2DefaultConfiguration.parse(json);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load default configuration", e);
        }
    }

    /**
     * Internal BMv2 context serializer.
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
            Bmv2Configuration configuration = Bmv2DefaultConfiguration.parse(Json.parse(jsonStr).asObject());
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
