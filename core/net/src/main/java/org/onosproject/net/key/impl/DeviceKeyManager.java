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

package org.onosproject.net.key.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyAdminService;
import org.onosproject.net.key.DeviceKeyEvent;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.key.DeviceKeyListener;
import org.onosproject.net.key.DeviceKeyService;
import org.onosproject.net.key.DeviceKeyStore;
import org.onosproject.net.key.DeviceKeyStoreDelegate;
import org.slf4j.Logger;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.DEVICE_KEY_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of device key services.
 */
@Component(immediate = true)
@Service
public class DeviceKeyManager extends AbstractListenerManager<DeviceKeyEvent, DeviceKeyListener>
        implements DeviceKeyService, DeviceKeyAdminService {

    private final Logger log = getLogger(getClass());

    private DeviceKeyStoreDelegate delegate = this::post;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceKeyStore store;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(DeviceKeyEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(DeviceKeyEvent.class);
        log.info("Stopped");
    }

    @Override
    public void addKey(DeviceKey deviceKey) {
        checkNotNull(deviceKey, "Device key cannot be null");
        store.createOrUpdateDeviceKey(deviceKey);
    }

    @Override
    public void removeKey(DeviceKeyId deviceKeyId) {
        checkNotNull(deviceKeyId, "Device key identifier cannot be null");
        store.deleteDeviceKey(deviceKeyId);
    }

    @Override
    public Collection<DeviceKey> getDeviceKeys() {
        checkPermission(DEVICE_KEY_READ);
        return store.getDeviceKeys();
    }

    @Override
    public DeviceKey getDeviceKey(DeviceKeyId deviceKeyId) {
        checkPermission(DEVICE_KEY_READ);
        checkNotNull(deviceKeyId, "Device key identifier cannot be null");
        return store.getDeviceKey(deviceKeyId);
    }
}

