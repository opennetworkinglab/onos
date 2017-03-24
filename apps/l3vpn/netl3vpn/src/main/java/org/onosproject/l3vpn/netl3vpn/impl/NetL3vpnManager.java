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

package org.onosproject.l3vpn.netl3vpn.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.CoreService;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev20130715.IetfInetTypes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.l3vpn.svc.rev20160730.IetfL3VpnSvc;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev20130715.IetfYangTypes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.l3vpn.svc.ext.rev20160730.L3VpnSvcExt;
import org.onosproject.yang.model.YangModel;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;
import org.onosproject.yang.runtime.DefaultModelRegistrationParam;
import org.onosproject.yang.runtime.ModelRegistrationParam;
import org.onosproject.yang.runtime.YangModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import static org.onosproject.yang.runtime.helperutils.YangApacheUtils.getYangModel;

/**
 * The IETF net l3vpn manager implementation.
 * // TODO: Implementation of the manager class.
 */
@Component(immediate = true)
public class NetL3vpnManager {

    private static final String APP_ID = "org.onosproject.app.l3vpn";
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected YangModelRegistry modelRegistry;

    private ModelRegistrationParam regParam = null;

    @Activate
    protected void activate() {
        coreService.registerApplication(APP_ID);
        log.info("Started");
        //TODO implementation
    }

    @Deactivate
    protected void deactivate() {
        modelRegistry.unregisterModel(regParam);
        log.info("Stopped");
    }

    private void registerModel() {
        YangModel model = getYangModel(getClass());
        Iterator<YangModuleId> it = model.getYangModulesId().iterator();

        //Create model registration param.
        ModelRegistrationParam.Builder b =
                DefaultModelRegistrationParam.builder().setYangModel(model);

        YangModuleId id;
        while (it.hasNext()) {
            id = it.next();
            switch (id.moduleName()) {
                case "ietf-inet-types":
                    b.addAppModuleInfo(id, new DefaultAppModuleInfo(
                            IetfInetTypes.class, null));
                    break;
                case "ietf-l3vpn-svc":
                    b.addAppModuleInfo(id, new DefaultAppModuleInfo(
                            IetfL3VpnSvc.class, null));
                    break;
                case "ietf-yang-types":
                    b.addAppModuleInfo(id, new DefaultAppModuleInfo(
                            IetfYangTypes.class, null));
                    break;
                case "l3vpn-svc-ext":
                    b.addAppModuleInfo(id, new DefaultAppModuleInfo(
                            L3VpnSvcExt.class, null));
                    break;
                default:
                    break;
            }
        }
        regParam = b.build();
        modelRegistry.registerModel(regParam);
    }
}
