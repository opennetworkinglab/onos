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

package org.onosproject.models.huawei;

import com.google.common.collect.ImmutableMap;
import org.osgi.service.component.annotations.Component;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.NeBgpcomm;
import org.onosproject.yang.gen.v1.nebgpcommtype.rev20141225.NeBgpcommType;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.NeL3VpnApi;
import org.onosproject.yang.gen.v1.nel3vpncomm.rev20141225.NeL3Vpncomm;
import org.onosproject.yang.gen.v1.nel3vpncommtype.rev20141225.NeL3VpncommType;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.NeTnlm;
import org.onosproject.yang.gen.v1.netnlmtype.rev20141225.NeTnlmType;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Representation of huawei model registrator which registers huawei device
 * models.
 */
@Component(immediate = true)
public class HuaweiModelRegistrator extends AbstractYangModelRegistrator {

    private static final String MODEL_VERSION = "2014-12-25";

    /**
     * Creates L3VPN model registrator.
     */
    public HuaweiModelRegistrator() {
        super(HuaweiModelRegistrator.class, getAppInfo());
    }

    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {
        Map<YangModuleId, AppModuleInfo> appInfo = new HashMap<>();
        appInfo.put(new DefaultYangModuleId("ne-bgpcomm", MODEL_VERSION),
                    new DefaultAppModuleInfo(NeBgpcomm.class, null));
        appInfo.put(new DefaultYangModuleId("ne-bgpcomm-type", MODEL_VERSION),
                    new DefaultAppModuleInfo(NeBgpcommType.class, null));
        appInfo.put(new DefaultYangModuleId("ne-l3vpn-api", MODEL_VERSION),
                    new DefaultAppModuleInfo(NeL3VpnApi.class, null));
        appInfo.put(new DefaultYangModuleId("ne-l3vpncomm", MODEL_VERSION),
                    new DefaultAppModuleInfo(NeL3Vpncomm.class, null));
        appInfo.put(new DefaultYangModuleId("ne-l3vpncomm-type", MODEL_VERSION),
                    new DefaultAppModuleInfo(NeL3VpncommType.class, null));
        appInfo.put(new DefaultYangModuleId("ne-tnlm", MODEL_VERSION),
                    new DefaultAppModuleInfo(NeTnlm.class, null));
        appInfo.put(new DefaultYangModuleId("ne-tnlm-type", MODEL_VERSION),
                    new DefaultAppModuleInfo(NeTnlmType.class, null));
        return ImmutableMap.copyOf(appInfo);
    }
}
