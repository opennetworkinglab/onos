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

package org.onosproject.net.pi.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.group.Group;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.service.PiFlowRuleTranslationStore;
import org.onosproject.net.pi.service.PiFlowRuleTranslator;
import org.onosproject.net.pi.service.PiGroupTranslationStore;
import org.onosproject.net.pi.service.PiGroupTranslator;
import org.onosproject.net.pi.service.PiMeterTranslationStore;
import org.onosproject.net.pi.service.PiMeterTranslator;
import org.onosproject.net.pi.service.PiTranslationException;
import org.onosproject.net.pi.service.PiTranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the PI translation service.
 */
@Component(immediate = true)
@Service
public class PiTranslationServiceImpl implements PiTranslationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // TODO: implement cache to speed up translation.

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PiFlowRuleTranslationStore flowRuleTranslationStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PiGroupTranslationStore groupTranslationStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PiMeterTranslationStore meterTranslationStore;

    private PiFlowRuleTranslator flowRuleTranslator;
    private PiGroupTranslator groupTranslator;
    private PiMeterTranslator meterTranslator;

    @Activate
    public void activate() {
        flowRuleTranslator = new InternalFlowRuleTranslator(flowRuleTranslationStore);
        groupTranslator = new InternalGroupTranslator(groupTranslationStore);
        meterTranslator = new InternalMeterTranslator(meterTranslationStore);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        flowRuleTranslator = null;
        groupTranslator = null;
        meterTranslator = null;
        log.info("Stopped");
    }

    @Override
    public PiFlowRuleTranslator flowRuleTranslator() {
        return flowRuleTranslator;
    }

    @Override
    public PiGroupTranslator groupTranslator() {
        return groupTranslator;
    }

    @Override
    public PiMeterTranslator meterTranslator() {
        return meterTranslator;
    }

    private Device getDevice(DeviceId deviceId) throws PiTranslationException {
        final Device device = deviceService.getDevice(deviceId);
        if (device == null) {
            throw new PiTranslationException("Unable to get device " + deviceId);
        }
        return device;
    }

    private final class InternalFlowRuleTranslator
            extends AbstractPiTranslatorImpl<FlowRule, PiTableEntry>
            implements PiFlowRuleTranslator {

        private InternalFlowRuleTranslator(PiFlowRuleTranslationStore store) {
            super(store);
        }

        @Override
        public PiTableEntry translate(FlowRule original, PiPipeconf pipeconf)
                throws PiTranslationException {
            return PiFlowRuleTranslatorImpl
                    .translate(original, pipeconf, getDevice(original.deviceId()));
        }
    }

    private final class InternalGroupTranslator
            extends AbstractPiTranslatorImpl<Group, PiActionGroup>
            implements PiGroupTranslator {

        private InternalGroupTranslator(PiGroupTranslationStore store) {
            super(store);
        }

        @Override
        public PiActionGroup translate(Group original, PiPipeconf pipeconf)
                throws PiTranslationException {
            return PiGroupTranslatorImpl
                    .translate(original, pipeconf, getDevice(original.deviceId()));
        }
    }

    private final class InternalMeterTranslator
            extends AbstractPiTranslatorImpl<Meter, PiMeterCellConfig>
            implements PiMeterTranslator {

        private InternalMeterTranslator(PiMeterTranslationStore store) {
            super(store);
        }

        @Override
        public PiMeterCellConfig translate(Meter original, PiPipeconf pipeconf)
                throws PiTranslationException {
            return PiMeterTranslatorImpl
                    .translate(original, pipeconf, getDevice(original.deviceId()));
        }
    }
}

