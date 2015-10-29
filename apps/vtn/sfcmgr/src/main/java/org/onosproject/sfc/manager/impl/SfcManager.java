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
package org.onosproject.sfc.manager.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.sfc.manager.SfcService;
import org.onosproject.vtnrsc.PortChain;
import org.slf4j.Logger;

/**
 * Provides implementation of SFC Service.
 */
@Component(immediate = true)
@Service
public class SfcManager implements SfcService {

    private final Logger log = getLogger(getClass());

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void onPortPairCreated() {
        log.debug("onPortPairCreated");
        // TODO: Process port-pair on creation.
        // TODO: Parameter also needs to be modified.
    }

    @Override
    public void onPortPairDeleted() {
        log.debug("onPortPairDeleted");
        // TODO: Process port-pair on deletion.
        // TODO: Parameter also needs to be modified.
    }

    @Override
    public void onPortPairGroupCreated() {
        log.debug("onPortPairGroupCreated");
        // TODO: Process port-pair-group on creation.
        // TODO: Parameter also needs to be modified.
    }

    @Override
    public void onPortPairGroupDeleted() {
        log.debug("onPortPairGroupDeleted");
        // TODO: Process port-pair-group on deletion.
        // TODO: Parameter also needs to be modified.
    }

    @Override
    public void onFlowClassifierCreated() {
        log.debug("onFlowClassifierCreated");
        // TODO: Process flow-classifier on creation.
        // TODO: Parameter also needs to be modified.
    }

    @Override
    public void onFlowClassifierDeleted() {
        log.debug("onFlowClassifierDeleted");
        // TODO: Process flow-classifier on deletion.
        // TODO: Parameter also needs to be modified.
    }

    @Override
    public void onPortChainCreated() {
        log.debug("onPortChainCreated");
        // TODO: Process port-chain on creation.
        // TODO: Parameter also needs to be modified.

    }

    @Override
    public void onPortChainDeleted() {
        log.debug("onPortChainDeleted");
        // TODO: Process port-chain on deletion.
        // TODO: Parameter also needs to be modified.
    }

    /**
     * Install SF Forwarding rule into OVS.
     *
     * @param portChain
     *            port chain
     */
    public void installForwardingRule(PortChain portChain) {
        log.debug("installForwardingRule");
        // TODO: Installation of SF Forwarding rule into OVS.
    }

    /**
     * Uninstall SF Forwarding rule from OVS.
     *
     * @param portChain
     *            port chain
     */
    public void unInstallForwardingRule(PortChain portChain) {
        log.debug("unInstallForwardingRule");
        // TODO: Uninstallation of SF Forwarding rule from OVS.
    }
}