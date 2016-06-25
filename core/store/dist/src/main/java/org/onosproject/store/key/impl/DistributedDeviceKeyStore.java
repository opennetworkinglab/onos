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

package org.onosproject.store.key.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyEvent;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.key.DeviceKeyStore;
import org.onosproject.net.key.DeviceKeyStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A distributed device key store implementation, device keys are stored consistently
 * across the cluster.
 */
@Component(immediate = true)
@Service
public class DistributedDeviceKeyStore
        extends AbstractStore<DeviceKeyEvent, DeviceKeyStoreDelegate>
        implements DeviceKeyStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ConsistentMap<DeviceKeyId, DeviceKey> deviceKeys;
    private Map<DeviceKeyId, DeviceKey> deviceKeysMap;

    private final MapEventListener<DeviceKeyId, DeviceKey> listener = new InternalMapListener();

    /**
     * Activate the distributed device key store.
     */
    @Activate
    public void activate() {
        deviceKeys = storageService.<DeviceKeyId, DeviceKey>consistentMapBuilder()
                .withSerializer(Serializer.using(Arrays.asList(KryoNamespaces.API),
                                DeviceKey.class,
                                DeviceKeyId.class,
                                DeviceKey.Type.class))
                .withName("onos-device-keys")
                .withRelaxedReadConsistency()
                .build();
        deviceKeys.addListener(listener);
        deviceKeysMap = deviceKeys.asJavaMap();

        log.info("Started");
    }

    /**
     * Deactivate the distributed device key store.
     */
    @Deactivate
    public void deactivate() {
        deviceKeys.removeListener(listener);
        log.info("Stopped");
    }

    @Override
    public void createOrUpdateDeviceKey(DeviceKey deviceKey) {

        // Add the device key to the store, if the device key already exists
        // then it will be replaced with the new one.
        deviceKeys.put(deviceKey.deviceKeyId(), deviceKey);
    }

    @Override
    public void deleteDeviceKey(DeviceKeyId deviceKeyId) {
        // Remove the device key from the store if the device key identifier exists.
        deviceKeys.remove(deviceKeyId);
    }

    @Override
    public Collection<DeviceKey> getDeviceKeys() {
        return deviceKeysMap.values();
    }

    @Override
    public DeviceKey getDeviceKey(DeviceKeyId deviceKeyId) {
        return deviceKeysMap.get(deviceKeyId);
    }

    /**
     * Listener class to map listener events to the device key events.
     */
    private class InternalMapListener implements MapEventListener<DeviceKeyId, DeviceKey> {
        @Override
        public void event(MapEvent<DeviceKeyId, DeviceKey> event) {
            DeviceKey deviceKey = null;

            DeviceKeyEvent.Type type = null;
            switch (event.type()) {
                case INSERT:
                    type = DeviceKeyEvent.Type.DEVICE_KEY_ADDED;
                    deviceKey = checkNotNull(event.newValue().value());
                    break;
                case UPDATE:
                    type = DeviceKeyEvent.Type.DEVICE_KEY_UPDATED;
                    deviceKey = checkNotNull(event.newValue().value());
                    break;
                case REMOVE:
                    type = DeviceKeyEvent.Type.DEVICE_KEY_REMOVED;
                    deviceKey = checkNotNull(event.oldValue().value());
                    break;
                default:
                    log.error("Unsupported event type: " + event.type());
            }
            notifyDelegate(new DeviceKeyEvent(type, deviceKey));
        }
    }
}
