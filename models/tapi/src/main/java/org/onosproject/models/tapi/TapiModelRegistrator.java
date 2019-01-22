/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.models.tapi;

import com.google.common.collect.ImmutableMap;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.TapiCommon;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.TapiConnectivity;
import org.onosproject.yang.gen.v1.tapidsr.rev20181210.TapiDsr;
import org.onosproject.yang.gen.v1.tapieth.rev20181210.TapiEth;
import org.onosproject.yang.gen.v1.tapinotification.rev20181210.TapiNotification;
import org.onosproject.yang.gen.v1.tapioam.rev20181210.TapiOam;
import org.onosproject.yang.gen.v1.tapiodu.rev20181210.TapiOdu;
import org.onosproject.yang.gen.v1.tapipathcomputation.rev20181210.TapiPathComputation;
import org.onosproject.yang.gen.v1.tapiphotonicmedia.rev20181210.TapiPhotonicMedia;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.TapiTopology;
import org.onosproject.yang.gen.v1.tapivirtualnetwork.rev20181210.TapiVirtualNetwork;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;
import org.osgi.service.component.annotations.Component;

import java.util.HashMap;
import java.util.Map;


/**
 * Component to register the TAPI 2.1.X service model and its
 * dependencies.
 */
@Component(immediate = true)
public class TapiModelRegistrator extends AbstractYangModelRegistrator {

    public TapiModelRegistrator() {
        super(TapiModelRegistrator.class, getAppInfo());
    }

    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {
        Map<YangModuleId, AppModuleInfo> appInfo = new HashMap<>();

        appInfo.put(new DefaultYangModuleId("tapi-connectivity", "2018-12-10"),
                new DefaultAppModuleInfo(TapiConnectivity.class, null));

        appInfo.put(new DefaultYangModuleId("tapi-common", "2018-12-10"),
                new DefaultAppModuleInfo(TapiCommon.class, null));

        appInfo.put(new DefaultYangModuleId("tapi-topology", "2018-12-10"),
                new DefaultAppModuleInfo(TapiTopology.class, null));

        appInfo.put(new DefaultYangModuleId("tapi-photonic-media", "2018-12-10"),
                new DefaultAppModuleInfo(TapiPhotonicMedia.class, null));

        appInfo.put(new DefaultYangModuleId("tapi-oam", "2018-12-10"),
                new DefaultAppModuleInfo(TapiOam.class, null));

        appInfo.put(new DefaultYangModuleId("tapi-odu", "2018-12-10"),
                new DefaultAppModuleInfo(TapiOdu.class, null));

        appInfo.put(new DefaultYangModuleId("tapi-path-computation", "2018-12-10"),
                new DefaultAppModuleInfo(TapiPathComputation.class, null));

        appInfo.put(new DefaultYangModuleId("tapi-notification", "2018-12-10"),
                new DefaultAppModuleInfo(TapiNotification.class, null));

        appInfo.put(new DefaultYangModuleId("tapi-virtual-network", "2018-12-10"),
                new DefaultAppModuleInfo(TapiVirtualNetwork.class, null));

        appInfo.put(new DefaultYangModuleId("tapi-dsr", "2018-12-10"),
                new DefaultAppModuleInfo(TapiDsr.class, null));

        appInfo.put(new DefaultYangModuleId("tapi-eth", "2018-12-10"),
                new DefaultAppModuleInfo(TapiEth.class, null));

        return ImmutableMap.copyOf(appInfo);
    }
}
