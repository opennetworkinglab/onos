/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.incubator.net.resource.label.impl;

import com.google.common.collect.Multimap;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceDelegate;
import org.onosproject.incubator.net.resource.label.LabelResourceEvent;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceListener;
import org.onosproject.incubator.net.resource.label.LabelResourcePool;
import org.onosproject.incubator.net.resource.label.LabelResourceProvider;
import org.onosproject.incubator.net.resource.label.LabelResourceProviderRegistry;
import org.onosproject.incubator.net.resource.label.LabelResourceProviderService;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.incubator.net.resource.label.LabelResourceStore;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceEvent.Type;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * provides implementation of the label resource NB &amp; SB APIs.
 *
 */
@Component(immediate = true,
           service = {LabelResourceService.class, LabelResourceAdminService.class, LabelResourceProviderRegistry.class})
public class LabelResourceManager
        extends AbstractListenerProviderRegistry<LabelResourceEvent, LabelResourceListener,
                                                 LabelResourceProvider, LabelResourceProviderService>
        implements LabelResourceService, LabelResourceAdminService, LabelResourceProviderRegistry {
    private final Logger log = getLogger(getClass());
    private final LabelResourceDelegate delegate = new InternalLabelResourceDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LabelResourceStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    private DeviceListener deviceListener = new InternalDeviceListener();

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(LabelResourceEvent.class, listenerRegistry);
        deviceService.addListener(deviceListener);
        log.info("Started");

    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(LabelResourceEvent.class);
        log.info("Stopped");
    }

    @Override
    public boolean createDevicePool(DeviceId deviceId,
                                    LabelResourceId beginLabel,
                                    LabelResourceId endLabel) {
        checkNotNull(deviceId, "deviceId is not null");
        checkNotNull(beginLabel, "beginLabel is not null");
        checkNotNull(endLabel, "endLabel is not null");
        checkArgument(beginLabel.labelId() >= 0 || endLabel.labelId() >= 0,
                      "The value of beginLabel and the value of endLabel must be both positive number.");
        checkArgument(beginLabel.labelId() < endLabel.labelId(),
                      "The value of endLabel must be greater than the value of beginLabel.");
        return store.createDevicePool(deviceId, beginLabel, endLabel);
    }

    @Override
    public boolean createGlobalPool(LabelResourceId beginLabel,
                                    LabelResourceId endLabel) {
        checkNotNull(beginLabel, "beginLabel is not null");
        checkNotNull(endLabel, "endLabel is not null");
        checkArgument(beginLabel.labelId() >= 0 && endLabel.labelId() >= 0,
                      "The value of beginLabel and the value of endLabel must be both positive number.");
        checkArgument(beginLabel.labelId() < endLabel.labelId(),
                      "The value of endLabel must be greater than the value of beginLabel.");
        return store.createGlobalPool(beginLabel, endLabel);
    }

    @Override
    public boolean destroyDevicePool(DeviceId deviceId) {
        checkNotNull(deviceId, "deviceId is not null");
        return store.destroyDevicePool(deviceId);
    }

    @Override
    public boolean destroyGlobalPool() {
        return store.destroyGlobalPool();
    }

    @Override
    public Collection<LabelResource> applyFromDevicePool(DeviceId deviceId,
                                                         long applyNum) {
        checkNotNull(deviceId, "deviceId is not null");
        return store.applyFromDevicePool(deviceId, applyNum);
    }

    @Override
    public Collection<LabelResource> applyFromGlobalPool(long applyNum) {
        return store.applyFromGlobalPool(applyNum);
    }

    @Override
    public boolean releaseToDevicePool(Multimap<DeviceId, LabelResource> release) {
        checkNotNull(release, "release is not null");
        return store.releaseToDevicePool(release);
    }

    @Override
    public boolean releaseToGlobalPool(Set<LabelResourceId> release) {
        checkNotNull(release, "release is not null");
        return store.releaseToGlobalPool(release);
    }

    @Override
    public boolean isDevicePoolFull(DeviceId deviceId) {
        checkNotNull(deviceId, "deviceId is not null");
        return store.isDevicePoolFull(deviceId);
    }

    @Override
    public boolean isGlobalPoolFull() {
        return store.isGlobalPoolFull();
    }

    @Override
    public long getFreeNumOfDevicePool(DeviceId deviceId) {
        checkNotNull(deviceId, "deviceId is not null");
        return store.getFreeNumOfDevicePool(deviceId);
    }

    @Override
    public long getFreeNumOfGlobalPool() {
        return store.getFreeNumOfGlobalPool();
    }

    @Override
    public LabelResourcePool getDeviceLabelResourcePool(DeviceId deviceId) {
        checkNotNull(deviceId, "deviceId is not null");
        return store.getDeviceLabelResourcePool(deviceId);
    }

    @Override
    public LabelResourcePool getGlobalLabelResourcePool() {
        return store.getGlobalLabelResourcePool();
    }

    private class InternalLabelResourceDelegate implements LabelResourceDelegate {
        @Override
        public void notify(LabelResourceEvent event) {
            post(event);
        }

    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            if (Type.DEVICE_REMOVED.equals(event.type())) {
                destroyDevicePool(device.id());
            }
        }
    }

    private class InternalLabelResourceProviderService
            extends AbstractProviderService<LabelResourceProvider>
            implements LabelResourceProviderService {

        protected InternalLabelResourceProviderService(LabelResourceProvider provider) {
            super(provider);
        }

        @Override
        public void deviceLabelResourcePoolDetected(DeviceId deviceId,
                                                    LabelResourceId beginLabel,
                                                    LabelResourceId endLabel) {
            checkNotNull(deviceId, "deviceId is not null");
            checkNotNull(beginLabel, "beginLabel is not null");
            checkNotNull(endLabel, "endLabel is not null");
            createDevicePool(deviceId, beginLabel, endLabel);
        }

        @Override
        public void deviceLabelResourcePoolDestroyed(DeviceId deviceId) {
            checkNotNull(deviceId, "deviceId is not null");
            destroyDevicePool(deviceId);
        }

    }

    @Override
    protected LabelResourceProviderService createProviderService(LabelResourceProvider provider) {
        return new InternalLabelResourceProviderService(provider);
    }
}
