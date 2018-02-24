/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.cfm;

import org.onlab.packet.MacAddress;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;

import java.util.Optional;

/**
 * Grouping of parameters used to create a Test Signal test on a MEP.
 */
public interface MepTsCreate {
    /**
     * The remote Mep will be identified by either a MacAddress or a MEPId.
     * @return The MAC address of the remoteMep
     */
    MacAddress remoteMepAddress();

    /**
     * The remote Mep will be identified by either a MacAddress or a MEPId.
     * @return The id of the remoteMep
     */
    MepId remoteMepId();

    /**
     * Indicates the MEP is acting in the role of a receiver.
     * True by default
     * @return true if MEP is acting as a receiver
     */
    Optional<Boolean> isReceiver();

    /**
     * Indicates the MEP is acting in the role of a generator.
     * False by default
     * @return true if MEP is acting as a generator
     */
    Optional<Boolean> isGenerator();

    /**
     * This attribute specifies the type of ETH-Test to perform whether it is service interrupting or not.
     * An 'in-service' value indicates that the ETH-Test is in service and normal
     * client service traffic is not interrupted. A 'out-of-service' value
     * indicates that the ETH-Test is out of service and normal client service
     * traffic is disrupted.
     * The test-type parameter is only relevant for the generator side
     * @return test type enumeration
     */
    Optional<TestType> testType();

    /**
     * Builder for {@link MepTsCreate}.
     */
    interface MepTsCreateBuilder {
        MepTsCreateBuilder isReceiver(Optional<Boolean> isReceiver);

        MepTsCreateBuilder isGenerator(Optional<Boolean> isGenerator);

        MepTsCreateBuilder testType(Optional<TestType> testType);

        MepTsCreateBuilder build();
    }

    /**
     * Types of Test Signal test.
     */
    public enum TestType {
        /**
         * Indicates the ETH-Test is in-service and normal client service traffic is not interrupted.
         */
        INSERVICE,
        /**
         * Indicates the ETH-Test is out-of-service and normal client service traffic is disrupted.
         */
        OUTOFSERVICE
    }
}
