/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import org.openstack4j.model.network.RouterInterface;

/**
 * Test implementation class of router interface.
 */
public final class TestRouterInterface implements RouterInterface {
    private final String id;
    private final String subnetId;
    private final String portId;
    private final String tenantId;

    public TestRouterInterface(String id, String subnetId,
                               String portId, String tenantId) {
        this.id = id;
        this.subnetId = subnetId;
        this.portId = portId;
        this.tenantId = tenantId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getSubnetId() {
        return subnetId;
    }

    @Override
    public String getPortId() {
        return portId;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }
}
