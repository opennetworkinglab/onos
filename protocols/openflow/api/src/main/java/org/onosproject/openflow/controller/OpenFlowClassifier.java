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

import org.onosproject.net.DeviceId;
import java.util.Objects;

/**
 * Represents to OpenFlow messages classifier.
 */
public final class OpenFlowClassifier {

    private final short ethernetType;
    private final int idQueue;
    private final DeviceId deviceId;

    /**
     * Builder of the OpenFlow classifier.
     */
    public static class Builder {
        private Short ethernetType = 0;
        private int idQueue;
        private DeviceId deviceId;

        /**
         * Builder constructor for OpenFlow classifier.
         *
         * @param deviceId the device id
         * @param idQueue  the queue id
         */
        public Builder(DeviceId deviceId, int idQueue) {
            this.deviceId = deviceId;
            this.idQueue = idQueue;
        }

        /**
         * Sets the ethernet type for the OpenFlow classifier that will be built.
         *
         * @param ethernetType the ethernet type
         * @return this builder
         */
        public Builder ethernetType(short ethernetType) {
            this.ethernetType = ethernetType;
            return this;
        }

        /**
         * Builds the OpenFlow classifier from the accumulated parameters.
         *
         * @return OpenFlow classifier instance
         */
        public OpenFlowClassifier build() {
            return new OpenFlowClassifier(this);
        }
    }

    private OpenFlowClassifier(Builder builder) {
        this.idQueue = builder.idQueue;
        this.ethernetType = builder.ethernetType;
        this.deviceId = builder.deviceId;
    }

    /**
     * Gets the ethernet type matched by the classifier.
     *
     * @return matched packet ethernet type
     */
    public short ethernetType() {
        return this.ethernetType;
    }

    /**
     * Gets the id of source OpenFlow device matched by the classifier.
     *
     * @return connected device id
     */
    public DeviceId deviceId() {
        return this.deviceId;
    }

    /**
     * Gets the queue id targeted by the classifier.
     *
     * @return target queue id
     */
    public int idQueue() {
        return this.idQueue;
    }

    /**
     * Compares OpenFlow classifiers.
     *
     * @param o object that we want to compare to
     * @return equality check result
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OpenFlowClassifier)) {
            return false;
        }
        OpenFlowClassifier classifier = (OpenFlowClassifier) o;
        return this.ethernetType == classifier.ethernetType()
               && this.idQueue == classifier.idQueue()
               && this.deviceId.equals(classifier.deviceId());
    }

    /**
     * Calculates hashCode of the OpenFlow Classifier object.
     *
     * @return hash of the OpenFlow Classifier
     */
    @Override
    public int hashCode() {
        return Objects.hash(deviceId, idQueue, ethernetType);
    }
}
