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
package org.onosproject.openroadm.network;

import com.google.common.collect.ImmutableMap;
import org.apache.felix.scr.annotations.Component;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.IetfInetTypes;
import org.onosproject.yang.gen.v1.orgopenroadmcommontypes.rev20161014.OrgOpenroadmCommonTypes;
import org.onosproject.yang.gen.v1.orgopenroadmdegree.rev20161014.OrgOpenroadmDegree;
import org.onosproject.yang.gen.v1.orgopenroadmequipmentstatestypes.rev20161014.OrgOpenroadmEquipmentStatesTypes;
import org.onosproject.yang.gen.v1.orgopenroadmexternalpluggable.rev20161014.OrgOpenroadmExternalPluggable;
import org.onosproject.yang.gen.v1.orgopenroadmnetwork.rev20161014.OrgOpenroadmNetwork;
import org.onosproject.yang.gen.v1.orgopenroadmroadm.rev20161014.OrgOpenroadmRoadm;
import org.onosproject.yang.gen.v1.orgopenroadmsrg.rev20161014.OrgOpenroadmSrg;
import org.onosproject.yang.gen.v1.orgopenroadmxponder.rev20161014.OrgOpenroadmXponder;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Component to register the Open ROADM network model and its dependencies.
 */
@Component(immediate = true)
public class OpenRoadmNetworkModelRegistrator extends AbstractYangModelRegistrator {
    public OpenRoadmNetworkModelRegistrator() {
        super(OrgOpenroadmNetwork.class, getAppInfo());
    }

    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {
        Map<YangModuleId, AppModuleInfo> appInfo = new HashMap<>();
        // Dependencies for org-openroadm-network
        appInfo.put(new DefaultYangModuleId("org-openroadm-roadm", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmRoadm.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-external-pluggable", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmExternalPluggable.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-xponder", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmXponder.class, null));

        // Dependencies for org-openroadm-roadm
        appInfo.put(new DefaultYangModuleId("org-openroadm-srg", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmSrg.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-degree", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmDegree.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-inet-types", "2013-07-15"),
                new DefaultAppModuleInfo(IetfInetTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-common-types", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmCommonTypes.class, null));

        // Dependencies for org-openroadm-external-pluggable
        appInfo.put(new DefaultYangModuleId("org-openroadm-equipment-states-types", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmEquipmentStatesTypes.class, null));

        appInfo.put(new DefaultYangModuleId("org-openroadm-network", "2016-10-14"),
                new DefaultAppModuleInfo(OrgOpenroadmNetwork.class, null));
        return ImmutableMap.copyOf(appInfo);
    }
}
