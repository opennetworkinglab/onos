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
package org.onosproject.sfc.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.vtnrsc.sfc.PortChain;
import org.slf4j.Logger;

/**
 * Provides implementation of SFC Service.
 */
@Component(immediate = true)
@Service
public class SfcManager implements SfcService {

    private final Logger log = getLogger(SfcManager.class);

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void InstallFlowClassification(PortChain portChain) {
        log.debug("InstallFlowClassification");
        //TODO: Installation of flow classification into OVS.
    }

    @Override
    public void UnInstallFlowClassification(PortChain portChain) {
        log.debug("UnInstallFlowClassification");
        //TODO: Un-installation flow classification from OVS
    }

    @Override
    public void InstallServiceFunctionChain(PortChain portChain) {
        log.debug("InstallServiceFunctionChain");
        //TODO: Installation of Service Function chain into OVS.
    }

    @Override
    public void UnInstallServiceFunctionChain(PortChain portChain) {
        log.debug("UnInstallServiceFunctionChain");
        //TODO: Un-installation of Service Function chain from OVS.
    }
}
