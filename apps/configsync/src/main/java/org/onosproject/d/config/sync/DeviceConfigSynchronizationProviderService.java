/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.d.config.sync;

import org.onosproject.net.provider.ProviderService;

import com.google.common.annotations.Beta;

/**
 * Service which configuration synchronization provider can interact
 * with the service.
 * <p>
 * Provides a mean to propagate device triggered change event upward to
 * dynamic config subsystem.
 */
@Beta
public interface DeviceConfigSynchronizationProviderService
    extends ProviderService<DeviceConfigSynchronizationProvider> {

    // TODO API to propagate device detected change upwards
    // e.g., in reaction to NETCONF async notification,
    //       report polling result up to DynConfig subsystem
}
