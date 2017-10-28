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
package org.onosproject.net.optical.device;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.optical.OpticalDevice;
import org.onosproject.net.utils.ForwardingDeviceService;
import org.slf4j.Logger;

import com.google.common.annotations.Beta;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Element;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;


// TODO replace places using DeviceService expecting Optical specific ports.
// with this

/**
 * Decorator, which provides a DeviceService view, which returns
 * Ports in optical specific ports.
 */
@Beta
public class OpticalDeviceServiceView
    extends ForwardingDeviceService
    implements DeviceService {

    private static final Logger log = getLogger(OpticalDeviceServiceView.class);

    /**
     * DeviceListener to wrapped DeviceListener map.
     * <p>
     * {@literal original listener -> wrapped listener}
     */
    private final Map<DeviceListener, DeviceListener> wrapped = Maps.newIdentityHashMap();

    // May need a way to monitor Drivers loaded on ONOS and
    // invalidate this Cache if a driver was added/updated
    /**
     * Device to {@link OpticalDevice} map cache.
     */
    private final LoadingCache<Element, Optional<OpticalDevice>> optdev
        = CacheBuilder.newBuilder()
            .weakKeys() // == for Key comparison
            .maximumSize(100)
            .build(CacheLoader.from(elm -> elm.project(OpticalDevice.class)));

    // Not intended to be instantiated directly
    protected OpticalDeviceServiceView(DeviceService base) {
        super(base);
    }

    /**
     * Wraps the given DeviceService to provide a view,
     * which returns port as optical specific Port class.
     *
     * @param base {@link DeviceService} view to use as baseline.
     * @return Decorated view of {@code base}
     */
    public static OpticalDeviceServiceView opticalView(DeviceService base) {
        // TODO might make sense to track and assign an instance for each `base`
        return new OpticalDeviceServiceView(base);
    }

    /**
     * Transform Port instance on the event to Optical specific port, if it is well-formed.
     *
     * @param event original event to transform
     * @return transformed {@link DeviceEvent}
     */
    public DeviceEvent augment(DeviceEvent event) {
        final Port port = augment(event.port());
        if (port == event.port()) {
            // If the Port not changed, pass through
            return event;
        }
        return new DeviceEvent(event.type(), event.subject(), port, event.time());
    }

    /**
     * Transform Port instance to Optical specific port, if it is well-formed.
     *
     * @param port Port instance to translate
     * @return Optical specific port instance or original {@code port}.
     */
    public Port augment(Port port) {
        if (port == null) {
            return null;
        }
        return optdev.getUnchecked(port.element())
            .map(odev -> odev.port(port))
            .orElse(port);
    }

    @Override
    public void addListener(DeviceListener listener) {
        super.addListener(wrapped.computeIfAbsent(listener, OpticalDeviceListener::new));
    }

    @Override
    public void removeListener(DeviceListener listener) {
        DeviceListener wrappedListener = wrapped.remove(listener);
        if (wrappedListener != null) {
            super.removeListener(wrappedListener);
        }
    }


    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        return Lists.transform(super.getPorts(deviceId),
                               this::augment);
    }

    @Override
    public PortStatistics getStatisticsForPort(DeviceId deviceId, PortNumber portNumber) {
        return null;
    }

    @Override
    public PortStatistics getDeltaStatisticsForPort(DeviceId deviceId, PortNumber portNumber) {
        return null;
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        return augment(super.getPort(deviceId, portNumber));
    }


    /**
     * DeviceListener, which translates generic Port to optical specific Port
     * before passing.
     */
    class OpticalDeviceListener implements DeviceListener {

        private final DeviceListener listener;

        // shallow cache to reuse transformed event in isRelevant and event call
        private Pair<DeviceEvent, DeviceEvent> cache;

        public OpticalDeviceListener(DeviceListener listener) {
            this.listener = listener;
        }

        private DeviceEvent opticalEvent(DeviceEvent event) {

            Pair<DeviceEvent, DeviceEvent> entry = cache;
            if (entry != null && entry.getLeft() == event) {
                return entry.getRight();
            }

            DeviceEvent opticalEvent = augment(event);
            cache = Pair.of(event, opticalEvent);
            return opticalEvent;
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return listener.isRelevant(opticalEvent(event));
        }

        @Override
        public void event(DeviceEvent event) {
            listener.event(opticalEvent(event));
        }
    }

}
