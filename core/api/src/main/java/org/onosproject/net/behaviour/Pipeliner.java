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
package org.onosproject.net.behaviour;

import org.onlab.osgi.ServiceDirectory;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;

import java.util.Collection;
import java.util.concurrent.Future;

/**
 * Behaviour for handling various pipelines.
 */
public interface Pipeliner {

    /**
     * Injecting the service directory into the driver.
     *
     * @param deviceId the deviceId
     * @param serviceDirectory the service directory.
     */
    void init(DeviceId deviceId, ServiceDirectory serviceDirectory);

    /**
     * Installs the filtering rules onto the device.
     *
     * @param filters the collection of filters
     * @return a future indicating the success of the operation
     */
     Future<Boolean> filter(Collection<FilteringObjective> filters);

    /**
     * Installs the forwarding rules onto the device.
     *
     * @param forwardings the collection of forwarding objectives
     * @return a future indicating the success of the operation
     */
    Future<Boolean> forward(Collection<ForwardingObjective> forwardings);

}
