/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.flowclassifier;

import org.onosproject.event.ListenerService;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;

/**
 * Provides Services for Flow Classifier.
 */
public interface FlowClassifierService extends ListenerService<FlowClassifierEvent, FlowClassifierListener> {

    /**
     * Check whether Flow Classifier is present based on given Flow Classifier
     * Id.
     *
     * @param id flow classifier identifier
     * @return true if flow classifier is present otherwise return false
     */
    boolean exists(FlowClassifierId id);

    /**
     * Returns the number of flow classifiers known to the system.
     *
     * @return number of flow classifiers
     */
    int getFlowClassifierCount();

    /**
     * Store Flow Classifier.
     *
     * @param flowClassifier flow classifier
     * @return true if adding flow classifier into store is success otherwise
     *         return false
     */
    boolean createFlowClassifier(FlowClassifier flowClassifier);

    /**
     * Return the existing collection of Flow Classifier.
     *
     * @return flow classifier collections
     */
    Iterable<FlowClassifier> getFlowClassifiers();

    /**
     * Retrieve the Flow Classifier based on given Flow Classifier id.
     *
     * @param id flow classifier identifier
     * @return flow classifier if present otherwise returns null
     */
    FlowClassifier getFlowClassifier(FlowClassifierId id);

    /**
     * Update Flow Classifier based on given Flow Classifier Id.
     *
     * @param flowClassifier flow classifier
     * @return true if flow classifier update is success otherwise return false
     */
    boolean updateFlowClassifier(FlowClassifier flowClassifier);

    /**
     * Remove Flow Classifier from store based on given Flow Classifier Id.
     *
     * @param id flow classifier identifier
     * @return true if flow classifier removal is success otherwise return
     *         false
     */
    boolean removeFlowClassifier(FlowClassifierId id);
}
