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
package org.onosproject.net.driver;

import org.junit.Test;

import java.util.Set;

/**
 * Base test class for driver loading.
 */
public abstract class AbstractDriverLoaderTest {

    private class DriverAdminServiceAdapter extends DriverServiceAdapter implements DriverAdminService {
        @Override
        public Set<DriverProvider> getProviders() {
            return null;
        }

        @Override
        public void registerProvider(DriverProvider provider) {
        }

        @Override
        public void unregisterProvider(DriverProvider provider) {
        }

        @Override
        public Class<? extends Behaviour> getBehaviourClass(String className) {
            return null;
        }
    }

    protected AbstractDriverLoader loader;

    @Test
    public void testLoader() {
        loader.driverAdminService = new DriverAdminServiceAdapter();
        loader.activate();
        loader.deactivate();
    }
}
