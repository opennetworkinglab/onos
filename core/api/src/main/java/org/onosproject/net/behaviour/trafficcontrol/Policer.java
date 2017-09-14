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

package org.onosproject.net.behaviour.trafficcontrol;

import com.google.common.annotations.Beta;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

import java.util.Collection;

/**
 * Generic abstraction for a policer which can mark and/or discard ingress
 * traffic. Each policer is made up of an identifier and a set of attributes
 * which defines the type of policer.
 * <p>
 * For example a policer specifying only a single token bucket, it will model
 * a simple drop policer or a marker policer. For the former, the traffic in
 * profile is green or red if it is out-of-profile. The latter envisages green
 * or yellow traffic. Currently there is no RFC for this kind of policer but
 * some vendors implement this model.
 * <p>
 * RFC 2697 can be modelled creating a policer with a collection of two
 * token buckets: [0] CIR + CBS; [1] CIR + EBS. In this way, it is possible
 * to create a policer single rate three color marker.
 * <p>
 * RFC 2698 and P4 meter are modelled in the same way but different attributes
 * for the token buckets: [0] PIR + PBS; [2] CIR + CBS. In this way, we can
 * create a policer two rate three color marker.
 * <p>
 * How these policers will be implemented depends on the specific technology
 * used in the device. For an OF device, the single rate two color marker it
 * could be implemented with a simple meter with a drop band.
 * <p>
 * Following abstraction has been designed to cover several types of policing
 * that have been specified during the years. However, this does not assure that
 * used technology will support all possible scenarios. For example, OF limitations
 * are well known in this field and implementations are even worse.
 */
@Beta
public interface Policer {

    /**
     * Unit of traffic used by this policer.
     */
    enum Unit {
        /**
         * Packets per second.
         */
        PKTS_PER_SEC,
        /**
         * Byte per second.
         */
        B_PER_SEC,
        /**
         * KByte per second.
         */
        KB_PER_SEC,
        /**
         * MByte per second.
         */
        MB_PER_SEC
    }

    /**
     * The device of this policer, where policing
     * is applied.
     *
     * @return the device id
     */
    DeviceId deviceId();

    /**
     * The id of the application which created this policer.
     *
     * @return the identifier of the application
     */
    ApplicationId applicationId();

    /**
     * Returns how many are referencing this policer.
     *
     * Availability of this information depends on the
     * technology used for the implementation of this policer.
     *
     * @return the reference count
     */
    long referenceCount();

    /**
     * Stats which reports how many packets have been
     * processed so far.
     *
     * Availability of this information depends on the
     * technology used for the implementation of this policer.
     *
     * @return the processed packets
     */
    long processedPackets();

    /**
     * Stats which reports how many bytes have been
     * processed so far.
     *
     * Availability of this information depends on the
     * technology used for the implementation of this policer.
     *
     * @return the processed bytes
     */
    long processedBytes();

    /**
     * The id of this policer.
     *
     * @return the policer id
     */
    PolicerId policerId();

    /**
     * Indicates if this policer is aware of the marking indication
     * in the ethernet frames.
     *
     * TODO Understand for the future how it is implemented by the vendors
     *
     * @return true if this policer is color aware.
     */
    boolean isColorAware();

    /**
     * The lifetime in seconds of this policer.
     *
     * Availability of this information depends on the
     * technology used for the implementation of this policer.
     *
     * @return number of seconds
     */
    long life();

    /**
     * The unit used within this policer.
     *
     * @return the unit
     */
    Unit unit();

    /**
     * The token buckets used within this policer.
     *
     * @return the list of the token buckets
     */
    Collection<TokenBucket> tokenBuckets();

    /**
     * Brief description of this policer.
     *
     * @return human readable description
     */
    String description();

    /**
     * A policer builder.
     */
    interface Builder {

        /**
         * Assigns the device for this policer.
         * <p>
         * Note: mandatory setter for this builder
         * </p>
         * @param deviceId a device id
         * @return this
         */
        Builder forDeviceId(DeviceId deviceId);

        /**
         * Assigns the application that built this policer.
         * <p>
         * Note: mandatory setter for this builder
         * </p>
         * @param appId an application id
         * @return this
         */
        Builder fromApp(ApplicationId appId);

        /**
         * Assigns the id to this policer.
         * <p>
         * Note: mandatory setter for this builder
         * </p>
         * @param id an identifier
         * @return this
         */
        Builder withId(PolicerId id);

        /**
         * Sets this policer to be color aware.
         * Defaults to false.
         *
         * @param isColorAware if it is color aware or not
         * @return this
         */
        Builder colorAware(boolean isColorAware);

        /**
         * Assigns the unit to use for this policer.
         * Defaults to MB/s.
         *
         * @param unit a unit
         * @return this
         */
        Builder withUnit(Unit unit);

        /**
         * Assigns policer id and device id for this policer.
         *
         * @param policingResource the policing resource
         * @return this
         */
        Builder withPolicingResource(PolicingResource policingResource);

        /**
         * Assigns token buckets for this policer.
         * <p>
         * Note: at least one token bucket
         * </p>
         * @param tokenBuckets the collection of token buckets
         * @return this
         */
        Builder withTokenBuckets(Collection<TokenBucket> tokenBuckets);

        /**
         * Assigns description for this policer.
         * Default is empty description.
         *
         * @param description the description
         * @return this
         */
        Builder withDescription(String description);

        /**
         * Builds the policer based on the specified parameters
         * when possible.
         *
         * @return a policer
         */
        Policer build();
    }

}
