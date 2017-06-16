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

package org.onosproject.net.pi.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiFlowRuleTranslationService;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.net.pi.impl.PiFlowRuleTranslator.translateFlowRule;

/**
 * Implementation of the protocol-independent flow rule translation service.
 */
@Component(immediate = true)
@Service
public class PiFlowRuleTranslationServiceImpl implements PiFlowRuleTranslationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // TODO: implement cache to speed up translation of flow rules.

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public PiTableEntry translate(FlowRule rule, PiPipeconf pipeconf)
            throws PiFlowRuleTranslationException {

        Device device = deviceService.getDevice(rule.deviceId());
        if (device == null) {
            throw new PiFlowRuleTranslationException("Unable to get device " + rule.deviceId());
        }

        return translateFlowRule(rule, pipeconf, device);
    }
}

