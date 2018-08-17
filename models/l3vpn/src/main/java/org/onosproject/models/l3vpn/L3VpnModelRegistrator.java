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

package org.onosproject.models.l3vpn;

import com.google.common.collect.ImmutableMap;
import org.osgi.service.component.annotations.Component;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.IetfBgpL3Vpn;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.IetfInterfaces;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.IetfIp;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.IetfL3VpnSvc;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.IetfNetworkInstance;
import org.onosproject.yang.gen.v1.l3vpnsvcext.rev20160730.L3VpnSvcExt;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Representation of L3VPN model registrator which registers L3VPN service
 * model.
 */
@Component(immediate = true)
public class L3VpnModelRegistrator extends AbstractYangModelRegistrator {

    /**
     * Creates L3VPN model registrator.
     */
    public L3VpnModelRegistrator() {
        super(L3VpnModelRegistrator.class, getAppInfo());
    }

    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {
        Map<YangModuleId, AppModuleInfo> appInfo = new HashMap<>();
        appInfo.put(new DefaultYangModuleId("ietf-l3vpn-svc", "2016-07-30"),
                    new DefaultAppModuleInfo(IetfL3VpnSvc.class, null));
        appInfo.put(new DefaultYangModuleId("l3vpn-svc-ext", "2016-07-30"),
                    new DefaultAppModuleInfo(L3VpnSvcExt.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-interfaces", "2014-05-08"),
                    new DefaultAppModuleInfo(IetfInterfaces.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-bgp-l3vpn", "2016-09-09"),
                    new DefaultAppModuleInfo(IetfBgpL3Vpn.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-ip", "2014-06-16"),
                    new DefaultAppModuleInfo(IetfIp.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-network-instance",
                                            "2016-06-23"),
                    new DefaultAppModuleInfo(IetfNetworkInstance.class, null));
        return ImmutableMap.copyOf(appInfo);
    }
}
