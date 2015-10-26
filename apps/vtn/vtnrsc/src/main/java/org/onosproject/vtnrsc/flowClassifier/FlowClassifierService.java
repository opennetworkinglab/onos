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
package org.onosproject.vtnrsc.flowClassifier;

import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;

/**
 * Provides Services for Flow Classifier.
 */
public interface FlowClassifierService {

    /**
     * Store Flow Classifier.
     *
     * @param flowClassifier Flow Classifier
     * @return true if adding Flow Classifier into store is success otherwise return false.
     */
    boolean createFlowClassifier(FlowClassifier flowClassifier);

    /**
     * Return the existing collection of Flow Classifier.
     *
     * @return Flow Classifier collections.
     */
    Iterable<FlowClassifier> getFlowClassifiers();

    /**
     * Check whether Flow Classifier is present based on given Flow Classifier Id.
     *
     * @param id Flow Classifier.
     * @return true if Flow Classifier is present otherwise return false.
     */
    boolean hasFlowClassifier(FlowClassifierId id);

    /**
     * Retrieve the Flow Classifier based on given Flow Classifier id.
     *
     * @param id Flow Classifier Id.
     * @return Flow Classifier if present otherwise returns null.
     */
    FlowClassifier getFlowClassifier(FlowClassifierId id);

    /**
     * Update Flow Classifier based on given Flow Classifier Id.
     *
     * @param flowClassifier Flow Classifier.
     * @return true if update is success otherwise return false.
     */
    boolean updateFlowClassifier(FlowClassifier flowClassifier);

    /**
     * Remove Flow Classifier from store based on given Flow Classifier Id.
     *
     * @param id Flow Classifier Id.
     * @return true if Flow Classifier removal is success otherwise return false.
     */
    boolean removeFlowClassifier(FlowClassifierId id);
}