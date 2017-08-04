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
package org.onosproject.provider.lisp.mapping.util;

import com.google.common.collect.Lists;
import org.onosproject.lisp.msg.protocols.LispLocator;
import org.onosproject.lisp.msg.protocols.LispMapRecord;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.mapping.DefaultMapping;
import org.onosproject.mapping.DefaultMappingEntry;
import org.onosproject.mapping.DefaultMappingKey;
import org.onosproject.mapping.DefaultMappingTreatment;
import org.onosproject.mapping.DefaultMappingValue;
import org.onosproject.mapping.Mapping;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingEntry.MappingEntryState;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.MappingValue;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.actions.MappingActions;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.onosproject.provider.lisp.mapping.util.MappingAddressBuilder.getAddress;

/**
 * Mapping entry builder class.
 */
public class MappingEntryBuilder {
    private static final Logger log =
            LoggerFactory.getLogger(MappingEntryBuilder.class);

    private final DeviceId deviceId;

    private final MappingAddress address;
    private final MappingAction action;
    private final List<MappingTreatment> treatments;

    private final DeviceService deviceService;

    /**
     * Default constructor for MappingEntryBuilder.
     *
     * @param deviceId      device identifier
     * @param record        LISP map record
     * @param deviceService device service
     */
    public MappingEntryBuilder(DeviceId deviceId, LispMapRecord record,
                               DeviceService deviceService) {
        this.deviceId = deviceId;
        this.address = buildAddress(record);
        this.action = buildAction(record);
        this.treatments = buildTreatments(record);
        this.deviceService = deviceService;
    }

    /**
     * Default constructor for MappingEntryBuilder.
     *
     * @param deviceId      device identifier
     * @param record        LISP map record
     */
    public MappingEntryBuilder(DeviceId deviceId, LispMapRecord record) {
        this.deviceId = deviceId;
        this.address = buildAddress(record);
        this.action = buildAction(record);
        this.treatments = buildTreatments(record);
        this.deviceService = null;
    }

    /**
     * Builds mapping entry from a specific LISP control message.
     *
     * @return mapping entry
     */
    public MappingEntry build() {
        Mapping.Builder builder;

        builder = DefaultMapping.builder()
                .withId(buildKey().hashCode())
                .forDevice(deviceId)
                .withKey(buildKey())
                .withValue(buildValue());

        // TODO: we assume that the mapping entry will be always
        // stored in routers without failure for now, which means
        // the mapping entry state will always be ADDED rather than
        // PENDING_ADD
        // we will revisit this part when LISP driver is finished
        return new DefaultMappingEntry(builder.build(), MappingEntryState.ADDED);
    }

    /**
     * Builds mapping key.
     *
     * @return mapping key
     */
    private MappingKey buildKey() {

        MappingKey.Builder builder = DefaultMappingKey.builder();

        builder.withAddress(address);

        return builder.build();
    }

    /**
     * Builds mapping value.
     *
     * @return mapping value
     */
    private MappingValue buildValue() {

        MappingValue.Builder builder = DefaultMappingValue.builder();
        builder.withAction(action);

        treatments.forEach(builder::add);

        return builder.build();
    }

    /**
     * Builds mapping action.
     *
     * @param record LISP map record
     * @return mapping action
     */
    private MappingAction buildAction(LispMapRecord record) {

        if (record == null) {
            return MappingActions.noAction();
        }

        switch (record.getAction()) {
            case NoAction:
                return MappingActions.noAction();
            case SendMapRequest:
                return MappingActions.forward();
            case NativelyForward:
                return MappingActions.nativeForward();
            case Drop:
                return MappingActions.drop();
            default:
                log.warn("Unsupported action type {}", record.getAction());
                return MappingActions.noAction();
        }
    }

    /**
     * Builds mapping address.
     *
     * @param record LISP map record
     * @return mapping address
     */
    private MappingAddress buildAddress(LispMapRecord record) {

        return record == null ? null :
                getAddress(deviceService, deviceId, record.getEidPrefixAfi());
    }

    /**
     * Builds a collection of mapping treatments.
     *
     * @param record LISP map record
     * @return a collection of mapping treatments
     */
    private List<MappingTreatment> buildTreatments(LispMapRecord record) {

        List<LispLocator> locators = record.getLocators();
        List<MappingTreatment> treatments = Lists.newArrayList();
        for (LispLocator locator : locators) {
            MappingTreatment.Builder builder = DefaultMappingTreatment.builder();
            LispAfiAddress address = locator.getLocatorAfi();

            final MappingAddress mappingAddress =
                    getAddress(deviceService, deviceId, address);
            if (mappingAddress != null) {
                builder.withAddress(mappingAddress);
            }

            builder.setUnicastWeight(locator.getWeight())
                    .setUnicastPriority(locator.getPriority())
                    .setMulticastWeight(locator.getMulticastWeight())
                    .setMulticastPriority(locator.getMulticastPriority());

            // TODO: need to convert specific properties to
            // abstracted extension properties

            treatments.add(builder.build());
        }

        return treatments;
    }
}
