/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.incubator.net.domain;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.config.Config;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Configuration for an intent domain including a name, set of internal devices,
 * set of edge ports, and the application bound to control the domain.
 */
@Beta
public class IntentDomainConfig extends Config<IntentDomainId> {

    private static final String DOMAIN_NAME = "name";
    private static final String APPLICATION_NAME = "applicationName";
    private static final String INTERNAL_DEVICES = "internalDevices";
    private static final String EDGE_PORTS = "edgePorts";


    /**
     * Returns the friendly name for the domain.
     *
     * @return domain name
     */
    public String domainName() {
        return get(DOMAIN_NAME, subject.toString());
    }

    /**
     * Sets the friendly name for the domain.
     *
     * @param domainName new name for the domain; null to clear
     * @return self
     */
    public IntentDomainConfig domainName(String domainName) {
        return (IntentDomainConfig) setOrClear(DOMAIN_NAME, domainName);
    }

    /**
     * Returns the friendly name for the domain.
     *
     * @return domain name
     */
    public String applicationName() {
        return get(APPLICATION_NAME, "FIXME"); //TODO maybe not null?
    }

    /**
     * Sets the friendly name for the domain.
     *
     * @param applicationName new name for the domain; null to clear
     * @return self
     */
    public IntentDomainConfig applicationName(String applicationName) {
        return (IntentDomainConfig) setOrClear(APPLICATION_NAME, applicationName);
    }

    /**
     * Returns the set of internal devices.
     *
     * @return set of internal devices
     */
    public Set<DeviceId> internalDevices() {
        return ImmutableSet.copyOf(getList(INTERNAL_DEVICES, DeviceId::deviceId));
    }

    /**
     * Sets the set of internal devices.
     *
     * @param devices set of devices; null to clear
     * @return self
     */
    public IntentDomainConfig internalDevices(Set<DeviceId> devices) {
        return (IntentDomainConfig) setOrClear(INTERNAL_DEVICES, devices);
    }

    /**
     * Returns the set of edge ports.
     *
     * @return set of edge ports
     */
    public Set<ConnectPoint> edgePorts() {
        return ImmutableSet.copyOf(getList(EDGE_PORTS, ConnectPoint::deviceConnectPoint));
    }

    /**
     * Sets the set of edge ports.
     *
     * @param connectPoints set of edge ports; null to clear
     * @return self
     */
    public IntentDomainConfig edgePorts(Set<ConnectPoint> connectPoints) {
        return (IntentDomainConfig) setOrClear(EDGE_PORTS, connectPoints);
    }

}
