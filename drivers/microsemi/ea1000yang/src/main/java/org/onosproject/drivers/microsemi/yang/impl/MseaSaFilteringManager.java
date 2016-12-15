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
package org.onosproject.drivers.microsemi.yang.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.drivers.microsemi.yang.MseaSaFilteringNetconfService;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.TargetConfig;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.sa.filtering.rev20160412.MseaSaFiltering;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.sa.filtering.rev20160412.MseaSaFilteringOpParam;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.sa.filtering.rev20160412.MseaSaFilteringService;

/**
 * Implementation of the MseaSaFiltering YANG model service.
 */
@Component(immediate = true, inherit = true)
@Service
public class MseaSaFilteringManager extends AbstractYangServiceImpl
    implements MseaSaFilteringNetconfService {
    public static final String MSEA_SA_FILTERING = "org.onosproject.drivers.microsemi.yang.mseasafiltering";

    @Activate
    public void activate() {
        appId = coreService.registerApplication(MSEA_SA_FILTERING);
        ych = ymsService.getYangCodecHandler();
        ych.addDeviceSchema(MseaSaFilteringService.class);
        log.info("MseaSaFilteringManager Started");
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        ymsService.unRegisterService(this, MseaSaFilteringService.class);
        ych = null;
        log.info("MseaSaFilteringManager Stopped");
    }

    /**
     * Get a filtered subset of the model.
     * This is meant to filter the current live model
     * against the attribute(s) given in the argument
     * and return the filtered model.
     */
    @Override
    public MseaSaFiltering getMseaSaFiltering(MseaSaFilteringOpParam mseaSaFilteringFilter, NetconfSession session)
            throws NetconfException {
        return (MseaSaFiltering) getNetconfObject(mseaSaFilteringFilter, session);
    }

    /**
     * Call NETCONF edit-config with a configuration.
     */
    @Override
    public void setMseaSaFiltering(
            MseaSaFilteringOpParam mseaSaFiltering, NetconfSession session, TargetConfig ncDs)
            throws NetconfException {
        setNetconfObject(mseaSaFiltering, session, ncDs);
    }
}
