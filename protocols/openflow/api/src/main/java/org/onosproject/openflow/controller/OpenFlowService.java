/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openflow.controller;

import java.util.Set;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;

/**
 * Abstraction of an OpenFlow classifiers management.
 */
public interface OpenFlowService extends ListenerService<OpenFlowEvent, OpenFlowListener> {

    /**
     * Adds the OpenFlow classifier to the information base.
     *
     * @param classifier the OpenFlow classifier
     */
    void add(OpenFlowClassifier classifier);

    /**
     * Removes the OpenFlow classifier from the information base.
     *
     * @param classifier classifier to remove
     */
    void remove(OpenFlowClassifier classifier);

    /**
     * Gets all OpenFlow classifiers in the system.
     *
     * @return set of OpenFlow classifiers
     */
    Set<OpenFlowClassifier> getClassifiers();

    /**
     * Obtains OpenFlow classifiers in the system by device id.
     *
     * @param deviceId the device id
     * @return set of OpenFlow classifiers
     */
    Set<OpenFlowClassifier> getClassifiersByDeviceId(DeviceId deviceId);

    /**
     * Obtains OpenFlow classifiers in the system by device id and number of queue.
     *
     * @param deviceId the device id
     * @param idQueue the queue id
     * @return set of OpenFlow classifiers
     */
    Set<OpenFlowClassifier> getClassifiersByDeviceIdAndQueue(DeviceId deviceId, int idQueue);
}
