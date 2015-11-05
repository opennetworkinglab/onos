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
package org.onosproject.openflow;

import java.util.Map;
import java.util.Set;

import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.openflow.controller.driver.OpenFlowSwitchDriver;

/**
 * Created by ray on 11/4/15.
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

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Behaviour> T createBehaviour(DriverHandler handler, Class<T> behaviourClass) {
        if (behaviourClass == OpenFlowSwitchDriver.class) {
            return (T) new OpenflowSwitchDriverAdapter();
        }
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
