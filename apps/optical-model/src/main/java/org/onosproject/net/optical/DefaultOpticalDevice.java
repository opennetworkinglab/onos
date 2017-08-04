/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.optical;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Optional;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractBehaviour;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.optical.device.port.OchPortMapper;
import org.onosproject.net.optical.device.port.OduCltPortMapper;
import org.onosproject.net.optical.device.port.OmsPortMapper;
import org.onosproject.net.optical.device.port.OtuPortMapper;
import org.onosproject.net.optical.device.port.PortMapper;
import org.onosproject.net.utils.ForwardingDevice;
import org.slf4j.Logger;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

/**
 * Implementation of {@link OpticalDevice}.
 * <p>
 * Currently supports
 * <ul>
 *  <li> {@link OchPort}
 *  <li> {@link OmsPort}
 *  <li> {@link OduCltPort}
 *  <li> {@link OtuPort}
 * </ul>
 */
@Beta
public class DefaultOpticalDevice
        extends AbstractBehaviour
        implements OpticalDevice, ForwardingDevice {

    private static final Logger log = getLogger(DefaultOpticalDevice.class);

    // shared Port type handler map.
    // TODO Is there a use case, where we need to differentiate this map per Device?
    private static final Map<Class<? extends Port>, PortMapper<? extends Port>> MAPPERS
        = ImmutableMap.<Class<? extends Port>, PortMapper<? extends Port>>builder()
            .put(OchPort.class, new OchPortMapper())
            .put(OmsPort.class, new OmsPortMapper())
            .put(OduCltPort.class, new OduCltPortMapper())
            .put(OtuPort.class, new OtuPortMapper())
            // TODO add other optical port type here
            .build();



    // effectively final
    private Device delegate;

    // Default constructor required as a Behaviour.
    public DefaultOpticalDevice() {}

    @Override
    public Device delegate() {
        if (delegate == null) {
            // dirty work around.
            // wanted to pass delegate Device at construction,
            // but was not possible. A Behaviour requires no-arg constructor.
            checkState(data() != null, "DriverData must exist");
            DriverData data = data();
            DeviceService service = DefaultServiceDirectory.getService(DeviceService.class);
            delegate = checkNotNull(service.getDevice(data.deviceId()),
                                    "No Device found for %s", data.deviceId());
        }
        return delegate;
    }

    @Override
    public <T extends Port> boolean portIs(Port port, Class<T> portClass) {

        PortMapper<? extends Port> mapper = MAPPERS.get(portClass);
        if (mapper != null) {
            return mapper.is(port);
        }
        return false;
    }

    @Override
    public <T extends Port> Optional<T> portAs(Port port, Class<T> portClass) {
        PortMapper<? extends Port> mapper = MAPPERS.get(portClass);
        if (mapper != null) {
            return (Optional<T>) (mapper.as(port));
        }
        return Optional.empty();
    }

    @Override
    public Port port(Port port) {
        for (PortMapper<? extends Port> mapper : MAPPERS.values()) {
            if (mapper.is(port)) {
                return mapper.as(port).map(Port.class::cast).orElse(port);
            }
        }
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        return delegate().equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("delegate", delegate)
                .toString();
    }
}
