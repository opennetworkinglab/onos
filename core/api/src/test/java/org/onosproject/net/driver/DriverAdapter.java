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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test adapter for the driver class.
 */
public class DriverAdapter implements Driver {
    @Override
    public String name() {
        return null;
    }

    @Override
    public Driver parent() {
        return null;
    }

    @Override
    public List<Driver> parents() {
        return null;
    }

    @Override
    public String manufacturer() {
        return null;
    }

    @Override
    public String hwVersion() {
        return null;
    }

    @Override
    public String swVersion() {
        return null;
    }

    @Override
    public Set<Class<? extends Behaviour>> behaviours() {
        return null;
    }

    @Override
    public Class<? extends Behaviour> implementation(Class<? extends Behaviour> behaviour) {
        return null;
    }

    @Override
    public boolean hasBehaviour(Class<? extends Behaviour> behaviourClass) {
        return true;
    }

    @Override
    public <T extends Behaviour> T createBehaviour(DriverData data, Class<T> behaviourClass) {
        return null;
    }

    @Override
    public <T extends Behaviour> T createBehaviour(DriverHandler handler, Class<T> behaviourClass) {
        return null;
    }

    @Override
    public Map<String, String> properties() {
        return null;
    }

    @Override
    public Driver merge(Driver other) {
        return null;
    }

    @Override
    public Set<String> keys() {
        return null;
    }

    @Override
    public String value(String key) {
        return null;
    }
}
