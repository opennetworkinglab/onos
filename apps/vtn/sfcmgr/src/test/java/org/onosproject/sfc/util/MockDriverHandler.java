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

package org.onosproject.sfc.util;

import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;

public class MockDriverHandler implements DriverHandler {

    @Override
    public Driver driver() {
        return null;
    }

    @Override
    public DriverData data() {
        return null;
    }

    @Override
    public <T extends Behaviour> T behaviour(Class<T> behaviourClass) {
        if (behaviourClass == ExtensionSelectorResolver.class) {
            return (T) new MockExtensionSelectorResolver();
        } else if (behaviourClass == ExtensionTreatmentResolver.class) {
            return (T) new MockExtensionTreatmentResolver();
        }
        return null;
    }

    @Override
    public <T> T get(Class<T> serviceClass) {
        return null;
    }
}