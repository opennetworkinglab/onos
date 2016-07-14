/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Representation of an intent domain which includes the set of internal devices,
 * the set of edge ports, and the implementation of the domain provider.
 */
@Beta
public class IntentDomain {

    private final IntentDomainId id;
    private String name;

    private Set<DeviceId> internalDevices;
    private Set<ConnectPoint> edgePorts;

    private IntentDomainProvider provider;

    public IntentDomain(IntentDomainId id, String name,
                 Set<DeviceId> internalDevices,
                 Set<ConnectPoint> edgePorts) {
        this.id = id;
        this.name = name;
        this.internalDevices = internalDevices;
        this.edgePorts = edgePorts;
    }

    /**
     * Returns the id for the intent domain.
     *
     * @return intent domain id
     */
    public IntentDomainId id() {
        return id;
    }

    /**
     * Returns the friendly name for the intent domain.
     *
     * @return intent domain name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the set of internal devices for the intent domain (devices under
     * exclusive control of the intent domain).
     *
     * @return set of internal devices
     */
    public Set<DeviceId> internalDevices() {
        return internalDevices;
    }

    /**
     * Returns the set of edge ports for the intent domain.
     *
     * @return set of edge ports
     */
    public Set<ConnectPoint> edgePorts() {
        return edgePorts;
    }

    /**
     * Returns the provider for the intent domain.
     *
     * @return intent domain provider
     */
    public IntentDomainProvider provider() {
        return provider;
    }

    /**
     * Returns the status of the intent domain. An intent domain is active if it
     * has an intent domain provider bound, and it is inactive if one is not bound.
     *
     * @return true if active; false otherwise
     */
    public boolean isActive() {
        return provider != null;
    }

    /**
     * Sets the provider for the intent domain if one is not already set.
     *
     * @param provider new intent domain provider
     */
    public void setProvider(IntentDomainProvider provider) {
        // TODO consider checkState depending on caller
        if (this.provider == null) {
            this.provider = provider;
        }
    }

    /**
     * Unsets the provider for the intent domain.
     */
    public void unsetProvider() {
        this.provider = null;
    }

    //TODO add remaining setters (we will probably want to link this to the network config)
}
