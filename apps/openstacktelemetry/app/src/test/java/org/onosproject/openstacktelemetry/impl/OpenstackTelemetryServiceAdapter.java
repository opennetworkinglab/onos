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
package org.onosproject.openstacktelemetry.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.TelemetryAdminService;

import java.util.Set;

/**
 * A mock up test class of openstack telemetry service.
 */
public class OpenstackTelemetryServiceAdapter implements OpenstackTelemetryService {

    Set<TelemetryAdminService> services = Sets.newConcurrentHashSet();

    @Override
    public void addTelemetryService(TelemetryAdminService telemetryService) {
        services.add(telemetryService);
    }

    @Override
    public void removeTelemetryService(TelemetryAdminService telemetryService) {
        services.remove(telemetryService);
    }

    @Override
    public void publish(Set<FlowInfo> flowInfos) {

    }

    @Override
    public Set<TelemetryAdminService> telemetryServices() {
        return ImmutableSet.copyOf(services);
    }

    @Override
    public TelemetryAdminService telemetryService(String type) {
        return null;
    }
}
