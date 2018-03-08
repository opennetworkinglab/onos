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
import org.apache.felix.scr.annotations.Component;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.TapiCommon;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.TapiConnectivity;
import org.onosproject.yang.gen.v1.tapipathcomputation.rev20180307.TapiPathComputation;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.TapiTopology;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;

import java.util.HashMap;
import java.util.Map;


/**
 * Component to register the TAPI 2.0 service model and its dependencies.
 */
@Component(immediate = true)
public class TapiModelRegistrator extends AbstractYangModelRegistrator {

    public TapiModelRegistrator() {
        super(TapiModelRegistrator.class, getAppInfo());
    }

    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {
        Map<YangModuleId, AppModuleInfo> appInfo = new HashMap<>();

        appInfo.put(new DefaultYangModuleId("tapi-connectivity", "2018-03-07"),
                    new DefaultAppModuleInfo(TapiConnectivity.class, null));

        appInfo.put(new DefaultYangModuleId("tapi-common", "2018-03-07"),
                    new DefaultAppModuleInfo(TapiCommon.class, null));

        appInfo.put(new DefaultYangModuleId("tapi-topology", "2018-03-07"),
                    new DefaultAppModuleInfo(TapiTopology.class, null));

        appInfo.put(new DefaultYangModuleId("tapi-path-computation", "2018-03-07"),
                    new DefaultAppModuleInfo(TapiPathComputation.class, null));

        return ImmutableMap.copyOf(appInfo);
    }
}
