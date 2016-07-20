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

import org.onosproject.event.AbstractEvent;
import org.onosproject.vtnrsc.FlowClassifier;

/**
 * Describes network Flow-Classifier event.
 */
public class FlowClassifierEvent extends AbstractEvent<FlowClassifierEvent.Type, FlowClassifier> {
    /**
     * Type of flow-classifier events.
     */
    public enum Type {
        /**
         * Signifies that flow-classifier has been created.
         */
        FLOW_CLASSIFIER_PUT,
        /**
         * Signifies that flow-classifier has been deleted.
         */
        FLOW_CLASSIFIER_DELETE,
        /**
         * Signifies that flow-classifier has been updated.
         */
        FLOW_CLASSIFIER_UPDATE
    }

    /**
     * Creates an event of a given type and for the specified Flow-Classifier.
     *
     * @param type Flow-Classifier event type
     * @param flowClassifier Flow-Classifier subject
     */
    public FlowClassifierEvent(Type type, FlowClassifier flowClassifier) {
        super(type, flowClassifier);
    }

    /**
     * Creates an event of a given type and for the specified Flow-Classifier.
     *
     * @param type Flow-Classifier event type
     * @param flowClassifier Flow-Classifier subject
     * @param time occurrence time
     */
    public FlowClassifierEvent(Type type, FlowClassifier flowClassifier, long time) {
        super(type, flowClassifier, time);
    }
}
