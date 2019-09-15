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
package org.onosproject.openstacknetworking.impl;

import org.onlab.packet.IpAddress;
import org.onosproject.core.CoreService;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.OpenstackHaService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_ACTIVE_IP_ADDRESS;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_HA_STATUS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing openstack HA service.
 */
@Component(
    immediate = true,
    service = { OpenstackHaService.class }
)
public class OpenstackHaManager implements OpenstackHaService {

    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    boolean activeFlag;
    IpAddress activeIpAddress;

    @Activate
    protected void activate() {
        coreService.registerApplication(Constants.OPENSTACK_NETWORKING_APP_ID);
        activeFlag = DEFAULT_HA_STATUS;
        activeIpAddress = DEFAULT_ACTIVE_IP_ADDRESS;
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean isActive() {
        return activeFlag;
    }

    @Override
    public IpAddress getActiveIp() {
        return activeIpAddress;
    }

    @Override
    public void setActiveIp(IpAddress ip) {
        this.activeIpAddress = ip;
    }

    @Override
    public void setActive(boolean flag) {
        this.activeFlag = flag;
    }
}
