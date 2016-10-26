/*
 * Copyright 2017-present Open Networking Laboratory
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
import com.google.common.annotations.Beta;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Collection;

/**
 * Behaviour for handling various operations for qos configurations.
 */
@Beta
public interface QosConfigBehaviour extends HandlerBehaviour {

    /**
     * Obtain all qoses configured on a device.
     *
     * @return a set of qos descriptions
     */
    Collection<QosDescription> getQoses();

    /**
     * Obtain a qos configured on a device.
     * @param qosDesc qos description
     * @return a qos description
     */
    QosDescription getQos(QosDescription qosDesc);

    /**
     * create QoS configuration on a device.
     * @param qosDesc qos description
     * @return true if succeeds, or false
     */
    boolean addQoS(QosDescription qosDesc);

    /**
     * Delete a QoS configuration.
     * @param qosId qos identifier
     */
    void deleteQoS(QosId qosId);
}