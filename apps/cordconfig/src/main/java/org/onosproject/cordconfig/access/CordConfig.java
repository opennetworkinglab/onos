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

package org.onosproject.cordconfig.access;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages the common CORD configuration.
 */
@Service
@Component(immediate = true)
public class CordConfig implements CordConfigService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkConfig;

    private Map<DeviceId, AccessDeviceData> accessDevices = new ConcurrentHashMap<>();
    private Map<DeviceId, AccessAgentData> accessAgents = new ConcurrentHashMap<>();

    private static final Class<AccessDeviceConfig> ACCESS_DEVICE_CONFIG_CLASS =
            AccessDeviceConfig.class;
    private static final String ACCESS_DEVICE_CONFIG_KEY = "accessDevice";

    private ConfigFactory<DeviceId, AccessDeviceConfig> deviceConfigFactory =
            new ConfigFactory<DeviceId, AccessDeviceConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    ACCESS_DEVICE_CONFIG_CLASS, ACCESS_DEVICE_CONFIG_KEY) {
                @Override
                public AccessDeviceConfig createConfig() {
                    return new AccessDeviceConfig();
                }
            };

    private static final Class<AccessAgentConfig> ACCESS_AGENT_CONFIG_CLASS =
            AccessAgentConfig.class;
    private static final String ACCESS_AGENT_CONFIG_KEY = "accessAgent";

    private ConfigFactory<DeviceId, AccessAgentConfig> agentConfigFactory =
            new ConfigFactory<DeviceId, AccessAgentConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    ACCESS_AGENT_CONFIG_CLASS, ACCESS_AGENT_CONFIG_KEY) {
                @Override
                public AccessAgentConfig createConfig() {
                    return new AccessAgentConfig();
                }
            };

    private InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();

    @Activate
    protected void activate() {
        networkConfig.registerConfigFactory(deviceConfigFactory);
        networkConfig.registerConfigFactory(agentConfigFactory);

        networkConfig.addListener(configListener);

        networkConfig.getSubjects(DeviceId.class, AccessDeviceConfig.class)
                .forEach(this::addAccessDeviceConfig);

        networkConfig.getSubjects(DeviceId.class, AccessAgentConfig.class)
                .forEach(this::addAccessAgentConfig);
    }

    @Deactivate
    protected void deactivate() {
        networkConfig.unregisterConfigFactory(deviceConfigFactory);
        networkConfig.unregisterConfigFactory(agentConfigFactory);
    }

    private void addAccessDeviceConfig(DeviceId subject) {
        AccessDeviceConfig config =
                networkConfig.getConfig(subject, ACCESS_DEVICE_CONFIG_CLASS);
        if (config != null) {
            addAccessDevice(config);
        }
    }

    private void addAccessDevice(AccessDeviceConfig config) {
        AccessDeviceData accessDevice = config.getAccessDevice();
        accessDevices.put(accessDevice.deviceId(), accessDevice);
    }

    private void removeAccessDeviceConfig(DeviceId subject) {
        accessDevices.remove(subject);
    }

    private void addAccessAgentConfig(DeviceId subject) {
        AccessAgentConfig config =
                networkConfig.getConfig(subject, ACCESS_AGENT_CONFIG_CLASS);
        if (config != null) {
            addAccessAgent(config);
        }
    }

    private void addAccessAgent(AccessAgentConfig config) {
        AccessAgentData accessAgent = config.getAgent();
        accessAgents.put(accessAgent.deviceId(), accessAgent);
    }

    private void removeAccessAgentConfig(DeviceId subject) {
        accessAgents.remove(subject);
    }

    @Override
    public Set<AccessDeviceData> getAccessDevices() {
        return ImmutableSet.copyOf(accessDevices.values());
    }

    @Override
    public Optional<AccessDeviceData> getAccessDevice(DeviceId deviceId) {
        checkNotNull(deviceId, "Device ID cannot be null");
        return Optional.ofNullable(accessDevices.get(deviceId));
    }

    @Override
    public Set<AccessAgentData> getAccessAgents() {
        return ImmutableSet.copyOf(accessAgents.values());
    }

    @Override
    public Optional<AccessAgentData> getAccessAgent(DeviceId deviceId) {
        checkNotNull(deviceId, "Device ID cannot be null");
        return Optional.ofNullable(accessAgents.get(deviceId));
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {
            case CONFIG_ADDED:
            case CONFIG_UPDATED:
                if (event.configClass().equals(ACCESS_DEVICE_CONFIG_CLASS)) {
                    addAccessDeviceConfig((DeviceId) event.subject());
                } else if (event.configClass().equals(ACCESS_AGENT_CONFIG_CLASS)) {
                    addAccessAgentConfig((DeviceId) event.subject());
                }
                break;
            case CONFIG_REMOVED:
                if (event.configClass().equals(ACCESS_DEVICE_CONFIG_CLASS)) {
                    removeAccessDeviceConfig((DeviceId) event.subject());
                } else if (event.configClass().equals(ACCESS_AGENT_CONFIG_CLASS)) {
                    removeAccessAgentConfig((DeviceId) event.subject());
                }
                break;
            case CONFIG_REGISTERED:
            case CONFIG_UNREGISTERED:
            default:
                break;
            }
        }
    }
}
