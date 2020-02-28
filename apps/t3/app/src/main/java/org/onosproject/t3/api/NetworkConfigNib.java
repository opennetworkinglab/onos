/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.t3.api;

import com.google.common.collect.ImmutableMap;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.basics.InterfaceConfig;
import org.onosproject.segmentrouting.config.SegmentRoutingDeviceConfig;
import org.slf4j.Logger;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Represents Network Information Base (NIB) for network configurations
 * and supports alternative functions to
 * {@link org.onosproject.net.config.NetworkConfigService} for offline data.
 */
public class NetworkConfigNib extends AbstractNib {

    private static final Logger log = getLogger(NetworkConfigNib.class);

    // Map of str ConnectPoint : InterfaceConfig
    private Map<String, Config> portConfigMap;
    // Map of str DeviceId : SegmentRoutingDeviceConfig
    private Map<String, Config> deviceConfigMap;

    // use the singleton helper to create the instance
    protected NetworkConfigNib() {
    }

    /**
     * Sets a map of port : configuration to the port.
     *
     * @param portConfigMap port-config map
     */
    public void setPortConfigMap(Map<String, Config> portConfigMap) {
         this.portConfigMap = portConfigMap;
    }

    /**
     * Sets a map of device : configuration to the device.
     *
     * @param deviceConfigMap device-config map
     */
    public void setDeviceConfigMap(Map<String, Config> deviceConfigMap) {
        this.deviceConfigMap = deviceConfigMap;
    }

    /**
     * Returns the port-config map.
     *
     * @return port-config map
     */
    public Map<Object, Object> getPortConfigMap() {
        return ImmutableMap.copyOf(portConfigMap);
    }

    /**
     * Returns the device-config map.
     *
     * @return device-config map
     */
    public Map<Object, Object> getDeviceConfigMap() {
        return ImmutableMap.copyOf(deviceConfigMap);
    }

    /**
     * Returns the configuration for the specified subject and configuration
     * class if one is available; null otherwise.
     *
     * @param subject     configuration subject
     * @param configClass configuration class
     * @param <S>         type of subject
     * @param <C>         type of configuration
     * @return configuration or null if one is not available
     */
    public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
        if (configClass.equals(InterfaceConfig.class)) {
            return (C) portConfigMap.get(subject.toString());
        } else if (configClass.equals(SegmentRoutingDeviceConfig.class)) {
            return (C) deviceConfigMap.get(subject.toString());
        } else {
            log.warn("Given configuration {} is not supported", configClass.toString());
            return null;
        }
    }

    /**
     * Returns the singleton instance of multicast routes NIB.
     *
     * @return instance of multicast routes NIB
     */
    public static NetworkConfigNib getInstance() {
        return NetworkConfigNib.SingletonHelper.INSTANCE;
    }

    private static class SingletonHelper {
        private static final NetworkConfigNib INSTANCE = new NetworkConfigNib();
    }

}
