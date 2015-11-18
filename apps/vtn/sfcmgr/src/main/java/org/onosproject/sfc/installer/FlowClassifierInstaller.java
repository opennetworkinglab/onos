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
package org.onosproject.sfc.installer;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.PortPair;

/**
 * Abstraction of an entity which installs flow classification rules in ovs.
 */
public interface FlowClassifierInstaller {

    /**
     * Install flow classifier rules.
     *
     * @param flowClassifier Flow Classifier
     * @param portPair Port pair
     */
    void install(FlowClassifier flowClassifier, PortPair portPair);

    /**
     * Programs forwarding object for flow classifier.
     *
     * @param flowClassifier flow classifier
     * @param portPair port pair
     * @param appid application id
     * @param type forwarding objective operation type
     */
    void programFlowClassification(FlowClassifier flowClassifier, PortPair portPair, ApplicationId appid,
            Objective.Operation type);
}
