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
package org.onosproject.openroadm.service;

import com.google.common.collect.ImmutableMap;
import org.osgi.service.component.annotations.Component;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.IetfInetTypes;
import org.onosproject.yang.gen.v1.ietfyangtypes.rev20130715.IetfYangTypes;
import org.onosproject.yang.gen.v1.orgopenroadmcommonservicetypes.rev20161014.OrgOpenroadmCommonServiceTypes;
import org.onosproject.yang.gen.v1.orgopenroadmcommontypes.rev20161014.OrgOpenroadmCommonTypes;
import org.onosproject.yang.gen.v1.orgopenroadmresource.rev20161014.OrgOpenroadmResource;
import org.onosproject.yang.gen.v1.orgopenroadmresourcetypes.rev20161014.OrgOpenroadmResourceTypes;
import org.onosproject.yang.gen.v1.orgopenroadmroutingconstraints.rev20161014.OrgOpenroadmRoutingConstraints;
import org.onosproject.yang.gen.v1.orgopenroadmservice.rev20161014.OrgOpenroadmService;
import org.onosproject.yang.gen.v1.orgopenroadmtopology.rev20161014.OrgOpenroadmTopology;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Component to register the Open ROADM service model and its dependencies.
 */
@Component(immediate = true)
public class OpenRoadmServiceModelRegistrator extends AbstractYangModelRegistrator {
    public OpenRoadmServiceModelRegistrator() {
        super(OrgOpenroadmService.class, getAppInfo());
    }

    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {
        Map<YangModuleId, AppModuleInfo> appInfo = new HashMap<>();
        // Dependencies for org-openroadm-service
        appInfo.put(new DefaultYangModuleId("ietf-yang-types", "2013-07-15"),
                new DefaultAppModuleInfo(IetfYangTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-routing-constraints", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmRoutingConstraints.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-common-types", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmCommonTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-resource-types", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmResourceTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-common-service-types", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmCommonServiceTypes.class, null));

        // Dependencies for org-openroadm-common-service-types
        appInfo.put(new DefaultYangModuleId("ietf-inet-types", "2013-07-15"),
                new DefaultAppModuleInfo(IetfInetTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-topology", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmTopology.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-topology", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmTopology.class, null));

        // Dependency for org-openroadm-topology
        appInfo.put(new DefaultYangModuleId("org-openroadm-resource", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmResource.class, null));


        appInfo.put(new DefaultYangModuleId("org-openroadm-service", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmService.class, null));
        return ImmutableMap.copyOf(appInfo);
    }
}
