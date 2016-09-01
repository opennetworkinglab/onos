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
package org.onosproject.pcelabelstore.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import org.onosproject.incubator.net.resource.label.DefaultLabelResource;
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
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceEvent.Type;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;

import com.google.common.collect.Multimap;

/**
 * Provides test implementation of class LabelResourceService.
 */
public class LabelResourceAdapter
        extends AbstractListenerProviderRegistry<LabelResourceEvent, LabelResourceListener,
                                                 LabelResourceProvider, LabelResourceProviderService>
        implements LabelResourceService, LabelResourceAdminService, LabelResourceProviderRegistry {
    public static final long GLOBAL_LABEL_SPACE_MIN = 4097;
    public static final long GLOBAL_LABEL_SPACE_MAX = 5121;
    public static final long LOCAL_LABEL_SPACE_MIN = 5122;
    public static final long LOCAL_LABEL_SPACE_MAX = 9217;

    private Random random = new Random();

    @Override
    public boolean createDevicePool(DeviceId deviceId,
                                    LabelResourceId beginLabel,
                                    LabelResourceId endLabel) {
       return true;
    }

    @Override
    public boolean createGlobalPool(LabelResourceId beginLabel,
                                    LabelResourceId endLabel) {
       return true;
    }

    @Override
    public boolean destroyDevicePool(DeviceId deviceId) {
       return true;
    }

    @Override
    public boolean destroyGlobalPool() {
       return true;
    }

    public long getLabelId(long min, long max) {
      return random.nextInt((int) max - (int) min + 1) + (int) min;
    }

    @Override
    public Collection<LabelResource> applyFromDevicePool(DeviceId deviceId,
                                                  long applyNum) {
        Collection<LabelResource> labelList = new LinkedList<>();
        LabelResource label = new DefaultLabelResource(deviceId,
                                  LabelResourceId.labelResourceId(
                                  getLabelId(LOCAL_LABEL_SPACE_MIN, LOCAL_LABEL_SPACE_MAX)));
        labelList.add(label);
        return labelList;
    }

    @Override
    public Collection<LabelResource> applyFromGlobalPool(long applyNum) {
        Collection<LabelResource> labelList = new LinkedList<>();
        LabelResource label = new DefaultLabelResource(DeviceId.deviceId("foo"),
                                  LabelResourceId.labelResourceId(
                                  getLabelId(GLOBAL_LABEL_SPACE_MIN, GLOBAL_LABEL_SPACE_MAX)));
        labelList.add(label);
        return labelList;
    }

    @Override
    public boolean releaseToDevicePool(Multimap<DeviceId, LabelResource> release) {
       return true;
    }

    @Override
    public boolean releaseToGlobalPool(Set<LabelResourceId> release) {
       return true;
    }

    @Override
    public boolean isDevicePoolFull(DeviceId deviceId) {
       return false;
    }

    @Override
    public boolean isGlobalPoolFull() {
       return false;
    }

    @Override
    public long getFreeNumOfDevicePool(DeviceId deviceId) {
       return 4;
    }

    @Override
    public long getFreeNumOfGlobalPool() {
       return 4;
    }

    @Override
    public LabelResourcePool getDeviceLabelResourcePool(DeviceId deviceId) {
       return null;
    }

    @Override
    public LabelResourcePool getGlobalLabelResourcePool() {
       return null;
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
        return null;
    }
}
