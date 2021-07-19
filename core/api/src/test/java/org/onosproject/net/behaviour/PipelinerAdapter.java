/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.behaviour;

import java.util.List;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;

/**
 * Testing adapter for pipeliner class.
 */
public class PipelinerAdapter implements Pipeliner {
    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {

    }

    @Override
    public void filter(FilteringObjective filterObjective) {

    }

    @Override
    public void forward(ForwardingObjective forwardObjective) {

    }

    @Override
    public void next(NextObjective nextObjective) {

    }

    @Override
    public void purgeAll(ApplicationId appId) {

    }

    @Override
    public DriverHandler handler() {
        return null;
    }

    @Override
    public void setHandler(DriverHandler handler) {

    }

    @Override
    public DriverData data() {
        return null;
    }

    @Override
    public void setData(DriverData data) {

    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        return null;
    }
}
