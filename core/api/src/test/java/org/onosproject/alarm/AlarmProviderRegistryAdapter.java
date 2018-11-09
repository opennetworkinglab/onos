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

package org.onosproject.alarm;

import org.onosproject.net.provider.ProviderId;

import java.util.Set;

/**
 * Adapter for Alarm Provider Registry.
 */
public class AlarmProviderRegistryAdapter implements AlarmProviderRegistry {

    @Override
    public AlarmProviderService register(AlarmProvider provider) {
        return null;
    }

    @Override
    public void unregister(AlarmProvider provider) {

    }

    @Override
    public Set<ProviderId> getProviders() {
        return null;
    }
}
